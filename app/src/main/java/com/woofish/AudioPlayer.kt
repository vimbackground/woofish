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
    private val soundIds = mutableListOf<Int>()
    private var bgmPlayer: MediaPlayer? = null

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
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attributes)
            .build()

        soundIds.add(soundPool.load(context, R.raw.sound_1, 1))
        soundIds.add(soundPool.load(context, R.raw.sound_2, 1))
    }

    fun playKnock(soundIndex: Int) {
        if (soundIndex in soundIds.indices) {
            soundPool.play(soundIds[soundIndex], 1f, 1f, 1, 0, 1f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(25)
        }
    }

    fun isBgmPlaying(): Boolean = bgmPlayer?.isPlaying == true

    fun toggleBgm(volume: Float): Boolean {
        return if (bgmPlayer == null) {
            bgmPlayer = MediaPlayer.create(context, R.raw.bgm).apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
            true
        } else {
            if (bgmPlayer?.isPlaying == true) {
                bgmPlayer?.pause()
                false
            } else {
                bgmPlayer?.start()
                true
            }
        }
    }

    fun setBgmVolume(volume: Float) {
        bgmPlayer?.setVolume(volume, volume)
    }

    fun release() {
        soundPool.release()
        bgmPlayer?.release()
        bgmPlayer = null
    }
}
