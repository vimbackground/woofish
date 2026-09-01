package woofish

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
    val isZenMode: Boolean = false,
    val showSettings: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        var instance: MainViewModel? = null
    }

    private val audioPlayer = AudioPlayer(application)
    private val prefs = application.getSharedPreferences("wooden_fish_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        WoodenFishUiState(
            count = prefs.getLong("key_count", 0L),
            bgmVolume = prefs.getFloat("key_volume", 0.3f),
            soundIndex = prefs.getInt("key_sound_index", 0),
            isAnimationEnabled = prefs.getBoolean("key_animation_enabled", true),
            isBgmPlaying = prefs.getBoolean("key_bgm_playing", false)
        )
    )
    val uiState: StateFlow<WoodenFishUiState> = _uiState.asStateFlow()

    init {
        instance = this
        // 启动/同步前台常驻服务以支持息屏控制
        MediaNotificationService.updateService(application)
    }

    fun onKnock() {
        audioPlayer.playKnock(_uiState.value.soundIndex)
        val newCount = _uiState.value.count + 1
        _uiState.value = _uiState.value.copy(count = newCount)
        prefs.edit().putLong("key_count", newCount).apply()
        MediaNotificationService.updateService(getApplication())
    }

    fun toggleSoundEffect() {
        val nextIndex = if (_uiState.value.soundIndex == 0) 1 else 0
        setSoundFromLockScreen(nextIndex)
        audioPlayer.playKnock(nextIndex)
    }

    fun setSoundFromLockScreen(index: Int) {
        _uiState.value = _uiState.value.copy(soundIndex = index)
        prefs.edit().putInt("key_sound_index", index).apply()
        MediaNotificationService.updateService(getApplication())
    }

    fun toggleBgm() {
        val playing = audioPlayer.toggleBgm(_uiState.value.bgmVolume)
        _uiState.value = _uiState.value.copy(isBgmPlaying = playing)
        prefs.edit().putBoolean("key_bgm_playing", playing).apply()
        MediaNotificationService.updateService(getApplication())
    }

    fun toggleBgmFromLockScreen() {
        val playing = audioPlayer.toggleBgm(_uiState.value.bgmVolume)
        _uiState.value = _uiState.value.copy(isBgmPlaying = playing)
    }

    fun toggleZenMode() {
        _uiState.value = _uiState.value.copy(isZenMode = !_uiState.value.isZenMode)
    }

    fun toggleAnimation() {
        val newEnabled = !_uiState.value.isAnimationEnabled
        setAnimationFromLockScreen(newEnabled)
    }

    fun setAnimationFromLockScreen(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAnimationEnabled = enabled)
        prefs.edit().putBoolean("key_animation_enabled", enabled).apply()
        MediaNotificationService.updateService(getApplication())
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
        MediaNotificationService.updateService(getApplication())
    }

    override fun onCleared() {
        super.onCleared()
        instance = null
        audioPlayer.release()
    }
}
