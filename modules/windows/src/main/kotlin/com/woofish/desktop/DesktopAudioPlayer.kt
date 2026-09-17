package com.woofish.desktop

import com.woofish.AppMode
import com.woofish.IAudioPlayer
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.*
import kotlin.math.log10

class DesktopAudioPlayer : IAudioPlayer {

    private class PreloadedClipPool(val audioBytes: ByteArray, val poolSize: Int = 4) {
        val clips = mutableListOf<Clip>()
        var index = 0

        init {
            for (i in 0 until poolSize) {
                try {
                    val stream = AudioSystem.getAudioInputStream(ByteArrayInputStream(audioBytes))
                    val clip = AudioSystem.getClip()
                    clip.open(stream)
                    clips.add(clip)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @Synchronized
        fun play(volume: Float = 1.0f) {
            if (clips.isEmpty()) return
            val clip = clips[index]
            index = (index + 1) % clips.size
            try {
                if (clip.isOpen) {
                    clip.stop()
                    clip.framePosition = 0
                    setClipVolume(clip, volume)
                    clip.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun release() {
            for (c in clips) {
                try {
                    c.stop()
                    c.close()
                } catch (_: Exception) {}
            }
            clips.clear()
        }
    }

    private val woodenFishPools = mutableListOf<PreloadedClipPool>()
    private val metronomePools = mutableListOf<PreloadedClipPool>()
    private val drumPools = mutableListOf<PreloadedClipPool>()
    private val pomodoroPools = mutableListOf<PreloadedClipPool>()
    private var pomodoroTickPool: PreloadedClipPool? = null
    private var trianglePool: PreloadedClipPool? = null

    private var bgmClip: Clip? = null
    private var currentBgmPath: String? = null
    private var currentBgmVolume: Float = 0.3f

    init {
        // 1. 木鱼音效 (首位为默认的禅韵木鱼)
        loadResource("raw/sound_fish_3.wav")?.let { woodenFishPools.add(PreloadedClipPool(it)) }
        loadResource("raw/sound_fish_1.wav")?.let { woodenFishPools.add(PreloadedClipPool(it)) }
        loadResource("raw/sound_fish_2.wav")?.let { woodenFishPools.add(PreloadedClipPool(it)) }

        // 2. 节拍器音效
        loadResource("raw/sound_metro_1.wav")?.let { metronomePools.add(PreloadedClipPool(it)) }
        loadResource("raw/sound_metro_2.wav")?.let { metronomePools.add(PreloadedClipPool(it)) }

        // 3. 电子鼓音效
        loadResource("raw/sound_drum_kick.wav")?.let { drumPools.add(PreloadedClipPool(it)) }
        loadResource("raw/sound_drum_hand.wav")?.let { drumPools.add(PreloadedClipPool(it)) }
        loadResource("raw/sound_drum_djembe.wav")?.let { drumPools.add(PreloadedClipPool(it)) }

        // 4. 番茄钟专属走针声
        loadResource("raw/sound_pomodoro_tick.wav")?.let {
            val pool = PreloadedClipPool(it)
            pomodoroTickPool = pool
            pomodoroPools.add(pool)
        }

        // 5. 倒计时结束真实三角铁敲击提示音
        loadResource("raw/sound_triangle.wav")?.let {
            trianglePool = PreloadedClipPool(it)
        }
    }

    private fun loadResource(path: String): ByteArray? {
        val classLoader = Thread.currentThread().contextClassLoader ?: javaClass.classLoader
        val input = classLoader.getResourceAsStream(path) ?: return null
        return input.use { it.readBytes() }
    }

    override fun playPomodoroTick() {
        pomodoroTickPool?.play(0.35f)
    }

    override fun playHit(mode: AppMode, soundIndex: Int, isManual: Boolean, vibrationMs: Int) {
        val list = when (mode) {
            AppMode.WOODEN_FISH -> woodenFishPools
            AppMode.METRONOME -> metronomePools
            AppMode.DRUM -> drumPools
            AppMode.POMODORO -> pomodoroPools
        }
        val safeIndex = soundIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
        if (safeIndex in list.indices) {
            list[safeIndex].play(1.0f)
        }
    }

    override fun playTimerFinishedFeedback() {
        if (trianglePool != null) {
            trianglePool?.play(1.0f)
        } else if (woodenFishPools.isNotEmpty()) {
            woodenFishPools[0].play(1.0f)
        }
    }

    override fun isBgmPlaying(): Boolean = bgmClip?.isRunning == true

    override fun playCustomBgm(uriOrPath: String, volume: Float): Boolean {
        stopBgm()
        currentBgmVolume = volume
        return try {
            val file = File(uriOrPath)
            if (!file.exists()) return false

            val audioStream = AudioSystem.getAudioInputStream(BufferedInputStream(file.inputStream()))
            val baseFormat = audioStream.format

            val decodedStream = if (baseFormat.encoding != AudioFormat.Encoding.PCM_SIGNED) {
                val decodedFormat = AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.sampleRate,
                    16,
                    baseFormat.channels,
                    baseFormat.channels * 2,
                    baseFormat.sampleRate,
                    false
                )
                AudioSystem.getAudioInputStream(decodedFormat, audioStream)
            } else {
                audioStream
            }

            val clip = AudioSystem.getClip()
            clip.open(decodedStream)
            setClipVolume(clip, volume)
            clip.loop(Clip.LOOP_CONTINUOUSLY)
            clip.start()

            bgmClip = clip
            currentBgmPath = uriOrPath
            true
        } catch (e: Exception) {
            e.printStackTrace()
            stopBgm()
            false
        }
    }

    override fun toggleBgm(uriOrPath: String?, volume: Float): Boolean {
        if (uriOrPath.isNullOrEmpty()) return false

        return if (bgmClip != null && currentBgmPath == uriOrPath) {
            if (bgmClip?.isRunning == true) {
                bgmClip?.stop()
                false
            } else {
                bgmClip?.start()
                true
            }
        } else {
            playCustomBgm(uriOrPath, volume)
        }
    }

    override fun stopBgm() {
        try {
            bgmClip?.stop()
            bgmClip?.close()
        } catch (_: Exception) {}
        bgmClip = null
        currentBgmPath = null
    }

    override fun setBgmVolume(volume: Float) {
        currentBgmVolume = volume
        bgmClip?.let { setClipVolume(it, volume) }
    }

    override fun release() {
        stopBgm()
        woodenFishPools.forEach { it.release() }
        metronomePools.forEach { it.release() }
        drumPools.forEach { it.release() }
        pomodoroPools.forEach { it.release() }
    }

    companion object {
        private fun setClipVolume(clip: Clip, volume: Float) {
            try {
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    val safeVolume = volume.coerceIn(0.0001f, 1f)
                    val dB = (log10(safeVolume.toDouble()) * 20.0).toFloat().coerceIn(gainControl.minimum, gainControl.maximum)
                    gainControl.value = dB
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
