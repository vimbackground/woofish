package com.woofish

import androidx.compose.runtime.staticCompositionLocalOf

interface AppStrings {
    val appName: String

    // Top Bar
    val enterZen: String
    val exitZen: String
    val animationSwitch: String
    val play: String
    val pause: String
    val reset: String
    val modeRunning: String
    val rhythmSettings: String
    val pomodoroSettings: String
    val audioSettings: String
    val appSettings: String

    // Mode Selection
    val selectModeTitle: String
    val selectModeSubtitle: String
    val woodenFishName: String
    val woodenFishSubtitle: String
    val woodenFishSounds: List<String>
    val metronomeName: String
    val metronomeSubtitle: String
    val metronomeSounds: List<String>
    val drumName: String
    val drumSubtitle: String
    val drumSounds: List<String>
    val pomodoroName: String
    val pomodoroSubtitle: String
    val pomodoroSounds: List<String>
    val woodenFishDesc: String
    val metronomeDesc: String
    val drumDesc: String
    val pomodoroDesc: String

    // Preset labels
    val woodenFishPresets: List<String>
    val metronomePresets: List<String>
    val drumPresets: List<String>
    val pomodoroPresets: List<String>
    val custom: String

    // Status explanation
    val tapToKnock: String
    val continuousRunning: String
    fun countdownLabel(timeStr: String): String
    fun stageLabel(current: Int, total: Int, minutes: Int): String
    val minuteUnit: String
    val resume: String
    val holdToReset: String

    // Settings Dialog
    val settingsTitle: String
    val back: String
    val done: String
    val close: String
    val displayText: String
    val displayTextPlaceholder: String
    val screenOrientation: String
    val orientationAuto: String
    val orientationPortrait: String
    val orientationLandscape: String
    val vibrationIntensity: String
    val vibrationOff: String
    val fullScreenTap: String
    val fullScreenTapDesc: String
    val languageSetting: String
    val languageSystem: String
    val totalKnocks: String
    val resetCount: String
    val resetCountConfirmTitle: String
    val resetCountConfirmMsg: String
    val cancel: String
    val confirm: String
    val sponsor: String
    val sponsorDesc: String
    val wechatPay: String

    // Rhythm Settings Dialog
    fun rhythmSettingTitle(modeName: String): String
    val autoContinuousRhythm: String
    fun runningInterval(intervalSec: String): String
    val paused: String
    val enablePomodoroCountdown: String
    fun pomodoroRunningStatus(stage: Int, total: Int, remain: String): String
    fun pomodoroPausedStatus(remain: String): String
    val autoKnockDuration: String
    val unlimitedContinuous: String
    fun countdownActive(timeStr: String): String
    fun durationMinutes(m: Int): String
    val tempoBpm: String
    val fineTuneInterval: String
    fun intervalSeconds(s: String): String
    val customBpmTitle: String
    val customBpmSubtitle: String
    val customPomodoroDialogTitle: String
    val customPomodoroDialogDesc: String
    val customPomodoroPlaceholder: String
    val quickPresets: String
    val startPlan: String
    val setButton: String
    val presetFocusDuration: String
    val presetRhythm: String
    val tempoFrequency: String
    val directInputBpm: String
    val start: String
    fun planPreview(totalMinutes: Int, previewStr: String): String

    // Audio Settings Dialog
    val audioSettingsTitle: String
    fun currentModeSoundTitle(modeName: String): String
    val bgmTitle: String
    val bgmStatusPlaying: String
    val bgmStatusPaused: String
    val selectMusic: String
    val selectMusicShort: String
    val changeMusic: String
    val clearMusic: String
    val clearMusicShort: String
    val bgmVolume: String
    val bgmLocalAudio: String
    val bgmNotSet: String
    val maternalHeartbeat: String
    val ambientDetection: String
    val ambientDetectionDesc: String
    val startDetection: String
    val stopDetection: String
    val listening: String
    val speedDetectionTitle: String
    val listenDetection: String
    val stopDetectionShort: String
    val confidenceHigh: String
    val confidenceGood: String
    val applySpeed: String
    val autoSyncBpm: String
    val tapTempoButton: String
    val tapTempoPrompt: String
    fun tapTempoResult(bpm: Int): String
}

object StringsZh : AppStrings {
    override val appName = "正念"

