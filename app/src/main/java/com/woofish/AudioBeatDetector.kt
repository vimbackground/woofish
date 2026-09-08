package com.woofish

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 环境音乐节奏检测状态
 */
data class BeatDetectionState(
    val isListening: Boolean = false,
    val amplitude: Float = 0f,         // 实时振幅 0.0f ~ 1.0f，用于波形跳动
    val detectedBpm: Int? = null,      // 当前识别出的节拍数 (30 ~ 240)
    val confidence: Float = 0f,        // 识别置信度 0.0f ~ 1.0f
    val statusText: String = "就绪",    // 状态提示文案
    val error: String? = null          // 错误信息
)

/**
 * 基于短时能量突变与自相关分析 (Autocorrelation) 的环境音乐节奏识别器
 */
class AudioBeatDetector {

    private val _state = MutableStateFlow(BeatDetectionState())
    val state: StateFlow<BeatDetectionState> = _state.asStateFlow()

    private var job: Job? = null
    private var audioRecord: AudioRecord? = null

    // 采样率与帧参数
    private val sampleRate = 22050
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val frameSize = 512 // 每次分析 512 采样点 (~23.2ms)
    private val frameRate = sampleRate.toFloat() / frameSize // ~43.06 帧/秒

    // 历史能量突变环形队列 (存储约 4.5 秒数据，~200 帧)
    private val bufferCapacity = 200
    private val onsetBuffer = FloatArray(bufferCapacity)
    private var bufferCount = 0
    private var writeIndex = 0

    // BPM 平滑历史 (最近 3-5 次检测结果求中位数)
    private val recentBpms = mutableListOf<Int>()

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        if (_state.value.isListening) return

        stop()

        job = scope.launch(Dispatchers.Default) {
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSizeInBytes = maxOf(minBufSize, frameSize * 2 * 2)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSizeInBytes
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    _state.value = BeatDetectionState(error = "麦克风初始化失败，请检查设备支持情况")
                    return@launch
                }

                audioRecord?.startRecording()
                _state.value = BeatDetectionState(
                    isListening = true,
                    statusText = "正在聆听环境音乐中..."
                )

                val audioBuffer = ShortArray(frameSize)
                var prevEnergy = 0f
                var frameCounter = 0
                bufferCount = 0
                writeIndex = 0
                recentBpms.clear()

