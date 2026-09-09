package com.woofish

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class WoodenFishUiState(
    val count: Long = 0,
    val currentMode: AppMode = AppMode.WOODEN_FISH,
    val subtitle: String = "正念",
    val isBgmPlaying: Boolean = false,
    val bgmVolume: Float = 0.3f,
    val customBgmUri: String? = null,
    val customBgmTitle: String? = null,
    val soundIndex: Int = 0,
    val isAnimationEnabled: Boolean = true,
    val isFullScreenTapEnabled: Boolean = true,
    val isZenMode: Boolean = false,
    val showSettings: Boolean = false,
    val isAutoKnockEnabled: Boolean = false,
    val bpm: Int = 60,
    val autoKnockIntervalMs: Long = 1000L,
    val showAutoKnockDialog: Boolean = false,
    val knockTrigger: Long = 0L // 用于驱动受力回弹与节拍摆动动画
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val audioPlayer = AudioPlayer(application)
    private val prefs = application.getSharedPreferences("wooden_fish_prefs", Context.MODE_PRIVATE)

    private var autoKnockJob: Job? = null

    private val initialMode: AppMode = AppMode.fromId(prefs.getString("key_mode", AppMode.WOODEN_FISH.id) ?: AppMode.WOODEN_FISH.id)
    private val initialBpm: Int = prefs.getInt("key_bpm", 60)

    private val _uiState = MutableStateFlow(
        WoodenFishUiState(
            count = prefs.getLong("key_count", 0L),
            currentMode = initialMode,
            subtitle = prefs.getString("key_subtitle", initialMode.defaultSubtitle) ?: initialMode.defaultSubtitle,
            bgmVolume = prefs.getFloat("key_volume", 0.3f),
            customBgmUri = prefs.getString("key_bgm_uri", null),
            customBgmTitle = prefs.getString("key_bgm_title", null),
            soundIndex = prefs.getInt("key_sound_${initialMode.id}", 0),
            isAnimationEnabled = prefs.getBoolean("key_animation_enabled", true),
            isFullScreenTapEnabled = prefs.getBoolean("key_full_screen_tap", true),
            bpm = initialBpm,
            autoKnockIntervalMs = (60000L / initialBpm).coerceIn(200L, 2000L)
        )
    )
    val uiState: StateFlow<WoodenFishUiState> = _uiState.asStateFlow()

    fun onHit(isManual: Boolean = false) {
        val mode = _uiState.value.currentMode
        val sIndex = _uiState.value.soundIndex
        audioPlayer.playHit(mode, sIndex, isManual = isManual)
        val newCount = _uiState.value.count + 1
        _uiState.value = _uiState.value.copy(
            count = newCount,
            knockTrigger = System.currentTimeMillis()
        )
        prefs.edit().putLong("key_count", newCount).apply()
    }

    fun onManualHit() {
        onHit(isManual = true)
    }

    // 兼容原敲击方法名
    fun onKnock() {
        onManualHit()
    }

    fun setAppMode(mode: AppMode) {
        if (mode == _uiState.value.currentMode) return
        val savedSoundIndex = prefs.getInt("key_sound_${mode.id}", 0)
        // 模式切换时，计数显示文案自动调整
        val newSubtitle = mode.defaultSubtitle

        _uiState.value = _uiState.value.copy(
            currentMode = mode,
            soundIndex = savedSoundIndex.coerceIn(0, (mode.soundNames.size - 1).coerceAtLeast(0)),
            subtitle = newSubtitle
        )
        prefs.edit()
            .putString("key_mode", mode.id)
            .putString("key_subtitle", newSubtitle)
            .apply()
    }

    fun toggleSoundEffect() {
        val currentMode = _uiState.value.currentMode
        val maxSounds = currentMode.soundNames.size
        val nextIndex = (_uiState.value.soundIndex + 1) % maxSounds
        _uiState.value = _uiState.value.copy(soundIndex = nextIndex)
        prefs.edit().putInt("key_sound_${currentMode.id}", nextIndex).apply()
        audioPlayer.playHit(currentMode, nextIndex, isManual = true)
    }

    fun updateSubtitle(text: String) {
        val trimmed = text.trim()
        val newSubtitle = if (trimmed.isEmpty()) _uiState.value.currentMode.defaultSubtitle else trimmed
        _uiState.value = _uiState.value.copy(subtitle = newSubtitle)
        prefs.edit().putString("key_subtitle", newSubtitle).apply()
    }

    fun setBpm(newBpm: Int) {
        val safeBpm = newBpm.coerceIn(30, 300)
        val intervalMs = 60000L / safeBpm
        _uiState.value = _uiState.value.copy(
            bpm = safeBpm,
            autoKnockIntervalMs = intervalMs
        )
        prefs.edit().putInt("key_bpm", safeBpm).apply()

        // 如果正在自动敲击，重启协程以应用新频率
        if (_uiState.value.isAutoKnockEnabled) {
            toggleAutoKnock(true)
        }
    }

    fun setCustomBgm(uriString: String, title: String) {
        prefs.edit()
            .putString("key_bgm_uri", uriString)
            .putString("key_bgm_title", title)
            .apply()
        val playing = audioPlayer.playCustomBgm(uriString, _uiState.value.bgmVolume)
        _uiState.value = _uiState.value.copy(
            customBgmUri = uriString,
            customBgmTitle = title,
            isBgmPlaying = playing
        )
    }

    fun clearCustomBgm() {
        audioPlayer.stopBgm()
        prefs.edit()
            .remove("key_bgm_uri")
            .remove("key_bgm_title")
            .apply()
        _uiState.value = _uiState.value.copy(
            customBgmUri = null,
            customBgmTitle = null,
            isBgmPlaying = false
        )
    }

    fun toggleBgm() {
        val uri = _uiState.value.customBgmUri ?: return
        val playing = audioPlayer.toggleBgm(uri, _uiState.value.bgmVolume)
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

    fun toggleAutoKnockDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAutoKnockDialog = show)
    }

    fun toggleAutoKnock(enabled: Boolean? = null) {
        val nextState = enabled ?: !_uiState.value.isAutoKnockEnabled
        _uiState.value = _uiState.value.copy(isAutoKnockEnabled = nextState)
        
        autoKnockJob?.cancel()
        if (nextState) {
            autoKnockJob = viewModelScope.launch {
                while (isActive) {
                    onHit(isManual = false)
                    delay(_uiState.value.autoKnockIntervalMs)
                }
            }
        }
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
        autoKnockJob?.cancel()
        audioPlayer.release()
    }
}
