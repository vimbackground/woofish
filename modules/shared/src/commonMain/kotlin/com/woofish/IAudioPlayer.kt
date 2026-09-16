package com.woofish

interface IAudioPlayer {
    fun playPomodoroTick()
    fun playHit(mode: AppMode, soundIndex: Int, isManual: Boolean = false, vibrationMs: Int = 120)
    fun vibrateManualKnock(durationMs: Int) {}
    fun playTimerFinishedFeedback()
    fun playKnock(soundIndex: Int) {
        playHit(AppMode.WOODEN_FISH, soundIndex, isManual = true, vibrationMs = 120)
    }
    fun isBgmPlaying(): Boolean
    fun playCustomBgm(uriOrPath: String, volume: Float): Boolean
    fun toggleBgm(uriOrPath: String?, volume: Float): Boolean
    fun stopBgm()
    fun setBgmVolume(volume: Float)
    fun release()
}
