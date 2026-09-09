package com.woofish

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AudioPlayer(private val context: Context) {
    private val soundPool: SoundPool
    private val woodenFishSounds = mutableListOf<Int>()
    private val metronomeSounds = mutableListOf<Int>()
    private val drumSounds = mutableListOf<Int>()
    private val eventSounds = mutableListOf<Int>()
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

        // 3. 电子鼓音效 (3 种：低音鼓、手鼓、非洲鼓)
        drumSounds.add(soundPool.load(context, R.raw.sound_drum_kick, 1))
        drumSounds.add(soundPool.load(context, R.raw.sound_drum_hand, 1))
        drumSounds.add(soundPool.load(context, R.raw.sound_drum_djembe, 1))

        // 4. 活动模式音效 (3 种：整齐拍掌、集体喊加油、集体怒吼)
        eventSounds.add(soundPool.load(context, R.raw.sound_event_clap, 1))
        eventSounds.add(soundPool.load(context, R.raw.sound_event_cheer, 1))
        eventSounds.add(soundPool.load(context, R.raw.sound_event_roar, 1))
    }

    fun playHit(mode: AppMode, soundIndex: Int, isManual: Boolean = false) {
        val list = when (mode) {
            AppMode.WOODEN_FISH -> woodenFishSounds
            AppMode.METRONOME -> metronomeSounds
            AppMode.DRUM -> drumSounds
            AppMode.EVENT -> eventSounds
        }
        val safeIndex = soundIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
        if (safeIndex in list.indices) {
            soundPool.play(list[safeIndex], 1f, 1f, 1, 0, 1f)
        }
        if (isManual) {
            vibrateManualKnock()
        }
    }

    fun vibrateManualKnock() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playKnock(soundIndex: Int) {
        playHit(AppMode.WOODEN_FISH, soundIndex, isManual = true)
    }

    fun isBgmPlaying(): Boolean = bgmPlayer?.isPlaying == true

    fun playCustomBgm(uriString: String, volume: Float): Boolean {
        return try {
            bgmPlayer?.release()
            bgmPlayer = null

            val uri = android.net.Uri.parse(uriString)
            val player = MediaPlayer().apply {
                setDataSource(context, uri)
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
            bgmPlayer = player
            currentBgmUri = uriString
            true
        } catch (e: Exception) {
            e.printStackTrace()
            bgmPlayer?.release()
            bgmPlayer = null
            currentBgmUri = null
            false
        }
    }

    fun toggleBgm(uriString: String?, volume: Float): Boolean {
        if (uriString.isNullOrEmpty()) {
            return false
        }

        return if (bgmPlayer != null && currentBgmUri == uriString) {
            if (bgmPlayer?.isPlaying == true) {
                bgmPlayer?.pause()
                false
            } else {
                bgmPlayer?.start()
                true
            }
        } else {
            playCustomBgm(uriString, volume)
        }
    }

    fun stopBgm() {
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

    fun setBgmVolume(volume: Float) {
        bgmPlayer?.setVolume(volume, volume)
    }

    fun release() {
        soundPool.release()
        stopBgm()
    }
}
