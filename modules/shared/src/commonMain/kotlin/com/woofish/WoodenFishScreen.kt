package com.woofish

import woofish.shared.generated.resources.*

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.SolidColor
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

// 触碰即响 (Touch DOWN 零延迟) 辅助修饰符
fun Modifier.detectInstantTap(
    enabled: Boolean = true,
    onDown: () -> Unit,
    onUp: () -> Unit = {}
): Modifier = if (enabled) {
    this.pointerInput(enabled) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            onDown()
            waitForUpOrCancellation()
            onUp()
        }
    }
} else this

// 纯黑白播放与暂停矢量图标组件 (避免系统 Emoji 字体彩斑)
@Composable
fun BwPlayIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.1f)
            lineTo(w * 0.9f, h * 0.5f)
            lineTo(w * 0.15f, h * 0.9f)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
fun BwPauseIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = w * 0.28f
        val top = h * 0.12f
        val barH = h * 0.76f
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.12f, top),
            size = Size(barW, barH)
        )
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.60f, top),
            size = Size(barW, barH)
        )
    }
}


// 乐器图示组件（根据模式渲染木鱼/节拍器/电子鼓，支持物理敲击受力动效）
@Composable
private fun InstrumentView(
    state: WoodenFishUiState,
    instrumentSize: androidx.compose.ui.unit.Dp,
    impactScale: Float,
    impactOffsetY: Float,
    metronomeAngle: Float,
    onHitDown: () -> Unit,
    onHitUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(instrumentSize)
            .detectInstantTap(
                enabled = !state.isFullScreenTapEnabled,
                onDown = onHitDown,
                onUp = onHitUp
            ),
        contentAlignment = Alignment.Center
    ) {
        if (state.currentMode == AppMode.METRONOME) {
            // 节拍器模式：开启动效时展示机身与摇摆指针；关闭动效时展示纯白色块（无指针）
            Image(
                painter = painterResource(if (state.isAnimationEnabled) Res.drawable.ic_metronome_body else Res.drawable.ic_metronome_solid),
                contentDescription = "节拍器机身",
                modifier = Modifier.fillMaxSize()
            )
            // 中间粗线条摆针，仅在开启左上方动效按钮时显示并左右律动
            if (state.isAnimationEnabled) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = metronomeAngle
                            transformOrigin = TransformOrigin(0.5f, 0.74f)
                        }
                ) {
                    val pivotX = size.width * 0.5f
                    val pivotY = size.height * 0.74f
                    val topY = size.height * 0.24f
                    val strokeW = size.width * 0.022f
                    drawLine(
                        color = Color(0xFF111111),
                        start = Offset(pivotX, pivotY),
                        end = Offset(pivotX, topY),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
        } else {
            // 木鱼 / 电子鼓模式：纯白实心剪影 + 真实固态打击动效
            Image(
                painter = painterResource(state.currentMode.icon),
                contentDescription = state.currentMode.displayName,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.scaleX = impactScale
                        this.scaleY = impactScale
                        this.translationY = impactOffsetY
                    }
            )
        }
    }
}