    override val enterZen = "进入清屏"
    override val exitZen = "退出清屏"
    override val animationSwitch = "动效开关"
    override val play = "播放"
    override val pause = "暂停"
    override val reset = "重置"
    override val modeRunning = "运行模式"
    override val rhythmSettings = "节奏设置"
    override val pomodoroSettings = "番茄钟专注设置"
    override val audioSettings = "声音设置"
    override val appSettings = "软件设置"

    override val selectModeTitle = "选择运行模式"
    override val selectModeSubtitle = "切换乐器与禅修节奏体系"
    override val woodenFishName = "木鱼模式"
    override val woodenFishSubtitle = "正念"
    override val woodenFishSounds = listOf("禅韵木鱼", "沉厚木鱼", "清脆木鱼")
    override val metronomeName = "节拍器模式"
    override val metronomeSubtitle = "节拍"
    override val metronomeSounds = listOf("机械节拍", "木质节拍", "真实心跳")
    override val drumName = "电子鼓模式"
    override val drumSubtitle = "律动"
    override val drumSounds = listOf("低音鼓", "手鼓", "非洲鼓")
    override val pomodoroName = "番茄钟模式"
    override val pomodoroSubtitle = "专注"
    override val pomodoroSounds = listOf("滴答音", "白噪音", "沉浸雨声")
    override val woodenFishDesc = "禅音正念 · 积聚功德 · 平和心绪"
    override val metronomeDesc = "精准律动 · 节拍辅助 · 稳定速率"
    override val drumDesc = "节奏打击 · 动感鼓点 · 释放压力"
    override val pomodoroDesc = "高效专注 · 阶段规划 · 沉浸自律"

    override val woodenFishPresets = listOf("禅修 30", "沉静 60", "舒缓 90", "诵经 120", "精进 150")
    override val metronomePresets = listOf("广板 30", "慢板 60", "行板 90", "快板 120", "急板 150")
    override val drumPresets = listOf("慢摇 30", "柔和 60", "律动 90", "动感 120", "狂热 150")
    override val pomodoroPresets = listOf("15+5", "25+5", "45+15", "50+10")
    override val custom = "自定义"

    override val tapToKnock = "点击屏幕敲击"
    override val continuousRunning = "持续运行"
    override fun countdownLabel(timeStr: String) = "倒计时 $timeStr"
    override fun stageLabel(current: Int, total: Int, minutes: Int) = "阶段 $current/$total: ${minutes}分"
    override val minuteUnit = "分钟"
    override val resume = "继续"
    override val holdToReset = "长按重置"

    override val settingsTitle = "软件设置"
    override val back = "← 返回"
    override val done = "完成"
    override val close = "关闭"
    override val displayText = "显示文案"
    override val displayTextPlaceholder = "例如：正念、功德、专注、节拍"
    override val screenOrientation = "屏幕方向"
    override val orientationAuto = "自动旋转"
    override val orientationPortrait = "锁定竖屏"
    override val orientationLandscape = "锁定横屏"
    override val vibrationIntensity = "敲击震动强度"
    override val vibrationOff = "已关闭"
    override val fullScreenTap = "全屏敲击模式"
    override val fullScreenTapDesc = "点击屏幕任意区域均可触发击打"
    override val languageSetting = "语言设置"
    override val languageSystem = "跟随系统"
    override val totalKnocks = "累计敲击总数"
    override val resetCount = "重置计数"
    override val resetCountConfirmTitle = "确认重置计数"
    override val resetCountConfirmMsg = "此操作将清零当前所有模式累计的敲击总数，是否继续？"
    override val cancel = "取消"
    override val confirm = "重置"
    override val sponsor = "💖 随喜赞助"
    override val sponsorDesc = "若正念对您有所助益，欢迎自愿赞助支持持续维护更新"
    override val wechatPay = "微信扫一扫 · 感恩有您 🙏"

