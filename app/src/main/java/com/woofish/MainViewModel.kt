package com.woofish

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WoodenFishUiState(
    val count: Long = 0,
    val isBgmPlaying: Boolean = false,
    val bgmVolume: Float = 0.3f,
    val soundIndex: Int = 0,
    val isAnimationEnabled: Boolean = true,
    val isFullScreenTapEnabled: Boolean = false,
    val isZenMode: Boolean = false,
    val showSettings: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val audioPlayer = AudioPlayer(application)
    private val prefs = application.getSharedPreferences("wooden_fish_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        WoodenFishUiState(
            count = prefs.getLong("key_count", 0L),
            bgmVolume = prefs.getFloat("key_volume", 0.3f),
            soundIndex = prefs.getInt("key_sound_index", 0),
            isAnimationEnabled = prefs.getBoolean("key_animation_enabled", true),
            isFullScreenTapEnabled = prefs.getBoolean("key_full_screen_tap", false)
        )
    )
    val uiState: StateFlow<WoodenFishUiState> = _uiState.asStateFlow()

    fun onKnock() {
        audioPlayer.playKnock(_uiState.value.soundIndex)
        val newCount = _uiState.value.count + 1
        _uiState.value = _uiState.value.copy(count = newCount)
        prefs.edit().putLong("key_count", newCount).apply()
    }

    fun toggleSoundEffect() {
        val nextIndex = if (_uiState.value.soundIndex == 0) 1 else 0
        _uiState.value = _uiState.value.copy(soundIndex = nextIndex)
        prefs.edit().putInt("key_sound_index", nextIndex).apply()
        audioPlayer.playKnock(nextIndex)
    }

    fun toggleBgm() {
        val playing = audioPlayer.toggleBgm(_uiState.value.bgmVolume)
        _uiState.value = _uiState.value.copy(isBgmPlaying = playing)
    }

    fun toggleZenMode() {
        _uiState.value = _uiState.value.copy(isZenMode = !_uiState.value.isZenMode)
    }

    fun toggleAnimation() {
        val newEnabled = !_uiState.value.isAnimationEnabled
        _uiState.value = _uiState.value.copy(isAnimationEnabled = newEnabled)
        prefs.edit().putBoolean("key_animation_enabled", newEnabled).apply()
    }

    fun setFullScreenTap(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isFullScreenTapEnabled = enabled)
        prefs.edit().putBoolean("key_full_screen_tap", enabled).apply()
    }

    fun toggleSettingsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSettings = show)
    }

    fun updateBgmVolume(volume: Float) {
        audioPlayer.setBgmVolume(volume)
        _uiState.value = _uiState.value.copy(bgmVolume = volume)
        prefs.edit().putFloat("key_volume", volume).apply()
    }

    fun resetCount() {
        _uiState.value = _uiState.value.copy(count = 0L)
        prefs.edit().putLong("key_count", 0L).apply()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
