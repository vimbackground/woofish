package com.woofish

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AudioPlayer(private val context: Context) : IAudioPlayer {
    private val soundPool: SoundPool
    private val woodenFishSounds = mutableListOf<Int>()
    private val metronomeSounds = mutableListOf<Int>()
    private val drumSounds = mutableListOf<Int>()
    private val pomodoroSounds = mutableListOf<Int>()
    private var pomodoroTickSound: Int = 0
    private var triangleSound: Int = 0
    private var bgmPlayer: MediaPlayer? = null
    private var currentBgmUri: String? = null

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attributes)
            .build()

        // 1. 木鱼音效 (首位为默认的禅韵木鱼)
        woodenFishSounds.add(soundPool.load(context, R.raw.sound_fish_3, 1)) // 禅韵木鱼
        woodenFishSounds.add(soundPool.load(context, R.raw.sound_fish_1, 1)) // 沉厚木鱼
        woodenFishSounds.add(soundPool.load(context, R.raw.sound_fish_2, 1)) // 清脆木鱼

        // 2. 节拍器音效 (2 种)
        metronomeSounds.add(soundPool.load(context, R.raw.sound_metro_1, 1))
        metronomeSounds.add(soundPool.load(context, R.raw.sound_metro_2, 1))

        // 3. 电子鼓音效 (3 种：低音鼓、真实拍中手鼓、非洲鼓)
        drumSounds.add(soundPool.load(context, R.raw.sound_drum_kick, 1))
        drumSounds.add(soundPool.load(context, R.raw.sound_drum_hand, 1))
        drumSounds.add(soundPool.load(context, R.raw.sound_drum_djembe, 1))

        // 4. 番茄钟专属背景轻柔秒针走动滴答音
        pomodoroTickSound = soundPool.load(context, R.raw.sound_pomodoro_tick, 1)
        pomodoroSounds.add(pomodoroTickSound)

        // 5. 倒计时结束真实三角铁敲击提示音
        triangleSound = soundPool.load(context, R.raw.sound_triangle, 1)
    }

    override fun playPomodoroTick() {
        if (pomodoroTickSound != 0) {
            soundPool.play(pomodoroTickSound, 0.35f, 0.35f, 1, 0, 1f)
        }
    }

    override fun playHit(mode: AppMode, soundIndex: Int, isManual: Boolean, vibrationMs: Int) {
        val list = when (mode) {
            AppMode.WOODEN_FISH -> woodenFishSounds
            AppMode.METRONOME -> metronomeSounds
            AppMode.DRUM -> drumSounds
            AppMode.POMODORO -> pomodoroSounds
        }
        val safeIndex = soundIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
        if (safeIndex in list.indices) {
            soundPool.play(list[safeIndex], 1f, 1f, 1, 0, 1f)
        }
        if (isManual) {
            vibrateManualKnock(vibrationMs)
        }
    }

    // 强力桌面物理震感：满功率(255振幅)驱动马达，手机放桌上单指敲击也能感知桌面震颤
    override fun vibrateManualKnock(durationMs: Int) {
        if (durationMs <= 0) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), 255))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs.toLong())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 倒计时结束禅意反馈：播放真实三角铁清脆提示音并配合双段式提示震动
    override fun playTimerFinishedFeedback() {
        if (triangleSound != 0) {
            soundPool.play(triangleSound, 1f, 1f, 1, 0, 1f)
        } else if (woodenFishSounds.isNotEmpty()) {
            soundPool.play(woodenFishSounds[0], 1f, 1f, 1, 0, 1f)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 150, 100, 250)
                val amplitudes = intArrayOf(0, 220, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(400)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun playKnock(soundIndex: Int) {
        playHit(AppMode.WOODEN_FISH, soundIndex, isManual = true, vibrationMs = 120)
    }

    override fun isBgmPlaying(): Boolean = bgmPlayer?.isPlaying == true

    override fun playCustomBgm(uriOrPath: String, volume: Float): Boolean {
        return try {
            bgmPlayer?.release()
            bgmPlayer = null

            val uri = android.net.Uri.parse(uriOrPath)
            val player = MediaPlayer().apply {
                setDataSource(context, uri)
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
            bgmPlayer = player
            currentBgmUri = uriOrPath
            true
        } catch (e: Exception) {
            e.printStackTrace()
            bgmPlayer?.release()
            bgmPlayer = null
            currentBgmUri = null
            false
        }
    }

    override fun toggleBgm(uriOrPath: String?, volume: Float): Boolean {
        if (uriOrPath.isNullOrEmpty()) {
            return false
        }

        return if (bgmPlayer != null && currentBgmUri == uriOrPath) {
            if (bgmPlayer?.isPlaying == true) {
                bgmPlayer?.pause()
                false
            } else {
                bgmPlayer?.start()
                true
            }
        } else {
            playCustomBgm(uriOrPath, volume)
        }
    }

    override fun stopBgm() {
        try {
            bgmPlayer?.stop()
            bgmPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bgmPlayer = null
            currentBgmUri = null
        }
    }

    override fun setBgmVolume(volume: Float) {
        bgmPlayer?.setVolume(volume, volume)
    }

    override fun release() {
        soundPool.release()
        stopBgm()
    }
}