// 状态说明组件：移至下方预设按钮上方，显示当前模式副标题、番茄钟阶段标签或定时倒计时胶囊
@Composable
private fun StatusExplanation(
    state: WoodenFishUiState,
    isLandscape: Boolean,
    onOpenAutoKnockDialog: () -> Unit
) {
    val strings = LocalAppStrings.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val baseSubtitle = if (state.subtitle in listOf("正念", "节拍", "律动", "专注", "Mindfulness", "Metronome", "Groove", "Focus")) {
            state.currentMode.getLocalizedDefaultSubtitle(strings)
        } else {
            state.subtitle
        }
        val isPomodoroMultiStage = state.currentMode == AppMode.POMODORO && state.pomodoroStages.size > 1

        // 非多阶段番茄钟时显示副标题与状态信息（如 60 BPM）；多阶段番茄钟时满足需求：展示多阶段标签
        if (!isPomodoroMultiStage) {
            val subtitleText = if (state.currentMode == AppMode.POMODORO) {
                val stageIdx = state.currentPomodoroStageIndex
                val curStageMins = state.pomodoroStages.getOrElse(stageIdx) { 25 }
                val minUnit = strings.minuteUnit
                "$baseSubtitle · $curStageMins$minUnit"
            } else {
                "$baseSubtitle · ${state.bpm} BPM"
            }

            Text(
                text = subtitleText,
                color = Color(0xFF888888),
                fontSize = if (isLandscape) 12.5.sp else 13.5.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 番茄钟多阶段标签指示（一行同时显示所有阶段，当前阶段稍微亮一些提醒）
        if (isPomodoroMultiStage) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.pomodoroStages.forEachIndexed { index, minutes ->
                    val isCurrent = index == state.currentPomodoroStageIndex
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCurrent) Color(0x33FFFFFF) else Color(0x14FFFFFF),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) Color(0x99FFFFFF) else Color(0xFF383838)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                        ) {
                            if (isCurrent && state.isPomodoroRunning) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(Color(0xFFEEEEEE), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = strings.stageLabel(index + 1, state.pomodoroStages.size, minutes),
                                color = if (isCurrent) Color(0xFFEEEEEE) else Color(0xFF757575),
                                fontSize = if (isLandscape) 11.5.sp else 11.sp,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // 定时倒计时微光药丸胶囊
        if (state.isTimerEnabled && state.currentMode != AppMode.POMODORO) {
            val m = state.timerRemainingSeconds / 60
            val s = state.timerRemainingSeconds % 60
            val timeStr = String.format("%02d:%02d", m, s)
            Surface(
                onClick = onOpenAutoKnockDialog,
                shape = RoundedCornerShape(12.dp),
                color = if (state.isAutoKnockEnabled) Color(0x33FFFFFF) else Color(0xFF222222),
                border = BorderStroke(
                    1.dp,
                    if (state.isAutoKnockEnabled) Color.White else Color(0xFF444444)
                )
            ) {
                Text(
                    text = if (state.isAutoKnockEnabled) "⏱️ ${strings.countdownLabel(timeStr)}" else "⏱️ ${strings.countdownLabel(timeStr)} (${strings.paused})",
                    color = if (state.isAutoKnockEnabled) Color.White else Color(0xFFB0B0B0),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun WoodenFishScreen(
    viewModel: MainViewModel,
    onPickCustomBgm: () -> Unit = {},
    onExitRequest: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val systemLocale = androidx.compose.ui.text.intl.Locale.current.language.lowercase()
    val isZh = when (state.appLanguage) {
        AppLanguage.SYSTEM -> systemLocale.startsWith("zh")
        AppLanguage.ZH -> true
        AppLanguage.EN -> false
    }
    val strings = if (isZh) StringsZh else StringsEn

    CompositionLocalProvider(LocalAppStrings provides strings) {
        var isTouchDown by remember { mutableStateOf(false) }


    // 自动节奏时驱动受力下压回弹动画
    var isAutoBouncing by remember { mutableStateOf(false) }
    LaunchedEffect(state.knockTrigger) {
        if (state.knockTrigger > 0L) {
            isAutoBouncing = true
            delay(80)
            isAutoBouncing = false
        }
    }

    val isKnocked = isTouchDown || isAutoBouncing
    val shouldAnimate = state.isAnimationEnabled && isKnocked

    // -------------------------------------------------------------
    // 实木/鼓面物理打击动效（去除果冻卡通横向拉伸，呈现稳重微下沉与无迟滞回弹）
    // -------------------------------------------------------------
    val impactScale by animateFloatAsState(
        targetValue = if (shouldAnimate) 0.965f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "impactScale"
    )
    val impactOffsetY by animateFloatAsState(
        targetValue = if (shouldAnimate) 4f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "impactOffsetY"
    )

    // -------------------------------------------------------------
    // 节拍器专属中间粗线条左右乒乓反复摆动动效（摆幅±12.5°，确保完全位于白区内部不越界）
    // -------------------------------------------------------------
    val metronomeTargetAngle = if (state.beatIndex == 0L) {
        0f
    } else if (state.beatIndex % 2L == 1L) {
        10.5f
    } else {
        -10.5f
    }
    val metronomeAnimDuration = (state.autoKnockIntervalMs * 0.85f).toInt().coerceIn(120, 500)
    val metronomeAngle by animateFloatAsState(
        targetValue = metronomeTargetAngle,
        animationSpec = tween(durationMillis = metronomeAnimDuration, easing = FastOutSlowInEasing),
        label = "metronomeAngle"
    )

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscapeOrientation = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isLandscape = isLandscapeOrientation || screenWidth > screenHeight
        var showModeDialog by remember { mutableStateOf(false) }

        // 全屏点击响应区域
        // 需求5：自动敲击状态下点击屏幕不发声且立即停止自动敲击；非自动敲击状态下才为手动敲击发声
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
                .detectInstantTap(
                    enabled = state.isFullScreenTapEnabled,
                    onDown = {
                        if (state.currentMode == AppMode.POMODORO) {
                            viewModel.onPomodoroTap()
                        } else if (state.isAutoKnockEnabled) {
                            viewModel.toggleAutoKnock(false)
                        } else {
                            isTouchDown = true
                            viewModel.onManualHit()
                        }
                    },
                    onUp = {
                        isTouchDown = false
                    }
                )
        )

        // -------------------------------------------------------------
        // 1. 顶栏全新工整布局：
        // 左侧【清屏模式 + 动效开关】，右侧【运行模式 + 计时节奏设置 + 声音设置 + 软件设置】
        // -------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (isLandscape) 6.dp else 12.dp
                )
                .height(52.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ◀ 左上角：【清屏开关】 + 【动效开关】
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 清屏开关按钮（置于左上角首位，满足需求6）
                IconButton(
                    onClick = { viewModel.toggleZenMode() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (state.isZenMode) Res.drawable.ic_visibility else Res.drawable.ic_visibility_off
                        ),
                        contentDescription = if (state.isZenMode) strings.exitZen else strings.enterZen,
                        tint = if (state.isZenMode) Color.White else Color(0xFF888888)
                    )
                }

                // 2. 物理打击动效开关
                IconButton(
                    onClick = { viewModel.toggleAnimation() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (state.isAnimationEnabled) Res.drawable.ic_auto_awesome else Res.drawable.ic_auto_awesome_outline
                        ),
                        contentDescription = strings.animationSwitch,
                        tint = if (state.isAnimationEnabled) Color.White else Color(0xFF888888)
                    )
                }

                // 3. 播放/暂停控制按钮（纯黑白极简无色，短按播放/暂停，长按重置时间）
                val isRunning = if (state.currentMode == AppMode.POMODORO) state.isPomodoroRunning else state.isAutoKnockEnabled
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .pointerInput(isRunning, state.currentMode) {
                            detectTapGestures(
                                onTap = {
                                    if (state.currentMode == AppMode.POMODORO) {
                                        if (state.isPomodoroRunning) viewModel.pausePomodoro() else viewModel.resumePomodoro()
                                    } else {
                                        viewModel.toggleAutoKnock(!state.isAutoKnockEnabled)
                                    }
                                },
                                onLongPress = {
                                    if (state.currentMode == AppMode.POMODORO) {
                                        viewModel.restartPomodoro()
                                    } else {
                                        if (state.isTimerEnabled) {
                                            viewModel.resetTimer()
                                        }
                                        viewModel.toggleAutoKnock(true)
                                    }
                                    if (state.vibrationMs > 0) {
                                        viewModel.audioPlayer.vibrateManualKnock(40)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isRunning) {
                        BwPauseIcon(
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    } else {
                        BwPlayIcon(
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF888888)
                        )
                    }
                }
            }

            // ▶ 右上角：【运行模式】 + 【计时/节奏设置】 + 【声音设置】 + 【软件设置】
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 0. 运行模式切换按钮 (满足需求6：独立设立按钮并位于时间设置左侧)
                IconButton(
                    onClick = { showModeDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(state.currentMode.icon),
                        contentDescription = "${strings.modeRunning}：${state.currentMode.getLocalizedDisplayName(strings)}",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 1. 计时与节奏设置按钮 (满足需求5)
                val isSettingActive = if (state.currentMode == AppMode.POMODORO) state.isPomodoroRunning else state.isAutoKnockEnabled
                IconButton(
                    onClick = { viewModel.toggleAutoKnockDialog(true) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(if (isSettingActive) Res.drawable.ic_timer else Res.drawable.ic_timer_outline),
                        contentDescription = when (state.currentMode) {
                            AppMode.WOODEN_FISH -> strings.rhythmSettingTitle(strings.woodenFishName)
                            AppMode.METRONOME -> strings.rhythmSettingTitle(strings.metronomeName)
                            AppMode.DRUM -> strings.rhythmSettingTitle(strings.drumName)
                            AppMode.POMODORO -> strings.pomodoroSettings
                        },
                        tint = if (isSettingActive) Color.White else Color(0xFF888888)
                    )
                }

                // 2. 音乐与音效设置按钮（满足需求4：弹出专门声音设置窗口，放在右上角）
                IconButton(
                    onClick = { viewModel.toggleAudioSettingsDialog(true) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_music_note),
                        contentDescription = strings.audioSettings,
                        tint = when {
                            state.isBgmPlaying -> Color.White
                            state.customBgmUri != null -> Color(0xFFB0B0B0)
                            else -> Color(0xFF888888)
                        }
                    )
                }

                // 3. 软件设置按钮（固定位于最右侧）
                IconButton(
                    onClick = { viewModel.toggleSettingsDialog(true) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_settings),
                        contentDescription = strings.appSettings,
                        tint = Color(0xFF888888)
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 2. 大计数与图示（自适应屏幕大小，横屏左右对半分，竖屏上下排布）
        // -------------------------------------------------------------
        var showResetConfirmDialog by remember { mutableStateOf(false) }

        val counterText = if (state.currentMode == AppMode.POMODORO) {
            val m = state.pomodoroRemainingSeconds / 60
            val s = state.pomodoroRemainingSeconds % 60
            String.format("%02d:%02d", m, s)
        } else {
            "${state.count}"
        }

        // 图示大小适当缩小并自适应屏幕高宽 (满足需求1与需求2)
        val baseInstrumentSize = if (isLandscape) {
            minOf(175.dp, screenHeight * 0.46f)
        } else {
            if (screenHeight < 700.dp) 165.dp else 190.dp
        }
        val instrumentSize = if (state.isZenMode) {
            baseInstrumentSize * 1.15f
        } else {
            baseInstrumentSize
        }

        if (isLandscape && !state.isZenMode) {
            // 横屏非清屏模式：左右均分对称布局 (左侧大数字，右侧图示，满足需求6)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .padding(top = 40.dp, bottom = 80.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧 50%：居中大计数数字
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = counterText,
                        color = Color.White,
                        fontSize = if (screenHeight < 400.dp) 64.sp else 74.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(
                            fontFeatureSettings = "tnum",
                            textAlign = TextAlign.Center
                        ),
                        letterSpacing = 2.sp,
                        modifier = Modifier.pointerInput(state.count, state.currentMode) {
                            detectTapGestures(
                                onLongPress = {
                                    if (state.currentMode != AppMode.POMODORO && state.count > 0L) {
                                        showResetConfirmDialog = true
                                    }
                                }
                            )
                        }
                    )
                }

                // 右侧 50%：居中乐器图示
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    InstrumentView(
                        state = state,
                        instrumentSize = instrumentSize,
                        impactScale = impactScale,
                        impactOffsetY = impactOffsetY,
                        metronomeAngle = metronomeAngle,
                        onHitDown = {
                            if (state.currentMode == AppMode.POMODORO) {
                                viewModel.onPomodoroTap()
                            } else if (state.isAutoKnockEnabled) {
                                viewModel.toggleAutoKnock(false)
                            } else {
                                isTouchDown = true
                                viewModel.onManualHit()
                            }
                        },
                        onHitUp = { isTouchDown = false }
                    )
                }
            }
        } else if (!state.isZenMode) {
            // 竖屏非清屏模式：顶部居中大数字，中央居中图示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (screenHeight < 680.dp) 72.dp else 90.dp)
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = counterText,
                    color = Color.White,
                    fontSize = if (screenHeight < 680.dp) 64.sp else 74.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = TextStyle(
                        fontFeatureSettings = "tnum",
                        textAlign = TextAlign.Center
                    ),
                    letterSpacing = 2.sp,
                    modifier = Modifier.pointerInput(state.count, state.currentMode) {
                        detectTapGestures(
                            onLongPress = {
                                if (state.currentMode != AppMode.POMODORO && state.count > 0L) {
                                    showResetConfirmDialog = true
                                }
                            }
                        )
                    }
                )
            }

            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                InstrumentView(
                    state = state,
                    instrumentSize = instrumentSize,
                    impactScale = impactScale,
                    impactOffsetY = impactOffsetY,
                    metronomeAngle = metronomeAngle,
                    onHitDown = {
                        if (state.currentMode == AppMode.POMODORO) {
                            viewModel.onPomodoroTap()
                        } else if (state.isAutoKnockEnabled) {
                            viewModel.toggleAutoKnock(false)
                        } else {
                            isTouchDown = true
                            viewModel.onManualHit()
                        }
                    },
                    onHitUp = { isTouchDown = false }
                )
            }
        } else {
            // 清屏模式：番茄钟居中显示大时间，其他模式居中显示图示
            if (state.currentMode == AppMode.POMODORO) {
                val m = state.pomodoroRemainingSeconds / 60
                val s = state.pomodoroRemainingSeconds % 60
                val timeStr = String.format("%02d:%02d", m, s)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .detectInstantTap(
                            enabled = !state.isFullScreenTapEnabled,
                            onDown = { viewModel.onPomodoroTap() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timeStr,
                        color = Color.White,
                        fontSize = if (isLandscape) 84.sp else (if (screenHeight < 680.dp) 68.sp else 78.sp),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(
                            fontFeatureSettings = "tnum",
                            textAlign = TextAlign.Center
                        ),
                        letterSpacing = 4.sp
                    )
                }
            } else {
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    InstrumentView(
                        state = state,
                        instrumentSize = instrumentSize,
                        impactScale = impactScale,
                        impactOffsetY = impactOffsetY,
                        metronomeAngle = metronomeAngle,
                        onHitDown = {
                            if (state.isAutoKnockEnabled) {
                                viewModel.toggleAutoKnock(false)
                            } else {
                                isTouchDown = true
                                viewModel.onManualHit()
                            }
                        },
                        onHitUp = { isTouchDown = false }
                    )
                }
            }
        }

        // 长按清零计数确认弹窗
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                containerColor = Color(0xFF262626),
                title = {
                    Text(text = strings.resetCountConfirmTitle, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(text = "${strings.resetCountConfirmMsg} (${state.count})", fontSize = 14.sp, color = Color.LightGray)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetCount()
                            showResetConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(text = strings.confirm, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text(text = strings.cancel, color = Color.Gray)
                    }
                }
            )
        }

        // -------------------------------------------------------------
        // 4. 软件主界面 6 个自动敲击快捷直控按钮（加大按钮尺寸与触控面积）
        // -------------------------------------------------------------
        var showPomodoroCustomDialog by remember { mutableStateOf(false) }
        var showQuickCustomBpmDialog by remember { mutableStateOf(false) }
        var quickCustomBpmText by remember { mutableStateOf(state.customBpm.toString()) }

        val minUnit = strings.minuteUnit
        val pomodoroPresets = remember(strings) {
            listOf(
                "2$minUnit" to listOf(2),
                "5$minUnit" to listOf(5),
                "6+2$minUnit" to listOf(6, 2),
                "15+5$minUnit" to listOf(15, 5),
                "25+5$minUnit" to listOf(25, 5)
            )
        }
        val pomodoroTopPresets = remember(pomodoroPresets) { pomodoroPresets.take(3) }
        val pomodoroBottomPresets = remember(pomodoroPresets) { pomodoroPresets.drop(3) }

        val tempoPresets = remember(state.currentMode, strings) {
            state.currentMode.getLocalizedTempoPresets(strings)
        }
        val topRowPresets = remember(tempoPresets) { tempoPresets.take(3) }
        val bottomRowPresets = remember(tempoPresets) { tempoPresets.drop(3) }

        // -------------------------------------------------------------
        // 清屏模式专属：屏幕下方中间低干扰"暂停/继续"按钮
        // 按钮上方显示微光不明显文字：当前节奏频率或番茄钟时间安排
        // 短按暂停/继续 乒乓切换，长按重新开始计时
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = state.isZenMode,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isLandscape) 20.dp else 76.dp)
        ) {
            val isRunning = if (state.currentMode == AppMode.POMODORO) state.isPomodoroRunning else state.isAutoKnockEnabled
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 不明显文字显示当前的节奏频率或者番茄钟时间安排
                val zenInfoText = if (state.currentMode == AppMode.POMODORO) {
                    val stagesStr = state.pomodoroStages.joinToString("+") + strings.minuteUnit
                    if (state.pomodoroStages.size > 1) {
                        val stageIdx = state.currentPomodoroStageIndex
                        val curStageMins = state.pomodoroStages.getOrElse(stageIdx) { 25 }
                        "${strings.stageLabel(stageIdx + 1, state.pomodoroStages.size, curStageMins).substringBefore(':')} ($stagesStr)"
                    } else {
                        stagesStr
                    }
                } else {
                    "${state.bpm} BPM"
                }
                Text(
                    text = zenInfoText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0x66FFFFFF),
                    letterSpacing = 0.5.sp
                )

                Surface(
                    shape = CircleShape,
                    color = if (isRunning) Color(0x33FFFFFF) else Color(0x44000000),
                    border = BorderStroke(1.5.dp, if (isRunning) Color.White else Color(0x55AAAAAA)),
                    modifier = Modifier
                        .width(148.dp)
                        .height(50.dp)
                        .pointerInput(isRunning, state.currentMode) {
                            detectTapGestures(
                                onTap = {
                                    if (state.currentMode == AppMode.POMODORO) {
                                        if (state.isPomodoroRunning) viewModel.pausePomodoro() else viewModel.resumePomodoro()
                                    } else {
                                        viewModel.toggleAutoKnock(!state.isAutoKnockEnabled)
                                    }
                                },
                                onLongPress = {
                                    if (state.currentMode == AppMode.POMODORO) {
                                        viewModel.restartPomodoro()
                                    } else {
                                        if (state.isTimerEnabled) {
                                            viewModel.resetTimer()
                                        }
                                        viewModel.toggleAutoKnock(true)
                                    }
                                    if (state.vibrationMs > 0) {
                                        viewModel.audioPlayer.vibrateManualKnock(40)
                                    }
                                }
                            )
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (isRunning) {
                            BwPauseIcon(
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                        } else {
                            BwPlayIcon(
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFB0B0B0)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isRunning) strings.pause else strings.resume,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRunning) Color.White else Color(0xFFB0B0B0)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !state.isZenMode,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(
                    start = if (isLandscape) 32.dp else 18.dp,
                    end = if (isLandscape) 32.dp else 18.dp,
                    bottom = if (isLandscape) 14.dp else 26.dp
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 10.dp)
            ) {
                // 状态说明：移到下方预设按钮上方 (满足需求3)
                StatusExplanation(
                    state = state,
                    isLandscape = isLandscape,
                    onOpenAutoKnockDialog = { viewModel.toggleAutoKnockDialog(true) }
                )

                if (isLandscape) {
                // 横屏模式：6个按钮排成一排
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.currentMode == AppMode.POMODORO) {
                        pomodoroPresets.forEach { (label, stages) ->
                            val isSelected = state.pomodoroActivePreset == label
                            val isRunning = isSelected && state.isPomodoroRunning
                            Surface(
                                onClick = {
                                    viewModel.onPomodoroPresetClick(label, stages)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0x26FFFFFF) else Color.Transparent,
                                border = BorderStroke(
                                    width = 1.2.dp,
                                    color = if (isSelected) Color.White else Color(0xFF555555)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (isRunning) {
                                        BwPauseIcon(
                                            modifier = Modifier.size(11.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                    }
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // 自定义 (长按进入配置，短按暂停/继续)
                        val isCustomSelected = state.pomodoroActivePreset == strings.custom || state.pomodoroActivePreset == "自定义" || state.pomodoroActivePreset == "Custom"
                        val isCustomRunning = isCustomSelected && state.isPomodoroRunning
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCustomSelected) Color(0x26FFFFFF) else Color.Transparent,
                            border = BorderStroke(
                                width = 1.2.dp,
                                color = if (isCustomSelected) Color.White else Color(0xFF555555)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .pointerInput(isCustomSelected, isCustomRunning, state.pomodoroRemainingSeconds, state.pomodoroCustomSequence) {
                                    detectTapGestures(
                                        onTap = {
                                            if (!isCustomSelected) {
                                                // 未激活自定义：单击进入设置
                                                showPomodoroCustomDialog = true
                                            } else {
                                                // 已激活自定义：单击开关
                                                if (isCustomRunning) {
                                                    viewModel.pausePomodoro()
                                                } else if (state.pomodoroRemainingSeconds > 0L) {
                                                    viewModel.resumePomodoro()
                                                } else {
                                                    val stages = viewModel.parsePomodoroSequence(state.pomodoroCustomSequence)
                                                    viewModel.startPomodoro(strings.custom, stages)
                                                }
                                            }
                                        },
                                        onLongPress = {
                                            // 任何时间：长按均进行自定义设置
                                            showPomodoroCustomDialog = true
                                            if (state.vibrationMs > 0) viewModel.audioPlayer.vibrateManualKnock(30)
                                        }
                                    )
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val customLabel = if (isCustomSelected && state.pomodoroCustomSequence.isNotBlank()) {
                                    "${strings.custom} ${state.pomodoroCustomSequence}"
                                } else {
                                    strings.custom
                                }
                                if (isCustomRunning) {
                                    BwPauseIcon(
                                        modifier = Modifier.size(11.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                }
                                Text(
                                    text = customLabel,
                                    color = if (isCustomSelected) Color.White else Color(0xFFB0B0B0),
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        // 常规模式 5 个预设 (来自 state.currentMode.getTempoPresets())
                        tempoPresets.forEach { (label, value) ->
                            val isSelected = state.tempoActivePreset == label
                            val isPlayingThis = isSelected && state.isAutoKnockEnabled
                            Surface(
                                onClick = {
                                    viewModel.onTempoPresetClick(label, value)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isPlayingThis) Color(0x26FFFFFF) else Color.Transparent,
                                border = BorderStroke(
                                    width = 1.2.dp,
                                    color = if (isPlayingThis) Color.White else Color(0xFF555555)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (isPlayingThis) {
                                        BwPauseIcon(
                                            modifier = Modifier.size(11.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                    }
                                    Text(
                                        text = label,
                                        color = if (isPlayingThis) Color.White else Color(0xFFB0B0B0),
                                        fontSize = 13.sp,
                                        fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // 自定义 BPM (未激活时单击进设置，激活时单击开关，长按均进设置)
                        val isCustomSelected = state.tempoActivePreset == strings.custom || state.tempoActivePreset == "自定义" || state.tempoActivePreset == "Custom"
                        val isPlayingCustom = isCustomSelected && state.isAutoKnockEnabled
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isPlayingCustom) Color(0x26FFFFFF) else Color.Transparent,
                            border = BorderStroke(
                                width = 1.2.dp,
                                color = if (isPlayingCustom) Color.White else Color(0xFF555555)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .pointerInput(isCustomSelected, isPlayingCustom, state.customBpm) {
                                    detectTapGestures(
                                        onTap = {
                                            if (!isCustomSelected) {
                                                // 未激活自定义：单击进入设置
                                                quickCustomBpmText = state.customBpm.toString()
                                                showQuickCustomBpmDialog = true
                                            } else {
                                                // 已激活自定义：单击开关
                                                viewModel.onTempoCustomClick()
                                            }
                                        },
                                        onLongPress = {
                                            // 任何时间：长按均进行自定义设置
                                            quickCustomBpmText = state.customBpm.toString()
                                            showQuickCustomBpmDialog = true
                                            if (state.vibrationMs > 0) viewModel.audioPlayer.vibrateManualKnock(30)
                                        }
                                    )
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val customText = if (isCustomSelected) "${strings.custom} ${state.customBpm}" else strings.custom
                                if (isPlayingCustom) {
                                    BwPauseIcon(
                                        modifier = Modifier.size(11.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                }
                                Text(
                                    text = customText,
                                    color = if (isPlayingCustom) Color.White else Color(0xFFB0B0B0),
                                    fontSize = 13.sp,
                                    fontWeight = if (isPlayingCustom) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            } else {
                // 竖屏模式：保持经典上下两排 (3 + 3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.currentMode == AppMode.POMODORO) {
                        // 上排 3 个预设：2分钟，5分钟，10分钟
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pomodoroTopPresets.forEach { (label, stages) ->
                                val isSelected = state.pomodoroActivePreset == label
                                val isRunning = isSelected && state.isPomodoroRunning
                                Surface(
                                    onClick = {
                                        viewModel.onPomodoroPresetClick(label, stages)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0x26FFFFFF) else Color.Transparent,
                                    border = BorderStroke(
                                        width = 1.2.dp,
                                        color = if (isSelected) Color.White else Color(0xFF555555)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (isRunning) {
                                            BwPauseIcon(
                                                modifier = Modifier.size(11.dp),
                                                tint = if (isSelected) Color.White else Color(0xFFB0B0B0)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // 下排：25+5分钟，50+10分钟，自定义
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pomodoroBottomPresets.forEach { (label, stages) ->
                                val isSelected = state.pomodoroActivePreset == label
                                val isRunning = isSelected && state.isPomodoroRunning
                                Surface(
                                    onClick = {
                                        viewModel.onPomodoroPresetClick(label, stages)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0x26FFFFFF) else Color.Transparent,
                                    border = BorderStroke(
                                        width = 1.2.dp,
                                        color = if (isSelected) Color.White else Color(0xFF555555)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (isRunning) {
                                            BwPauseIcon(
                                                modifier = Modifier.size(11.dp),
                                                tint = if (isSelected) Color.White else Color(0xFFB0B0B0)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // 自定义按钮
                            val isCustomSelected = state.pomodoroActivePreset == strings.custom || state.pomodoroActivePreset == "自定义" || state.pomodoroActivePreset == "Custom"
                            val isCustomRunning = isCustomSelected && state.isPomodoroRunning
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCustomSelected) Color(0x26FFFFFF) else Color.Transparent,
                                border = BorderStroke(
                                    width = 1.2.dp,
                                    color = if (isCustomSelected) Color.White else Color(0xFF555555)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .pointerInput(isCustomSelected, isCustomRunning, state.pomodoroRemainingSeconds, state.pomodoroCustomSequence) {
                                        detectTapGestures(
                                            onTap = {
                                                if (!isCustomSelected) {
                                                    // 未激活自定义：单击进入设置
                                                    showPomodoroCustomDialog = true
                                                } else {
                                                    // 已激活自定义：单击开关
                                                    if (isCustomRunning) {
                                                        viewModel.pausePomodoro()
                                                    } else if (state.pomodoroRemainingSeconds > 0L) {
                                                        viewModel.resumePomodoro()
                                                    } else {
                                                        val stages = viewModel.parsePomodoroSequence(state.pomodoroCustomSequence)
                                                        viewModel.startPomodoro(strings.custom, stages)
                                                    }
                                                }
                                            },
                                            onLongPress = {
                                                // 任何时间：长按均进行自定义设置
                                                showPomodoroCustomDialog = true
                                                if (state.vibrationMs > 0) viewModel.audioPlayer.vibrateManualKnock(30)
                                            }
                                        )
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val customLabel = if (isCustomSelected && state.pomodoroCustomSequence.isNotBlank()) {
                                        "${strings.custom} ${state.pomodoroCustomSequence}"
                                    } else {
                                        strings.custom
                                    }
                                    if (isCustomRunning) {
                                        BwPauseIcon(
                                            modifier = Modifier.size(11.dp),
                                            tint = if (isCustomSelected) Color.White else Color(0xFFB0B0B0)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = customLabel,
                                        color = if (isCustomSelected) Color.White else Color(0xFFB0B0B0),
                                        fontSize = 12.sp,
                                        fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    } else {
                        // 常规模式上排 3 个预设
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            topRowPresets.forEach { (label, value) ->
                                val isSelected = state.tempoActivePreset == label
                                val isPlayingThis = isSelected && state.isAutoKnockEnabled
                                Surface(
                                    onClick = {
                                        viewModel.onTempoPresetClick(label, value)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isPlayingThis) Color(0x26FFFFFF) else Color.Transparent,
                                    border = BorderStroke(
                                        width = 1.2.dp,
                                        color = if (isPlayingThis) Color.White else Color(0xFF555555)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (isPlayingThis) {
                                            BwPauseIcon(
                                                modifier = Modifier.size(11.dp),
                                                tint = if (isPlayingThis) Color.White else Color(0xFFB0B0B0)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = label,
                                            color = if (isPlayingThis) Color.White else Color(0xFFB0B0B0),
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // 常规模式下排 2 个预设 + 自定义
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            bottomRowPresets.forEach { (label, value) ->
                                val isSelected = state.tempoActivePreset == label
                                val isPlayingThis = isSelected && state.isAutoKnockEnabled
                                Surface(
                                    onClick = {
                                        viewModel.onTempoPresetClick(label, value)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isPlayingThis) Color(0x26FFFFFF) else Color.Transparent,
                                    border = BorderStroke(
                                        width = 1.2.dp,
                                        color = if (isPlayingThis) Color.White else Color(0xFF555555)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (isPlayingThis) {
                                            BwPauseIcon(
                                                modifier = Modifier.size(11.dp),
                                                tint = if (isPlayingThis) Color.White else Color(0xFFB0B0B0)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = label,
                                            color = if (isPlayingThis) Color.White else Color(0xFFB0B0B0),
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // 自定义 BPM 按钮
                            val isCustomSelected = state.tempoActivePreset == strings.custom || state.tempoActivePreset == "自定义" || state.tempoActivePreset == "Custom"
                            val isPlayingCustom = isCustomSelected && state.isAutoKnockEnabled
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isPlayingCustom) Color(0x26FFFFFF) else Color.Transparent,
                                border = BorderStroke(
                                    width = 1.2.dp,
                                    color = if (isPlayingCustom) Color.White else Color(0xFF555555)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .pointerInput(isCustomSelected, isPlayingCustom, state.customBpm) {
                                        detectTapGestures(
                                            onTap = {
                                                if (!isCustomSelected) {
                                                    // 未激活自定义：单击进入设置
                                                    quickCustomBpmText = state.customBpm.toString()
                                                    showQuickCustomBpmDialog = true
                                                } else {
                                                    // 已激活自定义：单击开关
                                                    viewModel.onTempoCustomClick()
                                                }
                                            },
                                            onLongPress = {
                                                // 任何时间：长按均进行自定义设置
                                                quickCustomBpmText = state.customBpm.toString()
                                                showQuickCustomBpmDialog = true
                                                if (state.vibrationMs > 0) viewModel.audioPlayer.vibrateManualKnock(30)
                                            }
                                        )
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val customText = if (isCustomSelected) "${strings.custom} ${state.customBpm}" else strings.custom
                                    if (isPlayingCustom) {
                                        BwPauseIcon(
                                            modifier = Modifier.size(11.dp),
                                            tint = if (isPlayingCustom) Color.White else Color(0xFFB0B0B0)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = customText,
                                        color = if (isPlayingCustom) Color.White else Color(0xFFB0B0B0),
                                        fontSize = 13.sp,
                                        fontWeight = if (isPlayingCustom) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }



        // 快速自定义 BPM 弹窗（包含节拍速度设置滑杆与左右加减以1为单位的微调按钮）
        if (showQuickCustomBpmDialog) {
            var tempBpm by remember(showQuickCustomBpmDialog) {
                mutableIntStateOf(state.bpm)
            }

            AlertDialog(
                onDismissRequest = { showQuickCustomBpmDialog = false },
                containerColor = Color(0xFF262626),
                title = {
                    Text(
                        text = strings.customBpmTitle,
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. 醒目大字展示当前 BPM 与折算单拍秒数
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$tempBpm",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "BPM",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            val interval = 60f / tempBpm
                            Text(
                                text = "${strings.intervalSeconds(String.format("%.2f", interval))} · 30~300 BPM",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        // 2. 节拍速度设置滑杆与左右加减微调按钮（步进 1）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // [-] 减 1 按钮
                            FilledIconButton(
                                onClick = {
                                    if (tempBpm > 30) {
                                        tempBpm -= 1
                                        quickCustomBpmText = tempBpm.toString()
                                    }
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFF383838),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Text(text = "−", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }

                            // 滑杆
                            Slider(
                                value = tempBpm.toFloat(),
                                onValueChange = {
                                    val rounded = it.roundToInt().coerceIn(30, 300)
                                    tempBpm = rounded
                                    quickCustomBpmText = rounded.toString()
                                },
                                valueRange = 30f..300f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color(0xFF383838)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            // [+] 加 1 按钮
                            FilledIconButton(
                                onClick = {
                                    if (tempBpm < 300) {
                                        tempBpm += 1
                                        quickCustomBpmText = tempBpm.toString()
                                    }
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFF383838),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Text(text = "+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 3. 直接输入数字框
                        OutlinedTextField(
                            value = quickCustomBpmText,
                            onValueChange = { text ->
                                if (text.length <= 4 && text.all { it.isDigit() }) {
                                    quickCustomBpmText = text
                                    val parsed = text.toIntOrNull()
                                    if (parsed != null && parsed in 30..300) {
                                        tempBpm = parsed
                                    }
                                }
                            },
                            label = { Text(strings.directInputBpm, color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val parsed = quickCustomBpmText.toIntOrNull()?.coerceIn(30, 300) ?: tempBpm
                                    viewModel.setCustomBpm(parsed)
                                    showQuickCustomBpmDialog = false
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsed = quickCustomBpmText.toIntOrNull()?.coerceIn(30, 300) ?: tempBpm
                            viewModel.setCustomBpm(parsed)
                            showQuickCustomBpmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(text = strings.start, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showQuickCustomBpmDialog = false }) {
                        Text(text = strings.cancel, color = Color.Gray)
                    }
                }
            )
        }

        // 5. 自定义番茄钟倒计时弹窗（支持多阶段如 15+5）
        if (showPomodoroCustomDialog) {
            var tempSeq by remember(showPomodoroCustomDialog) {
                mutableStateOf(
                    if (state.pomodoroCustomSequence.isNotBlank() && state.pomodoroCustomSequence !in listOf("15+5", "25+5", "45+15", "50+10")) {
                        state.pomodoroCustomSequence
                    } else {
                        "5+2+1"
                    }
                )
            }
            val parsedStages = remember(tempSeq) {
                viewModel.parsePomodoroSequence(tempSeq)
            }
            val totalMinutes = remember(parsedStages) {
                parsedStages.sum()
            }

            AlertDialog(
                onDismissRequest = { showPomodoroCustomDialog = false },
                containerColor = Color(0xFF262626),
                title = {
                    Text(
                        text = strings.customPomodoroDialogTitle,
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = strings.customPomodoroDialogDesc,
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )

                        // 输入框
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = tempSeq,
                                onValueChange = { tempSeq = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 阶段解析预览
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF333333)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                val previewStr = parsedStages.mapIndexed { idx, m ->
                                    strings.stageLabel(idx + 1, parsedStages.size, m)
                                }.joinToString(" ➔ ")
                                Text(
                                    text = strings.planPreview(totalMinutes, previewStr),
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.setPomodoroCustomSequence(tempSeq)
                            showPomodoroCustomDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(text = strings.startPlan, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPomodoroCustomDialog = false }) {
                        Text(text = strings.cancel, color = Color.Gray)
                    }
                }
            )
        }

        // -------------------------------------------------------------
        // 6. 计时与节奏设置弹窗 (横屏下直接作为完整全屏界面呈现)
        // -------------------------------------------------------------
        if (state.showAutoKnockDialog) {
            AutoKnockDialog(
                state = state,
                isLandscape = isLandscape,
                onDismiss = { viewModel.toggleAutoKnockDialog(false) },
                onToggleAutoKnock = { viewModel.toggleAutoKnock(it) },
                onBpmChange = { viewModel.setBpm(it) },
                onToggleTimer = { viewModel.toggleTimer(it) },
                onTimerDurationChange = { viewModel.setTimerDuration(it) },
                onTogglePomodoro = {
                    if (state.isPomodoroRunning) {
                        viewModel.pausePomodoro()
                    } else if (state.pomodoroRemainingSeconds > 0L) {
                        viewModel.resumePomodoro()
                    } else {
                        val stages = viewModel.parsePomodoroSequence(state.pomodoroCustomSequence)
                        viewModel.startPomodoro(strings.custom, stages)
                    }
                },
                onPomodoroPresetClick = { label, stages ->
                    viewModel.onPomodoroPresetClick(label, stages)
                },
                onPomodoroCustomSeqChange = { seq ->
                    viewModel.setPomodoroCustomSequence(seq)
                },
                onParsePomodoroSeq = { seq ->
                    viewModel.parsePomodoroSequence(seq)
                },
                onTogglePomodoroSound = { viewModel.togglePomodoroSound() },
                onSoundIndexChange = { viewModel.setSoundIndex(it) },
                onTempoPresetClick = { label, bpm ->
                    viewModel.onTempoPresetClick(label, bpm)
                },
                onTempoCustomClick = {
                    viewModel.onTempoCustomClick()
                },
                onSetCustomBpm = {
                    viewModel.setCustomBpm(it)
                }
            )
        }

        // -------------------------------------------------------------
        // 7. 运行模式选择弹窗 (满足需求6)
        // -------------------------------------------------------------
        if (showModeDialog) {
            AlertDialog(
                onDismissRequest = { showModeDialog = false },
                containerColor = Color(0xFF1E1E1E),
                title = {
                    Text(
                        text = strings.selectModeTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppMode.entries.forEach { mode ->
                            val isSelected = state.currentMode == mode
                            Surface(
                                onClick = {
                                    viewModel.setAppMode(mode)
                                    showModeDialog = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0x26FFFFFF) else Color(0xFF282828),
                                border = if (isSelected) BorderStroke(1.2.dp, Color.White) else BorderStroke(1.dp, Color(0xFF383838)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(mode.icon),
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.getLocalizedDisplayName(strings),
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFFE0E0E0)
                                        )
                                        val modeDesc = when (mode) {
                                            AppMode.WOODEN_FISH -> strings.woodenFishDesc
                                            AppMode.METRONOME -> strings.metronomeDesc
                                            AppMode.DRUM -> strings.drumDesc
                                            AppMode.POMODORO -> strings.pomodoroDesc
                                        }
                                        Text(
                                            text = modeDesc,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF888888)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showModeDialog = false }) {
                        Text(text = strings.close, color = Color.White)
                    }
                }
            )
        }

        // -------------------------------------------------------------
        // 8. 音乐与音效设置弹窗 (满足需求4)
        // -------------------------------------------------------------
        if (state.showAudioSettings) {
            AudioSettingsDialog(
                state = state,
                streamProvider = viewModel.audioStreamProvider,
                isLandscape = isLandscape,
                onDismiss = { viewModel.toggleAudioSettingsDialog(false) },
                onToggleBgm = { viewModel.toggleBgm() },
                onPickBgm = { onPickCustomBgm() },
                onClearBgm = { viewModel.clearCustomBgm() },
                onVolumeChange = { viewModel.updateBgmVolume(it) },
                onSoundIndexChange = { viewModel.setSoundIndex(it) },
                onTogglePomodoroSound = { viewModel.togglePomodoroSound() },
                onBpmChange = { viewModel.setBpm(it) }
            )
        }

        // -------------------------------------------------------------
        // 9. 软件设置弹窗 (横屏下直接作为完整全屏界面呈现)
        // -------------------------------------------------------------
        if (state.showSettings) {
            SettingsDialog(
                state = state,
                isLandscape = isLandscape,
                onDismiss = { viewModel.toggleSettingsDialog(false) },
                onSubtitleChange = { viewModel.updateSubtitle(it) },
                onVibrationChange = { viewModel.updateVibrationMs(it) },
                onOrientationChange = { viewModel.setScreenOrientation(it) },
                onFullScreenTapChange = { viewModel.setFullScreenTap(it) },
                onResetCount = { viewModel.resetCount() },
                onLanguageChange = { viewModel.setAppLanguage(it) },
                onExitRequest = onExitRequest
            )
        }
    }
    }
}


