package com.woofish

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
 * 2 阶 IIR 巴特沃斯低通滤波器，用于滤除人声与高频杂音，聚焦 180Hz 以下的底鼓与贝斯节奏骨架
 */
class BiquadLowPassFilter {
    // 22050Hz 采样率下 180Hz 截止频率系数
    private val b0 = 0.0006346
    private val b1 = 0.0012691
    private val b2 = 0.0006346
    private val a1 = -1.9274926
    private val a2 = 0.9300308

    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun process(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x
        y2 = y1
        y1 = y
        return y
    }

    fun reset() {
        x1 = 0.0
        x2 = 0.0
        y1 = 0.0
        y2 = 0.0
    }
}

/**
 * 基于低通滤波瞬态突变、谐波自相关校准与直方图投票的高精度环境音乐节奏识别器
 */
class AudioBeatDetector(
    private val streamProvider: IAudioStreamProvider? = null
) {

    private val _state = MutableStateFlow(BeatDetectionState())
    val state: StateFlow<BeatDetectionState> = _state.asStateFlow()

    private var job: Job? = null

    // 采样率与帧参数
    private val sampleRate = 22050
    private val frameSize = 512 // 每次分析 512 采样点 (~23.2ms)
    private val frameRate = sampleRate.toFloat() / frameSize // ~43.06 帧/秒

    // 历史瞬态突变环形队列 (存储约 4.5 秒数据，~200 帧)
    private val bufferCapacity = 200
    private val onsetBuffer = FloatArray(bufferCapacity)
    private var bufferCount = 0
    private var writeIndex = 0

    // 自适应阈值滑动窗口 (最近 16 帧)
    private val recentFluxHistory = FloatArray(16)
    private var fluxHistoryIndex = 0

    // BPM 直方图投票历史 (最近 16 次分析结果)
    private val bpmVotes = mutableListOf<Pair<Int, Float>>()

    fun start(scope: CoroutineScope) {
        if (_state.value.isListening) return
        stop()

        if (streamProvider == null || !streamProvider.isSupported) {
            _state.value = BeatDetectionState(error = "当前平台未配置麦克风音频采集支持")
            return
        }

        job = scope.launch(Dispatchers.Default) {
            try {
                _state.value = BeatDetectionState(
                    isListening = true,
                    statusText = "正在聆听环境音乐中..."
                )

                val lowPassFilter = BiquadLowPassFilter()
                var prevEnergy = 0f
                var frameCounter = 0
                bufferCount = 0
                writeIndex = 0
                fluxHistoryIndex = 0
                recentFluxHistory.fill(0f)
                bpmVotes.clear()

                streamProvider.start(scope, sampleRate, frameSize) { audioBuffer, readCount ->
                    if (!isActive || !_state.value.isListening) return@start

                    // 1. 低通滤波并提取低频节奏带 RMS 能量与全频最大振幅
                    var sumSquareFiltered = 0.0
                    var maxAmp = 0
                    for (i in 0 until readCount) {
                        val rawSample = audioBuffer[i]
                        val absSample = abs(rawSample.toInt())
                        if (absSample > maxAmp) maxAmp = absSample

                        val filtered = lowPassFilter.process(rawSample.toDouble())
                        sumSquareFiltered += filtered * filtered
                    }

                    val filteredRms = sqrt(sumSquareFiltered / readCount).toFloat()
                    val normalizedAmp = (maxAmp / 32767f).coerceIn(0f, 1f)

                    // 2. 计算低通能量瞬态上升沿 (Onset Flux)
                    val rawFlux = (filteredRms - prevEnergy).coerceAtLeast(0f)
                    prevEnergy = filteredRms

                    // 3. 自适应阈值动态减除，彻底分离节拍瞬态尖峰
                    recentFluxHistory[fluxHistoryIndex] = rawFlux
                    fluxHistoryIndex = (fluxHistoryIndex + 1) % recentFluxHistory.size
                    val adaptiveMean = recentFluxHistory.average().toFloat()
                    val cleanOnset = (rawFlux - adaptiveMean * 1.25f).coerceAtLeast(0f)

                    // 4. 写入环形缓冲区
                    onsetBuffer[writeIndex] = cleanOnset
                    writeIndex = (writeIndex + 1) % bufferCapacity
                    if (bufferCount < bufferCapacity) bufferCount++

                    frameCounter++

                    // 每 12 帧 (~280ms) 执行一次节奏自相关与谐波校准
                    if (frameCounter % 12 == 0 && bufferCount >= 75) {
                        val (bpm, confidence) = analyzeTempoWithHarmonics(onsetBuffer, writeIndex, bufferCount, frameRate)

                        if (bpm != null && confidence > 0.15f) {
                            bpmVotes.add(bpm to confidence)
                            if (bpmVotes.size > 14) bpmVotes.removeAt(0)
                        }

                        // 通过直方图聚类投票选取最稳定节拍
                        val votedBpm = getVotedTempo()
                        val finalConfidence = if (votedBpm != null) confidence else 0f

                        val status = when {
                            normalizedAmp < 0.025f -> "环境声音较小，请靠近音乐源..."
                            finalConfidence > 0.38f && votedBpm != null -> "已锁定节拍: $votedBpm BPM"
                            finalConfidence > 0.18f && votedBpm != null -> "正在锁定音乐节奏: $votedBpm BPM"
                            else -> "正在分析音乐节拍鼓点..."
                        }

                        _state.value = _state.value.copy(
                            amplitude = normalizedAmp,
                            detectedBpm = votedBpm ?: _state.value.detectedBpm,
                            confidence = finalConfidence,
                            statusText = status
                        )
                    } else if (frameCounter % 3 == 0) {
                        // 15Hz 高频更新振幅跳动
                        _state.value = _state.value.copy(amplitude = normalizedAmp)
                    }
                }
            } catch (e: Exception) {
                _state.value = BeatDetectionState(error = "音频捕获出错: ${e.message}")
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        streamProvider?.stop()
        _state.value = _state.value.copy(isListening = false, amplitude = 0f, statusText = "已停止")
    }

    /**
     * 直方图聚类投票机制：过滤偶然跳变，获取置信度最高的 BPM
     */
    private fun getVotedTempo(): Int? {
        if (bpmVotes.isEmpty()) return null
        val clusters = mutableListOf<Pair<Int, Float>>() // (bpm, totalWeight)

        for ((candBpm, weight) in bpmVotes) {
            val matchedIndex = clusters.indexOfFirst { abs(it.first - candBpm) <= 3 }
            if (matchedIndex != -1) {
                val old = clusters[matchedIndex]
                clusters[matchedIndex] = old.first to (old.second + weight)
            } else {
                clusters.add(candBpm to weight)
            }
        }

        val best = clusters.maxByOrNull { it.second } ?: return null
        // 至少有 2 次以上的有效累计支持
        return if (best.second > 0.45f) best.first else null
    }

    /**
     * 自相关计算节奏周期并进行谐波梳状加权 (Harmonic Comb Autocorrelation)
     */
    private fun analyzeTempoWithHarmonics(
        buffer: FloatArray,
        currIndex: Int,
        count: Int,
        fps: Float
    ): Pair<Int?, Float> {
        val minBpm = 45
        val maxBpm = 220

        val minLag = (fps * 60f / maxBpm).roundToInt().coerceAtLeast(4)
        val maxLag = (fps * 60f / minBpm).roundToInt().coerceAtMost(count / 2)

        if (maxLag <= minLag) return null to 0f

        val lagScores = FloatArray(maxLag + 1)
        var maxScore = 0f
        var bestLag = 0

        for (lag in minLag..maxLag) {
            var sumProduct = 0f
            var normSum = 0.00001f

            val evalLength = count - lag
            for (i in 0 until evalLength) {
                val idx1 = (currIndex - 1 - i + bufferCapacity) % bufferCapacity
                val idx2 = (currIndex - 1 - i - lag + bufferCapacity) % bufferCapacity

                val v1 = buffer[idx1]
                val v2 = buffer[idx2]
                sumProduct += v1 * v2
                normSum += v1 * v1
            }

            val autocorr = sumProduct / normSum
            lagScores[lag] = autocorr
        }

        // 谐波梳状加权：加入 1/2 拍与 2 倍拍的能量，消除八度混淆
        for (lag in minLag..maxLag) {
            var combEnergy = lagScores[lag]
            val halfLag = lag / 2
            if (halfLag >= minLag) {
                combEnergy += lagScores[halfLag] * 0.45f
            }
            val doubleLag = lag * 2
            if (doubleLag <= maxLag) {
                combEnergy += lagScores[doubleLag] * 0.35f
            }

            if (combEnergy > maxScore) {
                maxScore = combEnergy
                bestLag = lag
            }
        }

        if (bestLag == 0 || maxScore <= 0.08f) {
            return null to 0f
        }

        val rawBpm = (fps * 60f / bestLag).roundToInt()
        val peakCorrelation = (lagScores[bestLag] * 1.6f).coerceIn(0f, 1f)

        return if (rawBpm in minBpm..maxBpm) {
            rawBpm to peakCorrelation
        } else {
            val clamped = rawBpm.coerceIn(minBpm, maxBpm)
            clamped to (peakCorrelation * 0.6f)
        }
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
