package com.woofish

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ScreenOrientationSetting(val id: String, val displayName: String) {
    AUTO("auto", "自动旋转"),
    PORTRAIT("portrait", "锁定竖屏"),
    LANDSCAPE("landscape", "锁定横屏");

    companion object {
        fun fromId(id: String): ScreenOrientationSetting =
            entries.find { it.id == id } ?: AUTO
    }
}

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
    val isBigNumberMode: Boolean = false,
    val showSettings: Boolean = false,
    val showAudioSettings: Boolean = false,
    val isAutoKnockEnabled: Boolean = false,
    val bpm: Int = 60,
    val autoKnockIntervalMs: Long = 1000L,
    val showAutoKnockDialog: Boolean = false,
    val beatIndex: Long = 0L, // 敲击/节拍累计索引，用于驱动受力回弹与节拍摆动动画
    val knockTrigger: Long = 0L, // 用于驱动受力回弹与节拍摆动动画
    val vibrationMs: Int = 40, // 敲击震动强度 (0~120ms，默认40ms)
    val isTimerEnabled: Boolean = false, // 是否开启倒计时功能
    val timerDurationMinutes: Int = 15, // 倒计时设定时长 (分钟)
    val timerRemainingSeconds: Long = 15 * 60L, // 倒计时当前剩余秒数
    val timerFinishedTrigger: Long = 0L, // 倒计时结束触发标记
    val tempoActivePreset: String = "", // 节奏模式当前选中的预设或"自定义"
    val customBpm: Int = 60, // 自定义独立BPM设置值
    val screenOrientation: ScreenOrientationSetting = ScreenOrientationSetting.AUTO, // 屏幕方向设置
    val appLanguage: AppLanguage = AppLanguage.SYSTEM, // 软件多语言设置 (跟随系统/中文/英文)
    // 番茄钟专注模式专属状态
    val isPomodoroRunning: Boolean = false,
    val pomodoroStages: List<Int> = listOf(25),
    val currentPomodoroStageIndex: Int = 0,
    val pomodoroRemainingSeconds: Long = 25 * 60L,
    val pomodoroActivePreset: String = "",
    val pomodoroCustomSequence: String = "5+2+1",
    val pomodoroStageFinishedTrigger: Long = 0L,
    val isPomodoroSoundEnabled: Boolean = true
)