                while (isActive && _state.value.isListening) {
                    val readCount = audioRecord?.read(audioBuffer, 0, frameSize) ?: -1
                    if (readCount <= 0) {
                        delay(20)
                        continue
                    }

                    // 1. 计算当前帧 RMS 能量与峰值
                    var sumSquare = 0.0
                    var maxAmp = 0
                    for (i in 0 until readCount) {
                        val sample = audioBuffer[i]
                        sumSquare += sample * sample
                        val absSample = abs(sample.toInt())
                        if (absSample > maxAmp) maxAmp = absSample
                    }

                    val rms = sqrt(sumSquare / readCount).toFloat()
                    val normalizedAmp = (maxAmp / 32767f).coerceIn(0f, 1f)

                    // 2. 计算瞬态突变能量 (Onset Flux)
                    val energyDelta = (rms - prevEnergy).coerceAtLeast(0f)
                    prevEnergy = rms

                    // 3. 写入环形缓冲区
                    onsetBuffer[writeIndex] = energyDelta
                    writeIndex = (writeIndex + 1) % bufferCapacity
                    if (bufferCount < bufferCapacity) bufferCount++

                    frameCounter++

                    // 每 15 帧 (~350ms) 执行一次节拍自相关计算
                    if (frameCounter % 15 == 0 && bufferCount >= 80) {
                        val (bpm, confidence) = analyzeTempo(onsetBuffer, writeIndex, bufferCount, frameRate)
                        val status = when {
                            normalizedAmp < 0.03f -> "环境声音较小，请靠近音乐源..."
                            confidence > 0.40f && bpm != null -> "已锁定节拍: $bpm BPM"
                            confidence > 0.20f && bpm != null -> "侦测到疑似节拍: $bpm BPM"
                            else -> "正在分析音乐节奏鼓点..."
                        }

                        _state.value = _state.value.copy(
                            amplitude = normalizedAmp,
                            detectedBpm = bpm ?: _state.value.detectedBpm,
                            confidence = confidence,
                            statusText = status
                        )
                    } else if (frameCounter % 3 == 0) {
                        // 高频更新波形振幅反馈 (约 15Hz)
                        _state.value = _state.value.copy(amplitude = normalizedAmp)
                    }
                }
            } catch (e: Exception) {
                _state.value = BeatDetectionState(error = "音频捕获出错: ${e.message}")
            } finally {
                cleanup()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        cleanup()
        _state.value = _state.value.copy(isListening = false, amplitude = 0f, statusText = "已停止")
    }

    private fun cleanup() {
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    /**
     * 自相关计算节奏周期 (Autocorrelation)
     */
    private fun analyzeTempo(
        buffer: FloatArray,
        headIndex: Int,
        count: Int,
        fps: Float
    ): Pair<Int?, Float> {
        // 解开环形队列为线性时序数组
        val linear = FloatArray(count)
        val start = (headIndex - count + bufferCapacity) % bufferCapacity
        for (i in 0 until count) {
            linear[i] = buffer[(start + i) % bufferCapacity]
        }

        // 目标 BPM 范围 45 ~ 220
        val minLag = (60.0f * fps / 220.0f).roundToInt().coerceAtLeast(4)
        val maxLag = (60.0f * fps / 45.0f).roundToInt().coerceAtMost(count / 2)

        if (maxLag <= minLag) return null to 0f

        var bestLag = minLag
        var bestScore = -1f
        val scores = mutableMapOf<Int, Float>()

        for (lag in minLag..maxLag) {
            var dot = 0.0
            var norm1 = 0.0
            var norm2 = 0.0
            val len = count - lag
            for (i in 0 until len) {
                val v1 = linear[i].toDouble()
                val v2 = linear[i + lag].toDouble()
                dot += v1 * v2
                norm1 += v1 * v1
                norm2 += v2 * v2
            }
            val denom = (sqrt(norm1) * sqrt(norm2)) + 1e-6
            val normScore = (dot / denom).toFloat()
            scores[lag] = normScore

            // 优先权重：偏向人类常见音乐速度区间 70 ~ 150 BPM
            val currentBpm = (60.0f * fps / lag)
            val tempoPrior = 1.0f / (1.0f + ((currentBpm - 110f) / 60f) * ((currentBpm - 110f) / 60f))
            val weightedScore = normScore * (0.6f + 0.4f * tempoPrior)

            if (weightedScore > bestScore) {
                bestScore = weightedScore
                bestLag = lag
            }
        }

        // 泛音/倍频校正：若半周期 (2倍速) 同样具备较强相关性，优先取基频
        val halfLag = (bestLag / 2.0f).roundToInt()
        if (halfLag >= minLag && (scores[halfLag] ?: 0f) > 0.68f * (scores[bestLag] ?: 1f)) {
            bestLag = halfLag
        }

        val rawBpm = (60.0f * fps / bestLag).roundToInt().coerceIn(30, 240)
        val peakCorrelation = scores[bestLag] ?: 0f

        if (peakCorrelation < 0.18f) {
            return null to peakCorrelation
        }

        // 平滑滤波
        recentBpms.add(rawBpm)
        if (recentBpms.size > 5) recentBpms.removeAt(0)
        val smoothedBpm = recentBpms.sorted()[recentBpms.size / 2]

        return smoothedBpm to peakCorrelation
    }
}

/**
 * 手动轻敲测速器 (Tap Tempo)，作为安静环境或无麦克风权限时的极速辅助
 */
class TapTempoTracker {
    private val tapTimes = mutableListOf<Long>()

    fun recordTap(): Int? {
        val now = System.currentTimeMillis()
        // 超过 2.5 秒未敲击，重新开始统计
        if (tapTimes.isNotEmpty() && (now - tapTimes.last()) > 2500) {
            tapTimes.clear()
        }
        tapTimes.add(now)
        if (tapTimes.size > 8) tapTimes.removeAt(0)

        if (tapTimes.size < 2) return null

        var totalInterval = 0L
        for (i in 1 until tapTimes.size) {
            totalInterval += (tapTimes[i] - tapTimes[i - 1])
        }
        val avgIntervalMs = totalInterval / (tapTimes.size - 1)
        if (avgIntervalMs <= 0) return null

        val bpm = (60000L / avgIntervalMs).toInt().coerceIn(30, 300)
        return bpm
    }

    fun reset() {
        tapTimes.clear()
    }
}