    override fun rhythmSettingTitle(modeName: String) = when (modeName) {
        "番茄钟模式", "Pomodoro" -> "番茄钟专注设置"
        "节拍器模式", "Metronome" -> "节拍器节奏设置"
        "电子鼓模式", "Drum" -> "电子鼓节奏设置"
        else -> "木鱼节奏设置"
    }
    override val autoContinuousRhythm = "自动连续节奏"
    override fun runningInterval(intervalSec: String) = "运行中 · 间隔 $intervalSec 秒"
    override val paused = "已暂停"
    override val enablePomodoroCountdown = "开启番茄钟倒计时"
    override fun pomodoroRunningStatus(stage: Int, total: Int, remain: String) = "进行中 · 阶段 $stage/$total 剩余 $remain"
    override fun pomodoroPausedStatus(remain: String) = "已暂停 · 当前阶段剩余 $remain"
    override val autoKnockDuration = "⏱️ 自动敲击持续时间"
    override val unlimitedContinuous = "无限制（持续运行）"
    override fun countdownActive(timeStr: String) = "倒计时进行中 · $timeStr 后自动停止"
    override fun durationMinutes(m: Int) = "$m 分钟"
    override val tempoBpm = "敲击频率 (BPM)"
    override val fineTuneInterval = "微调间隔 (秒)"
    override fun intervalSeconds(s: String) = "间隔 $s 秒"
    override val customBpmTitle = "自定义敲击频率"
    override val customBpmSubtitle = "滑动选择节奏速度 (BPM)"
    override val customPomodoroDialogTitle = "自定义番茄钟倒计时"
    override val customPomodoroDialogDesc = "输入各阶段专注与休息分钟数，用加号连接："
    override val customPomodoroPlaceholder = "阶段分钟数，如 15+5 或 25+5+10"
    override val quickPresets = "快捷推荐："
    override val startPlan = "规划启动"
    override val setButton = "设定"
    override val presetFocusDuration = "预设专注时长"
    override val presetRhythm = "自动节奏预设"
    override val tempoFrequency = "自动节奏频率"
    override val directInputBpm = "直接输入数值"
    override val start = "开始"
    override fun planPreview(totalMinutes: Int, previewStr: String) = "预览规划 (共 $totalMinutes 分钟)：$previewStr"

    override val audioSettingsTitle = "声音设置"
    override fun currentModeSoundTitle(modeName: String) = "🔊 模式音效选择 ($modeName)"
    override val bgmTitle = "背景音乐"
    override val bgmStatusPlaying = "正在播放"
    override val bgmStatusPaused = "已暂停"
    override val selectMusic = "选择音乐"
    override val selectMusicShort = "选音乐"
    override val changeMusic = "更换"
    override val clearMusic = "清除音乐"
    override val clearMusicShort = "清除"
    override val bgmVolume = "背景音乐音量"
    override val bgmLocalAudio = "本地音频"
    override val bgmNotSet = "未设置背景音乐（可自选手机音频）"
    override val maternalHeartbeat = "母体心跳"
    override val ambientDetection = "环境音测速"
    override val ambientDetectionDesc = "通过麦克风智能识别周围音乐或环境节奏频率"
    override val startDetection = "开始测速"
    override val stopDetection = "停止测速"
    override val listening = "正在侦测..."
    override val speedDetectionTitle = "🎵 音效测速与节奏侦测"
    override val listenDetection = "🎙️ 听音测速"
    override val stopDetectionShort = "⏹️ 停止"
    override val confidenceHigh = "置信度: 优"
    override val confidenceGood = "置信度: 良好"
    override val applySpeed = "应用此速度"
    override val autoSyncBpm = "跟随侦测自动同步 BPM"
    override val tapTempoButton = "🖐️ 点击测速"
    override val tapTempoPrompt = "或跟随音乐节拍点击测速："
    override fun tapTempoResult(bpm: Int) = "轻敲测得: $bpm BPM"
}

object StringsEn : AppStrings {
    override val appName = "Mindfulness"

    override val enterZen = "Zen Mode"
    override val exitZen = "Exit Zen"
    override val animationSwitch = "Animation"
    override val play = "Play"
    override val pause = "Pause"
    override val reset = "Reset"
    override val modeRunning = "Mode"
    override val rhythmSettings = "Rhythm Settings"
    override val pomodoroSettings = "Pomodoro Settings"
    override val audioSettings = "Audio Settings"
    override val appSettings = "Settings"

