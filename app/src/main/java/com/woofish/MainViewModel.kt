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
    val beatIndex: Long = 0L, // 敲击/节拍累计索引，用于驱动受力回弹与节拍摆动动画
    val knockTrigger: Long = 0L, // 用于驱动受力回弹与节拍摆动动画
    val vibrationMs: Int = 120, // 敲击震动强度 (0~500ms，默认120ms)
    val isTimerEnabled: Boolean = false, // 是否开启倒计时功能
    val timerDurationMinutes: Int = 15, // 倒计时设定时长 (分钟)
    val timerRemainingSeconds: Long = 15 * 60L, // 倒计时当前剩余秒数
    val timerFinishedTrigger: Long = 0L, // 倒计时结束触发标记
    // 番茄钟专注模式专属状态
    val isPomodoroRunning: Boolean = false,
    val pomodoroStages: List<Int> = listOf(25),
    val currentPomodoroStageIndex: Int = 0,
    val pomodoroRemainingSeconds: Long = 25 * 60L,
    val pomodoroActivePreset: String = "",
    val pomodoroCustomSequence: String = "15+5",
    val pomodoroStageFinishedTrigger: Long = 0L,
    val isPomodoroSoundEnabled: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val audioPlayer = AudioPlayer(application)
    private val prefs = application.getSharedPreferences("wooden_fish_prefs", Context.MODE_PRIVATE)

    private var autoKnockJob: Job? = null
    private var timerJob: Job? = null
    private var pomodoroJob: Job? = null
    private var lastManualHitTime: Long = 0L

    private val initialMode: AppMode = AppMode.fromId(prefs.getString("key_mode", AppMode.WOODEN_FISH.id) ?: AppMode.WOODEN_FISH.id)
    private val initialBpm: Int = prefs.getInt("key_bpm", 60)
    private val initialVibrationMs: Int = prefs.getInt("key_vibration_ms", 120)
    private val initialTimerEnabled: Boolean = prefs.getBoolean("key_timer_enabled", false)
    private val initialTimerMinutes: Int = prefs.getInt("key_timer_duration_minutes", 15)
    private val initialPomodoroCustomSeq: String = prefs.getString("key_pomodoro_custom_sequence", "15+5") ?: "15+5"
    private val initialPomodoroSound: Boolean = prefs.getBoolean("key_pomodoro_sound_enabled", true)

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
            autoKnockIntervalMs = (60000L / initialBpm).coerceIn(200L, 2000L),
            vibrationMs = initialVibrationMs,
            isTimerEnabled = initialTimerEnabled,
            timerDurationMinutes = initialTimerMinutes,
            timerRemainingSeconds = initialTimerMinutes * 60L,
            pomodoroCustomSequence = initialPomodoroCustomSeq,
            isPomodoroSoundEnabled = initialPomodoroSound
        )
    )
    val uiState: StateFlow<WoodenFishUiState> = _uiState.asStateFlow()

    fun onHit(isManual: Boolean = false) {
        val mode = _uiState.value.currentMode
        val sIndex = _uiState.value.soundIndex
        val vMs = _uiState.value.vibrationMs
        audioPlayer.playHit(mode, sIndex, isManual = isManual, vibrationMs = vMs)
        val newBeatIndex = _uiState.value.beatIndex + 1
        if (isManual) {
            val newCount = _uiState.value.count + 1
            _uiState.value = _uiState.value.copy(
                count = newCount,
                beatIndex = newBeatIndex,
                knockTrigger = System.currentTimeMillis()
            )
            prefs.edit().putLong("key_count", newCount).apply()
        } else {
            _uiState.value = _uiState.value.copy(
                beatIndex = newBeatIndex,
                knockTrigger = System.currentTimeMillis()
            )
        }
    }

    fun onManualHit() {
        val now = System.currentTimeMillis()
        if (now - lastManualHitTime < 50L) {
            return
        }
        lastManualHitTime = now
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

        if (mode != AppMode.POMODORO) {
            stopPomodoro()
        } else {
            toggleAutoKnock(false)
        }

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
        audioPlayer.playHit(currentMode, nextIndex, isManual = true, vibrationMs = _uiState.value.vibrationMs)
    }

    fun updateSubtitle(text: String) {
        val trimmed = text.trim()
        val newSubtitle = if (trimmed.isEmpty()) _uiState.value.currentMode.defaultSubtitle else trimmed
        _uiState.value = _uiState.value.copy(subtitle = newSubtitle)
        prefs.edit().putString("key_subtitle", newSubtitle).apply()
    }

    fun updateVibrationMs(ms: Int) {
        val safeMs = ms.coerceIn(0, 500)
        _uiState.value = _uiState.value.copy(vibrationMs = safeMs)
        prefs.edit().putInt("key_vibration_ms", safeMs).apply()
        // 调节滑块时轻触预览震感
        if (safeMs > 0) {
            audioPlayer.vibrateManualKnock(safeMs)
        }
    }

    fun setBpm(newBpm: Int) {
        val safeBpm = newBpm.coerceIn(30, 300)
        val intervalMs = 60000L / safeBpm
        _uiState.value = _uiState.value.copy(
            bpm = safeBpm,
            autoKnockIntervalMs = intervalMs
        )
        prefs.edit().putInt("key_bpm", safeBpm).apply()

        // 如果正在自动敲击，平滑重启敲击协程以应用新频率，保持倒计时正常运转
        if (_uiState.value.isAutoKnockEnabled) {
            autoKnockJob?.cancel()
            autoKnockJob = viewModelScope.launch {
                while (isActive) {
                    onHit(isManual = false)
                    delay(intervalMs)
                }
            }
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
        timerJob?.cancel()

        if (nextState) {
            autoKnockJob = viewModelScope.launch {
                while (isActive) {
                    onHit(isManual = false)
                    delay(_uiState.value.autoKnockIntervalMs)
                }
            }
            if (_uiState.value.isTimerEnabled) {
                startTimerCountdown()
            }
        }
    }

    private fun startTimerCountdown() {
        timerJob?.cancel()
        if (!_uiState.value.isTimerEnabled) return

        if (_uiState.value.timerRemainingSeconds <= 0L) {
            _uiState.value = _uiState.value.copy(
                timerRemainingSeconds = _uiState.value.timerDurationMinutes * 60L
            )
        }

        timerJob = viewModelScope.launch {
            while (isActive && _uiState.value.timerRemainingSeconds > 0L) {
                delay(1000L)
                val remaining = _uiState.value.timerRemainingSeconds - 1L
                if (remaining <= 0L) {
                    _uiState.value = _uiState.value.copy(
                        timerRemainingSeconds = _uiState.value.timerDurationMinutes * 60L,
                        timerFinishedTrigger = System.currentTimeMillis()
                    )
                    audioPlayer.playTimerFinishedFeedback()
                    toggleAutoKnock(false)
                    break
                } else {
                    _uiState.value = _uiState.value.copy(timerRemainingSeconds = remaining)
                }
            }
        }
    }

    fun toggleTimer(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isTimerEnabled = enabled,
            timerRemainingSeconds = if (enabled && _uiState.value.timerRemainingSeconds <= 0L) {
                _uiState.value.timerDurationMinutes * 60L
            } else {
                _uiState.value.timerRemainingSeconds
            }
        )
        prefs.edit().putBoolean("key_timer_enabled", enabled).apply()

        if (enabled && _uiState.value.isAutoKnockEnabled) {
            startTimerCountdown()
        } else if (!enabled) {
            timerJob?.cancel()
        }
    }

    fun setTimerDuration(minutes: Int) {
        val safeMinutes = minutes.coerceIn(1, 180)
        _uiState.value = _uiState.value.copy(
            timerDurationMinutes = safeMinutes,
            timerRemainingSeconds = safeMinutes * 60L
        )
        prefs.edit().putInt("key_timer_duration_minutes", safeMinutes).apply()

        if (_uiState.value.isTimerEnabled && _uiState.value.isAutoKnockEnabled) {
            startTimerCountdown()
        }
    }

    fun resetTimer() {
        _uiState.value = _uiState.value.copy(
            timerRemainingSeconds = _uiState.value.timerDurationMinutes * 60L
        )
        if (_uiState.value.isTimerEnabled && _uiState.value.isAutoKnockEnabled) {
            startTimerCountdown()
        }
    }

    fun updateBgmVolume(volume: Float) {
        audioPlayer.setBgmVolume(volume)
        _uiState.value = _uiState.value.copy(bgmVolume = volume)
        prefs.edit().putFloat("key_volume", volume).apply()
    }

    fun resetCount() {
        _uiState.value = _uiState.value.copy(count = 0L, beatIndex = 0L)
        prefs.edit().putLong("key_count", 0L).apply()
    }

    // -------------------------------------------------------------
    // 番茄钟多阶段专注倒计时控制体系
    // -------------------------------------------------------------
    fun parsePomodoroSequence(input: String): List<Int> {
        val items = input.split('+', '、', ',', '，', ' ')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..180 }
        return if (items.isNotEmpty()) items else listOf(25)
    }

    fun startPomodoro(presetLabel: String, stages: List<Int>) {
        pomodoroJob?.cancel()
        val validStages = if (stages.isNotEmpty()) stages else listOf(25)
        _uiState.value = _uiState.value.copy(
            isPomodoroRunning = true,
            pomodoroActivePreset = presetLabel,
            pomodoroStages = validStages,
            currentPomodoroStageIndex = 0,
            pomodoroRemainingSeconds = validStages[0] * 60L
        )
        runPomodoroTicker()
    }

    private fun runPomodoroTicker() {
        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                // 需求2：番茄钟专属轻柔背景秒针走动滴答音
                if (_uiState.value.isPomodoroSoundEnabled) {
                    audioPlayer.playPomodoroTick()
                }
                val curRemaining = _uiState.value.pomodoroRemainingSeconds - 1L
                if (curRemaining <= 0L) {
                    val nextStageIdx = _uiState.value.currentPomodoroStageIndex + 1
                    val allStages = _uiState.value.pomodoroStages
                    if (nextStageIdx < allStages.size) {
                        // 阶段过渡：触发提示音并进入下一阶段（如 15分 -> 5分）
                        if (_uiState.value.isPomodoroSoundEnabled) {
                            audioPlayer.playTimerFinishedFeedback()
                        }
                        _uiState.value = _uiState.value.copy(
                            currentPomodoroStageIndex = nextStageIdx,
                            pomodoroRemainingSeconds = allStages[nextStageIdx] * 60L,
                            pomodoroStageFinishedTrigger = System.currentTimeMillis()
                        )
                    } else {
                        // 全部阶段圆满结束
                        if (_uiState.value.isPomodoroSoundEnabled) {
                            audioPlayer.playTimerFinishedFeedback()
                        }
                        _uiState.value = _uiState.value.copy(
                            isPomodoroRunning = false,
                            currentPomodoroStageIndex = 0,
                            pomodoroRemainingSeconds = allStages[0] * 60L,
                            pomodoroStageFinishedTrigger = System.currentTimeMillis()
                        )
                        break
                    }
                } else {
                    _uiState.value = _uiState.value.copy(pomodoroRemainingSeconds = curRemaining)
                }
            }
        }
    }

    fun resumePomodoro() {
        if (_uiState.value.isPomodoroRunning) return
        _uiState.value = _uiState.value.copy(isPomodoroRunning = true)
        runPomodoroTicker()
    }

    fun pausePomodoro() {
        pomodoroJob?.cancel()
        _uiState.value = _uiState.value.copy(isPomodoroRunning = false)
    }

    fun stopPomodoro() {
        pausePomodoro()
    }

    fun togglePomodoroSound() {
        val next = !_uiState.value.isPomodoroSoundEnabled
        _uiState.value = _uiState.value.copy(isPomodoroSoundEnabled = next)
        prefs.edit().putBoolean("key_pomodoro_sound_enabled", next).apply()
    }

    fun onPomodoroPresetClick(label: String, minutes: Int) {
        val currentPreset = _uiState.value.pomodoroActivePreset
        if (currentPreset == label) {
            if (_uiState.value.isPomodoroRunning) {
                pausePomodoro()
            } else {
                if (_uiState.value.pomodoroRemainingSeconds > 0L) {
                    resumePomodoro()
                } else {
                    startPomodoro(label, listOf(minutes))
                }
            }
        } else {
            // 点击预设直接开始倒计时，不需要额外点击 (需求4)
            startPomodoro(label, listOf(minutes))
        }
    }

    fun togglePomodoro(presetLabel: String? = null, stages: List<Int>? = null) {
        val currentPreset = _uiState.value.pomodoroActivePreset
        if (presetLabel == null || presetLabel == currentPreset) {
            if (_uiState.value.isPomodoroRunning) {
                pausePomodoro()
            } else {
                if (_uiState.value.pomodoroRemainingSeconds > 0L) {
                    resumePomodoro()
                } else {
                    startPomodoro(currentPreset.ifEmpty { "25分" }, _uiState.value.pomodoroStages)
                }
            }
        } else {
            startPomodoro(presetLabel, stages ?: listOf(25))
        }
    }

    fun setPomodoroCustomSequence(seqStr: String) {
        val trimmed = seqStr.trim()
        val stages = parsePomodoroSequence(trimmed)
        prefs.edit().putString("key_pomodoro_custom_sequence", trimmed).apply()
        _uiState.value = _uiState.value.copy(
            pomodoroCustomSequence = trimmed
        )
        startPomodoro("自定义", stages)
    }

    fun onPomodoroTap() {
        if (_uiState.value.isPomodoroRunning) {
            pausePomodoro()
        } else {
            if (_uiState.value.pomodoroRemainingSeconds > 0L) {
                resumePomodoro()
            } else {
                startPomodoro(_uiState.value.pomodoroActivePreset.ifEmpty { "25分" }, _uiState.value.pomodoroStages)
            }
        }
        // 需求3：番茄钟模式不需要点击发声，仅保留触感轻微震动
        if (_uiState.value.vibrationMs > 0) {
            audioPlayer.vibrateManualKnock((_uiState.value.vibrationMs / 3).coerceIn(20, 50))
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoKnockJob?.cancel()
        timerJob?.cancel()
        pomodoroJob?.cancel()
        audioPlayer.release()
    }
}