open class MainViewModel(
    val audioPlayer: IAudioPlayer,
    val prefs: IPreferences,
    val audioStreamProvider: IAudioStreamProvider? = null,
    val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {
    val viewModelScope = coroutineScope

    private var autoKnockJob: Job? = null
    private var timerJob: Job? = null
    private var pomodoroJob: Job? = null
    private var lastManualHitTime: Long = 0L

    private val initialMode: AppMode = AppMode.fromId(prefs.getString("key_mode", AppMode.WOODEN_FISH.id) ?: AppMode.WOODEN_FISH.id)
    private val initialBpm: Int = prefs.getInt("key_bpm", 60)
    private val initialCustomBpm: Int = prefs.getInt("key_custom_bpm", 60)
    private val initialTempoPreset: String = prefs.getString("key_tempo_active_preset", "") ?: ""
    private val initialVibrationMs: Int = prefs.getInt("key_vibration_ms", 40).coerceIn(0, 120)
    private val initialScreenOrientation: ScreenOrientationSetting = ScreenOrientationSetting.fromId(
        prefs.getString("key_screen_orientation", ScreenOrientationSetting.AUTO.id) ?: ScreenOrientationSetting.AUTO.id
    )
    private fun getModeTimerDuration(mode: AppMode): Int {
        val legacyMinutes = prefs.getInt("key_timer_duration_minutes", 15)
        return if (mode != AppMode.POMODORO) {
            prefs.getInt("key_timer_duration_${mode.id}", legacyMinutes)
        } else {
            legacyMinutes
        }
    }

    private fun getModeTimerEnabled(mode: AppMode): Boolean {
        val legacyEnabled = prefs.getBoolean("key_timer_enabled", false)
        return if (mode != AppMode.POMODORO) {
            prefs.getBoolean("key_timer_enabled_${mode.id}", legacyEnabled)
        } else {
            false
        }
    }

    private val initialTimerEnabled: Boolean = getModeTimerEnabled(initialMode)
    private val initialTimerMinutes: Int = getModeTimerDuration(initialMode)
    private val savedPomodoroSeq: String? = prefs.getString("key_pomodoro_custom_sequence_v2", null)
        ?: prefs.getString("key_pomodoro_custom_sequence", null)?.takeIf {
            it !in listOf("15+5", "25+5", "45+15", "50+10", "15+5分钟", "25+5分钟")
        }
    private val initialPomodoroCustomSeq: String = savedPomodoroSeq?.ifBlank { "5+2+1" } ?: "5+2+1"
    private val initialPomodoroSound: Boolean = prefs.getBoolean("key_pomodoro_sound_enabled", true)
    private val initialLanguage: AppLanguage = AppLanguage.fromId(
        prefs.getString("key_app_language", AppLanguage.SYSTEM.id) ?: AppLanguage.SYSTEM.id
    )

    private val _uiState = MutableStateFlow(
        WoodenFishUiState(
            count = prefs.getLong("key_count", 0L),
            currentMode = initialMode,
            subtitle = prefs.getString("key_subtitle", initialMode.defaultSubtitle) ?: initialMode.defaultSubtitle,
            bgmVolume = prefs.getFloat("key_volume", 0.3f),
            customBgmUri = prefs.getString("key_bgm_uri", null),
            customBgmTitle = prefs.getString("key_bgm_title", null),
            soundIndex = prefs.getInt("key_sound_${initialMode.id}", if (initialMode == AppMode.WOODEN_FISH) 1 else 0),
            isAnimationEnabled = prefs.getBoolean("key_animation_enabled", true),
            isFullScreenTapEnabled = prefs.getBoolean("key_full_screen_tap", true),
            bpm = initialBpm,
            autoKnockIntervalMs = (60000L / initialBpm).coerceIn(200L, 2000L),
            vibrationMs = initialVibrationMs,
            isTimerEnabled = initialTimerEnabled,
            timerDurationMinutes = initialTimerMinutes,
            timerRemainingSeconds = initialTimerMinutes * 60L,
            tempoActivePreset = initialTempoPreset,
            customBpm = initialCustomBpm,
            screenOrientation = initialScreenOrientation,
            appLanguage = initialLanguage,
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
            prefs.putLong("key_count", newCount)
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
        val defaultSound = if (mode == AppMode.WOODEN_FISH) 1 else 0
        val savedSoundIndex = prefs.getInt("key_sound_${mode.id}", defaultSound)
        val newSubtitle = mode.defaultSubtitle

        if (mode != AppMode.POMODORO) {
            stopPomodoro()
        }
        toggleAutoKnock(false)

        val newTimerDuration = getModeTimerDuration(mode)
        val newTimerEnabled = getModeTimerEnabled(mode)

        _uiState.value = _uiState.value.copy(
            currentMode = mode,
            soundIndex = savedSoundIndex.coerceIn(0, (mode.soundNames.size - 1).coerceAtLeast(0)),
            subtitle = newSubtitle,
            timerDurationMinutes = newTimerDuration,
            timerRemainingSeconds = newTimerDuration * 60L,
            isTimerEnabled = newTimerEnabled
        )
        prefs.putString("key_mode", mode.id)
        prefs.putString("key_subtitle", newSubtitle)
    }

    fun toggleSoundEffect() {
        val currentMode = _uiState.value.currentMode
        val maxSounds = currentMode.soundNames.size
        val nextIndex = (_uiState.value.soundIndex + 1) % maxSounds
        setSoundIndex(nextIndex)
    }

    fun setSoundIndex(index: Int) {
        val currentMode = _uiState.value.currentMode
        val safeIndex = index.coerceIn(0, (currentMode.soundNames.size - 1).coerceAtLeast(0))
        _uiState.value = _uiState.value.copy(soundIndex = safeIndex)
        prefs.putInt("key_sound_${currentMode.id}", safeIndex)

        if (currentMode == AppMode.POMODORO && _uiState.value.isPomodoroRunning) {
            if (_uiState.value.isPomodoroSoundEnabled) {
                if (safeIndex != 0) {
                    audioPlayer.startPomodoroLoop(safeIndex)
                } else {
                    audioPlayer.stopPomodoroLoop()
                }
            }
            return
        }

        val isAutoRunning = _uiState.value.isAutoKnockEnabled ||
            (currentMode == AppMode.POMODORO && _uiState.value.isPomodoroRunning)
        if (!isAutoRunning) {
            audioPlayer.playHit(currentMode, safeIndex, isManual = true, vibrationMs = _uiState.value.vibrationMs)
        }
    }

    fun toggleAudioSettingsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAudioSettings = show)
    }

    fun updateSubtitle(text: String) {
        val trimmed = text.trim()
        val newSubtitle = if (trimmed.isEmpty()) _uiState.value.currentMode.defaultSubtitle else trimmed
        _uiState.value = _uiState.value.copy(subtitle = newSubtitle)
        prefs.putString("key_subtitle", newSubtitle)
    }

    fun updateVibrationMs(ms: Int) {
        val safeMs = ms.coerceIn(0, 120)
        _uiState.value = _uiState.value.copy(vibrationMs = safeMs)
        prefs.putInt("key_vibration_ms", safeMs)
        if (safeMs > 0) {
            audioPlayer.vibrateManualKnock(safeMs)
        }
    }

    fun setScreenOrientation(setting: ScreenOrientationSetting) {
        _uiState.value = _uiState.value.copy(screenOrientation = setting)
        prefs.putString("key_screen_orientation", setting.id)
    }

    fun setAppLanguage(language: AppLanguage) {
        _uiState.value = _uiState.value.copy(appLanguage = language)
        prefs.putString("key_app_language", language.id)
    }

    fun setBpm(newBpm: Int) {
        val safeBpm = newBpm.coerceIn(30, 300)
        val intervalMs = 60000L / safeBpm
        _uiState.value = _uiState.value.copy(
            bpm = safeBpm,
            autoKnockIntervalMs = intervalMs
        )
        prefs.putInt("key_bpm", safeBpm)

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
        prefs.putString("key_bgm_uri", uriString)
        prefs.putString("key_bgm_title", title)
        val playing = audioPlayer.playCustomBgm(uriString, _uiState.value.bgmVolume)
        _uiState.value = _uiState.value.copy(
            customBgmUri = uriString,
            customBgmTitle = title,
            isBgmPlaying = playing
        )
    }

    fun clearCustomBgm() {
        audioPlayer.stopBgm()
        prefs.remove("key_bgm_uri")
        prefs.remove("key_bgm_title")
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

    fun toggleBigNumberMode(enabled: Boolean? = null) {
        val nextState = enabled ?: !_uiState.value.isBigNumberMode
        _uiState.value = _uiState.value.copy(isBigNumberMode = nextState)
    }

    fun toggleAnimation() {
        val newEnabled = !_uiState.value.isAnimationEnabled
        _uiState.value = _uiState.value.copy(isAnimationEnabled = newEnabled)
        prefs.putBoolean("key_animation_enabled", newEnabled)
    }

    fun setFullScreenTap(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isFullScreenTapEnabled = enabled)
        prefs.putBoolean("key_full_screen_tap", enabled)
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

    fun playTimerFinishedNotification() {
        viewModelScope.launch {
            for (i in 0 until 3) {
                audioPlayer.playTimerFinishedFeedback()
                if (i < 2) {
                    delay(1000L)
                }
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
                    playTimerFinishedNotification()
                    toggleAutoKnock(false)
                    break
                } else {
                    _uiState.value = _uiState.value.copy(timerRemainingSeconds = remaining)
                }
            }
        }
    }

    fun toggleTimer(enabled: Boolean) {
        val mode = _uiState.value.currentMode
        _uiState.value = _uiState.value.copy(
            isTimerEnabled = enabled,
            timerRemainingSeconds = if (enabled && _uiState.value.timerRemainingSeconds <= 0L) {
                _uiState.value.timerDurationMinutes * 60L
            } else {
                _uiState.value.timerRemainingSeconds
            }
        )
        prefs.putBoolean("key_timer_enabled", enabled)
        if (mode != AppMode.POMODORO) {
            prefs.putBoolean("key_timer_enabled_${mode.id}", enabled)
        }

        if (enabled && _uiState.value.isAutoKnockEnabled) {
            startTimerCountdown()
        } else if (!enabled) {
            timerJob?.cancel()
        }
    }

    fun setTimerDuration(minutes: Int) {
        val safeMinutes = minutes.coerceIn(1, 180)
        val mode = _uiState.value.currentMode
        _uiState.value = _uiState.value.copy(
            timerDurationMinutes = safeMinutes,
            timerRemainingSeconds = safeMinutes * 60L
        )
        prefs.putInt("key_timer_duration_minutes", safeMinutes)
        if (mode != AppMode.POMODORO) {
            prefs.putInt("key_timer_duration_${mode.id}", safeMinutes)
        }

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
        prefs.putFloat("key_volume", volume)
    }

    fun resetCount() {
        _uiState.value = _uiState.value.copy(count = 0L, beatIndex = 0L)
        prefs.putLong("key_count", 0L)
    }

    // -------------------------------------------------------------
    // 番茄钟多阶段专注倒计时控制体系
    // -------------------------------------------------------------
    fun parsePomodoroSequence(input: String): List<Int> {
        val items = input.split('+', '、', ',', '，', ' ')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..180 }
        return if (items.isNotEmpty()) items else listOf(5, 2, 1)
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
        if (_uiState.value.isPomodoroSoundEnabled && _uiState.value.soundIndex != 0) {
            audioPlayer.startPomodoroLoop(_uiState.value.soundIndex)
        } else {
            audioPlayer.stopPomodoroLoop()
        }
        pomodoroJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                if (_uiState.value.isPomodoroSoundEnabled && _uiState.value.soundIndex == 0) {
                    audioPlayer.playPomodoroTick()
                }
                val curRemaining = _uiState.value.pomodoroRemainingSeconds - 1L
                if (curRemaining <= 0L) {
                    audioPlayer.stopPomodoroLoop()
                    val nextStageIdx = _uiState.value.currentPomodoroStageIndex + 1
                    val allStages = _uiState.value.pomodoroStages
                    if (nextStageIdx < allStages.size) {
                        // 分阶段计时：中间每一阶段结束时响 1 声
                        audioPlayer.playTimerFinishedFeedback()
                        _uiState.value = _uiState.value.copy(
                            currentPomodoroStageIndex = nextStageIdx,
                            pomodoroRemainingSeconds = allStages[nextStageIdx] * 60L,
                            pomodoroStageFinishedTrigger = System.currentTimeMillis()
                        )
                        if (_uiState.value.isPomodoroSoundEnabled && _uiState.value.soundIndex != 0) {
                            audioPlayer.startPomodoroLoop(_uiState.value.soundIndex)
                        }
                    } else {
                        // 最后全部结束时：开启走针音响 3 声，不开启走针音（静音走针）时只响 1 声
                        if (_uiState.value.isPomodoroSoundEnabled) {
                            playTimerFinishedNotification()
                        } else {
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
        audioPlayer.stopPomodoroLoop()
        _uiState.value = _uiState.value.copy(isPomodoroRunning = false)
    }

    fun stopPomodoro() {
        pausePomodoro()
    }

    fun resetPomodoro() {
        pomodoroJob?.cancel()
        audioPlayer.stopPomodoroLoop()
        val stages = _uiState.value.pomodoroStages
        val stageIdx = _uiState.value.currentPomodoroStageIndex
        val currentStageMins = stages.getOrElse(stageIdx) { stages.firstOrNull() ?: 25 }
        _uiState.value = _uiState.value.copy(
            isPomodoroRunning = false,
            pomodoroRemainingSeconds = currentStageMins * 60L
        )
    }

    fun restartPomodoro() {
        pomodoroJob?.cancel()
        val stages = _uiState.value.pomodoroStages
        val firstStage = stages.firstOrNull() ?: 25
        _uiState.value = _uiState.value.copy(
            isPomodoroRunning = true,
            currentPomodoroStageIndex = 0,
            pomodoroRemainingSeconds = firstStage * 60L
        )
        runPomodoroTicker()
    }

    fun togglePomodoroSound() {
        val next = !_uiState.value.isPomodoroSoundEnabled
        _uiState.value = _uiState.value.copy(isPomodoroSoundEnabled = next)
        prefs.putBoolean("key_pomodoro_sound_enabled", next)
        if (_uiState.value.isPomodoroRunning) {
            if (next && _uiState.value.soundIndex != 0) {
                audioPlayer.startPomodoroLoop(_uiState.value.soundIndex)
            } else {
                audioPlayer.stopPomodoroLoop()
            }
        }
    }

    fun onPomodoroPresetClick(label: String, stages: List<Int>) {
        val currentPreset = _uiState.value.pomodoroActivePreset
        if (currentPreset == label) {
            if (_uiState.value.isPomodoroRunning) {
                pausePomodoro()
            } else {
                if (_uiState.value.pomodoroRemainingSeconds > 0L) {
                    resumePomodoro()
                } else {
                    startPomodoro(label, stages)
                }
            }
        } else {
            startPomodoro(label, stages)
        }
    }

    // -------------------------------------------------------------
    // 节奏模式 (木鱼/节拍器/电子鼓) 预设与自定义独立解耦控制体系
    // -------------------------------------------------------------
    fun onTempoPresetClick(label: String, presetBpm: Int) {
        val currentPreset = _uiState.value.tempoActivePreset
        if (currentPreset == label) {
            toggleAutoKnock(!_uiState.value.isAutoKnockEnabled)
        } else {
            setBpm(presetBpm)
            _uiState.value = _uiState.value.copy(tempoActivePreset = label)
            prefs.putString("key_tempo_active_preset", label)
            toggleAutoKnock(true)
        }
    }

    fun onTempoCustomClick() {
        val currentPreset = _uiState.value.tempoActivePreset
        if (currentPreset == "自定义") {
            toggleAutoKnock(!_uiState.value.isAutoKnockEnabled)
        } else {
            val customBpm = _uiState.value.customBpm
            setBpm(customBpm)
            _uiState.value = _uiState.value.copy(tempoActivePreset = "自定义")
            prefs.putString("key_tempo_active_preset", "自定义")
            toggleAutoKnock(true)
        }
    }

    fun setCustomBpm(newBpm: Int) {
        val safeBpm = newBpm.coerceIn(30, 300)
        prefs.putInt("key_custom_bpm", safeBpm)
        prefs.putString("key_tempo_active_preset", "自定义")
        _uiState.value = _uiState.value.copy(
            customBpm = safeBpm,
            tempoActivePreset = "自定义"
        )
        setBpm(safeBpm)
        toggleAutoKnock(true)
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
        prefs.putString("key_pomodoro_custom_sequence", trimmed)
        prefs.putString("key_pomodoro_custom_sequence_v2", trimmed)
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
        if (_uiState.value.vibrationMs > 0) {
            audioPlayer.vibrateManualKnock((_uiState.value.vibrationMs / 3).coerceIn(20, 50))
        }
    }

    open fun onCleared() {
        coroutineScope.cancel()
        autoKnockJob?.cancel()
        timerJob?.cancel()
        pomodoroJob?.cancel()
        audioPlayer.stopPomodoroLoop()
        audioPlayer.release()
    }
}