    override val selectModeTitle = "Select Mode"
    override val selectModeSubtitle = "Switch instrument and mindfulness rhythm system"
    override val woodenFishName = "Wooden Fish"
    override val woodenFishSubtitle = "Mindfulness"
    override val woodenFishSounds = listOf("Zen Fish", "Deep Fish", "Crisp Fish")
    override val metronomeName = "Metronome"
    override val metronomeSubtitle = "Beats"
    override val metronomeSounds = listOf("Mechanical", "Woodblock", "Heartbeat")
    override val drumName = "Drum"
    override val drumSubtitle = "Rhythm"
    override val drumSounds = listOf("Bass Drum", "Hand Drum", "Djembe")
    override val pomodoroName = "Pomodoro"
    override val pomodoroSubtitle = "Focus"
    override val pomodoroSounds = listOf("Ticking", "White Noise", "Rain")
    override val woodenFishDesc = "Mindful rhythm · Inner peace · Calm focus"
    override val metronomeDesc = "Precise tempo · Beat keeper · Steady pace"
    override val drumDesc = "Acoustic percussion · Dynamic beats · Stress relief"
    override val pomodoroDesc = "Deep focus · Multi-stage intervals · Productivity"

    override val woodenFishPresets = listOf("Zen 30", "Calm 60", "Soothe 90", "Chant 120", "Diligence 150")
    override val metronomePresets = listOf("Largo 30", "Adagio 60", "Andante 90", "Allegro 120", "Presto 150")
    override val drumPresets = listOf("Slow 30", "Soft 60", "Groove 90", "Dynamic 120", "Fever 150")
    override val pomodoroPresets = listOf("15+5", "25+5", "45+15", "50+10")
    override val custom = "Custom"

    override val tapToKnock = "Tap screen to knock"
    override val continuousRunning = "Continuous"
    override fun countdownLabel(timeStr: String) = "Countdown $timeStr"
    override fun stageLabel(current: Int, total: Int, minutes: Int) = "Stage $current/$total: ${minutes}m"
    override val minuteUnit = "min"
    override val resume = "Resume"
    override val holdToReset = "Hold to reset"

    override val settingsTitle = "Settings"
    override val back = "← Back"
    override val done = "Done"
    override val close = "Close"
    override val displayText = "Display Text"
    override val displayTextPlaceholder = "e.g. Mindfulness, Merit, Focus, Beats"
    override val screenOrientation = "Screen Orientation"
    override val orientationAuto = "Auto"
    override val orientationPortrait = "Portrait"
    override val orientationLandscape = "Landscape"
    override val vibrationIntensity = "Haptic Feedback"
    override val vibrationOff = "Off"
    override val fullScreenTap = "Full-Screen Tap"
    override val fullScreenTapDesc = "Tap anywhere on screen to trigger knock"
    override val languageSetting = "Language"
    override val languageSystem = "Follow System"
    override val totalKnocks = "Total Knocks"
    override val resetCount = "Reset Count"
    override val resetCountConfirmTitle = "Reset Total Count"
    override val resetCountConfirmMsg = "This will clear all accumulated knock counts to zero. Continue?"
    override val cancel = "Cancel"
    override val confirm = "Reset"
    override val sponsor = "💖 Support & Donate"
    override val sponsorDesc = "If Mindfulness helps you, voluntary support is welcomed to help ongoing maintenance."
    override val wechatPay = "WeChat Pay · Thank You 🙏"

    override fun rhythmSettingTitle(modeName: String) = when (modeName) {
        "番茄钟模式", "Pomodoro" -> "Pomodoro Focus Settings"
        "节拍器模式", "Metronome" -> "Metronome Rhythm Settings"
        "电子鼓模式", "Drum" -> "Drum Rhythm Settings"
        else -> "Wooden Fish Rhythm Settings"
    }
    override val autoContinuousRhythm = "Auto Continuous Rhythm"
    override fun runningInterval(intervalSec: String) = "Running · Interval $intervalSec s"
    override val paused = "Paused"
    override val enablePomodoroCountdown = "Enable Pomodoro Countdown"
    override fun pomodoroRunningStatus(stage: Int, total: Int, remain: String) = "Running · Stage $stage/$total left $remain"
    override fun pomodoroPausedStatus(remain: String) = "Paused · Current stage left $remain"
    override val autoKnockDuration = "⏱️ Auto Knock Duration"
    override val unlimitedContinuous = "Unlimited (Continuous)"
    override fun countdownActive(timeStr: String) = "Counting down · stops in $timeStr"
    override fun durationMinutes(m: Int) = "$m mins"
    override val tempoBpm = "Tempo (BPM)"
    override val fineTuneInterval = "Fine-tune Interval (s)"
    override fun intervalSeconds(s: String) = "Interval $s s"
    override val customBpmTitle = "Custom Rhythm BPM"
    override val customBpmSubtitle = "Slide to select rhythm tempo (BPM)"
    override val customPomodoroDialogTitle = "Custom Pomodoro Countdown"
    override val customPomodoroDialogDesc = "Enter stage focus and rest minutes connected by plus:"
    override val customPomodoroPlaceholder = "Stage minutes, e.g. 15+5 or 25+5+10"
    override val quickPresets = "Quick Presets:"
    override val startPlan = "Start Plan"
    override val setButton = "Set"
    override val presetFocusDuration = "Preset Focus Duration"
    override val presetRhythm = "Rhythm Presets"
    override val tempoFrequency = "Rhythm Tempo"
    override val directInputBpm = "Enter BPM directly"
    override val start = "Start"
    override fun planPreview(totalMinutes: Int, previewStr: String) = "Plan Preview (Total $totalMinutes mins): $previewStr"

    override val audioSettingsTitle = "Audio Settings"
    override fun currentModeSoundTitle(modeName: String) = "🔊 Sound Effect Selection ($modeName)"
    override val bgmTitle = "Background Music"
    override val bgmStatusPlaying = "Playing"
    override val bgmStatusPaused = "Paused"
    override val selectMusic = "Select Music"
    override val selectMusicShort = "Select"
    override val changeMusic = "Change"
    override val clearMusic = "Clear Music"
    override val clearMusicShort = "Clear"
    override val bgmVolume = "BGM Volume"
    override val bgmLocalAudio = "Local Audio"
    override val bgmNotSet = "No BGM set (select local audio)"
    override val maternalHeartbeat = "Maternal Heartbeat"
    override val ambientDetection = "Ambient BPM Detection"
    override val ambientDetectionDesc = "Detect rhythm and tempo from surroundings using microphone"
    override val startDetection = "Start Detection"
    override val stopDetection = "Stop Detection"
    override val listening = "Listening..."
    override val speedDetectionTitle = "🎵 Rhythm & Tempo Detection"
    override val listenDetection = "🎙️ Listen"
    override val stopDetectionShort = "⏹️ Stop"
    override val confidenceHigh = "Confidence: High"
    override val confidenceGood = "Confidence: Good"
    override val applySpeed = "Apply Tempo"
    override val autoSyncBpm = "Auto-sync detected BPM"
    override val tapTempoButton = "🖐️ Tap Tempo"
    override val tapTempoPrompt = "Or tap with rhythm to detect:"
    override fun tapTempoResult(bpm: Int) = "Tapped: $bpm BPM"
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { StringsZh }

fun AppMode.getLocalizedDisplayName(strings: AppStrings): String = when (this) {
    AppMode.WOODEN_FISH -> strings.woodenFishName
    AppMode.METRONOME -> strings.metronomeName
    AppMode.DRUM -> strings.drumName
    AppMode.POMODORO -> strings.pomodoroName
}

fun AppMode.getLocalizedDefaultSubtitle(strings: AppStrings): String = when (this) {
    AppMode.WOODEN_FISH -> strings.woodenFishSubtitle
    AppMode.METRONOME -> strings.metronomeSubtitle
    AppMode.DRUM -> strings.drumSubtitle
    AppMode.POMODORO -> strings.pomodoroSubtitle
}

fun AppMode.getLocalizedSoundNames(strings: AppStrings): List<String> = when (this) {
    AppMode.WOODEN_FISH -> strings.woodenFishSounds
    AppMode.METRONOME -> strings.metronomeSounds
    AppMode.DRUM -> strings.drumSounds
    AppMode.POMODORO -> strings.pomodoroSounds
}

fun AppMode.getLocalizedTempoPresets(strings: AppStrings): List<Pair<String, Int>> {
    val labels = when (this) {
        AppMode.WOODEN_FISH -> strings.woodenFishPresets
        AppMode.METRONOME -> strings.metronomePresets
        AppMode.DRUM -> strings.drumPresets
        AppMode.POMODORO -> strings.pomodoroPresets
    }
    val defaultPairs = this.getTempoPresets()
    return labels.mapIndexed { idx, label ->
        label to (defaultPairs.getOrNull(idx)?.second ?: 60)
    }
}
