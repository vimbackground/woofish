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

@Composable
fun WoodenFishScreen(viewModel: MainViewModel, onPickCustomBgm: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .systemBarsPadding()
    ) {
        val isLandscape = maxWidth > maxHeight

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
        // 1. 顶栏全部保留：
        // 左侧【BGM + 动效 + 音效】，右侧【自动节奏设置 + 清屏 + 软件设置】
        // -------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ◀ 左上角：【BGM 开关】 + 【动效开关】 + 【音效切换】
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 背景音乐开关
                IconButton(
                    onClick = {
                        if (state.customBgmUri == null) {
                            onPickCustomBgm()
                        } else {
                            viewModel.toggleBgm()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_music_note),
                        contentDescription = if (state.customBgmUri == null) "选择本地背景音乐" else "背景音乐开关",
                        tint = when {
                            state.isBgmPlaying -> Color.White
                            state.customBgmUri != null -> Color(0xFFB0B0B0)
                            else -> Color(0xFF888888)
                        }
                    )
                }

                // 2. 物理打击动效开关
                IconButton(
                    onClick = { viewModel.toggleAnimation() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(if (state.isAnimationEnabled) Res.drawable.ic_auto_awesome else Res.drawable.ic_auto_awesome_outline
                        ),
                        contentDescription = "动效开关",
                        tint = if (state.isAnimationEnabled) Color.White else Color(0xFF888888)
                    )
                }

                // 3. 当前模式下的专属音效切换胶囊按钮 (番茄钟模式直接开关滴答音)
                if (state.currentMode == AppMode.POMODORO) {
                    val isSoundOn = state.isPomodoroSoundEnabled
                    Surface(
                        onClick = { viewModel.togglePomodoroSound() },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSoundOn) Color(0x33FFFFFF) else Color(0xFF222222),
                        border = if (isSoundOn) BorderStroke(1.dp, Color.White) else null,
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = if (isSoundOn) "🔊 滴答音" else "🔇 静音",
                            color = if (isSoundOn) Color.White else Color(0xFF888888),
                            fontSize = 13.sp,
                            fontWeight = if (isSoundOn) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    val currentSoundName = state.currentMode.soundNames.getOrNull(state.soundIndex)
                        ?: state.currentMode.soundNames.firstOrNull() ?: "音效"
                    Surface(
                        onClick = { viewModel.toggleSoundEffect() },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF222222),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = "🔊 $currentSoundName",
                            color = Color(0xFFB0B0B0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ▶ 右上角：【自动节奏/节拍器设置】 + 【清屏开关】 + 【软件设置】
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 自动节奏 / 番茄钟设置按钮
                val isSettingActive = if (state.currentMode == AppMode.POMODORO) state.isPomodoroRunning else state.isAutoKnockEnabled
                IconButton(
                    onClick = { viewModel.toggleAutoKnockDialog(true) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(if (isSettingActive) Res.drawable.ic_timer else Res.drawable.ic_timer_outline),
                        contentDescription = if (state.currentMode == AppMode.POMODORO) "番茄钟专注设置" else "自动节奏设置",
                        tint = if (isSettingActive) Color.White else Color(0xFF888888)
                    )
                }

                // 2. 清屏开关按钮
                IconButton(
                    onClick = { viewModel.toggleZenMode() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(if (state.isZenMode) Res.drawable.ic_visibility else Res.drawable.ic_visibility_off
                        ),
                        contentDescription = if (state.isZenMode) "退出清屏" else "进入清屏",
                        tint = if (state.isZenMode) Color.White else Color(0xFF888888)
                    )
                }

                // 3. 软件设置按钮（固定位于最右侧）
                IconButton(
                    onClick = { viewModel.toggleSettingsDialog(true) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_settings),
                        contentDescription = "软件设置",
                        tint = Color(0xFF888888)
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 2. 大计数与文字（等宽排版稳定无抖动，支持长按清零与倒计时浮动胶囊）
        // -------------------------------------------------------------
        var showResetConfirmDialog by remember { mutableStateOf(false) }

        AnimatedVisibility(
            visible = !state.isZenMode,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .then(
                    if (isLandscape) {
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 140.dp)
                            .width(320.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 112.dp)
                            .align(Alignment.TopCenter)
                    }
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = if (isLandscape) Modifier.width(320.dp) else Modifier.fillMaxWidth()
            ) {
                val counterText = if (state.currentMode == AppMode.POMODORO) {
                    val m = state.pomodoroRemainingSeconds / 60
                    val s = state.pomodoroRemainingSeconds % 60
                    String.format("%02d:%02d", m, s)
                } else {
                    "${state.count}"
                }

                Text(
                    text = counterText,
                    color = Color.White,
                    fontSize = if (isLandscape) 86.sp else 76.sp,
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

                // 番茄钟多阶段显示 或 竖屏常规副标题
                if (!isLandscape || state.currentMode == AppMode.POMODORO) {
                    val subtitleText = if (state.currentMode == AppMode.POMODORO) {
                        val stageCount = state.pomodoroStages.size
                        val stageIdx = state.currentPomodoroStageIndex
                        val curStageMins = state.pomodoroStages.getOrElse(stageIdx) { 25 }
                        if (stageCount in 2..3) {
                            val stagesDurationStr = state.pomodoroStages.joinToString(" + ") { "${it}分" }
                            "${state.subtitle} · 阶段 ${stageIdx + 1}/$stageCount ($stagesDurationStr)"
                        } else if (stageCount > 3) {
                            "${state.subtitle} · 阶段 ${stageIdx + 1}/$stageCount (${curStageMins}分)"
                        } else {
                            "${state.subtitle} (${curStageMins}分)"
                        }
                    } else {
                        state.subtitle
                    }

                    Text(
                        text = subtitleText,
                        color = Color(0xFF555555),
                        fontSize = if (state.currentMode == AppMode.POMODORO && state.pomodoroStages.size in 2..3) 16.sp else 18.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // 如果有多阶段，三阶段以内的，同时显示每阶段的时长标签，更清晰直观
                    if (state.currentMode == AppMode.POMODORO && state.pomodoroStages.size in 2..3) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            state.pomodoroStages.forEachIndexed { index, minutes ->
                                val isCurrent = index == state.currentPomodoroStageIndex
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) Color(0x33FFFFFF) else Color(0x14FFFFFF),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrent) Color.White else Color(0xFF3E3E3E)
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        if (isCurrent && state.isPomodoroRunning) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color.White, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = "阶段${index + 1}: ${minutes}分",
                                            color = if (isCurrent) Color.White else Color(0xFF888888),
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 状态胶囊指示器：按需求已移除番茄钟模式中间的"专注中/已暂停"胶囊，保持画面整洁
                if (state.isTimerEnabled && state.currentMode != AppMode.POMODORO) {
                    // 首页倒计时浮动微光药丸胶囊
                    val m = state.timerRemainingSeconds / 60
                    val s = state.timerRemainingSeconds % 60
                    val timeStr = String.format("%02d:%02d", m, s)
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = { viewModel.toggleAutoKnockDialog(true) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (state.isAutoKnockEnabled) Color(0x33FFFFFF) else Color(0xFF222222),
                        border = BorderStroke(
                            1.dp,
                            if (state.isAutoKnockEnabled) Color.White else Color(0xFF444444)
                        )
                    ) {
                        Text(
                            text = if (state.isAutoKnockEnabled) "⏱️ 倒计时 $timeStr" else "⏱️ 定时 $timeStr (待开始)",
                            color = if (state.isAutoKnockEnabled) Color.White else Color(0xFFB0B0B0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // 长按清零计数确认弹窗
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                containerColor = Color(0xFF262626),
                title = {
                    Text(text = "清零计数", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(text = "是否将当前累积的功德/击打次数 (${state.count}) 清零？", fontSize = 14.sp, color = Color.LightGray)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetCount()
                            showResetConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(text = "清零", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text(text = "取消", color = Color.Gray)
                    }
                }
            )
        }

        // -------------------------------------------------------------
        // 3. 居中区域：
        // 在番茄钟模式且清屏时：居中仅显示大字倒计时（完全不显示乐器图示，避免与数字重叠）；
        // 其他情况下（常规模式、以及番茄钟正常显示时）：居中显示乐器图示（横屏下位于右侧）
        // -------------------------------------------------------------
        if (state.isZenMode && state.currentMode == AppMode.POMODORO) {
            val m = state.pomodoroRemainingSeconds / 60
            val s = state.pomodoroRemainingSeconds % 60
            val timeStr = String.format("%02d:%02d", m, s)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .detectInstantTap(
                        enabled = !state.isFullScreenTapEnabled,
                        onDown = {
                            viewModel.onPomodoroTap()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeStr,
                    color = Color.White,
                    fontSize = if (isLandscape) 100.sp else 80.sp,
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
            val instrumentSize = if (state.isZenMode) {
                if (isLandscape) 250.dp else 240.dp
            } else {
                if (isLandscape) 240.dp else 240.dp
            }
            Box(
                modifier = Modifier
                    .size(instrumentSize)
                    .then(
                        if (state.isZenMode) {
                            Modifier.align(Alignment.Center)
                        } else if (isLandscape) {
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 160.dp)
                        } else {
                            Modifier.align(Alignment.Center)
                        }
                    )
                    .detectInstantTap(
                        enabled = !state.isFullScreenTapEnabled,
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
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (state.currentMode == AppMode.METRONOME) {
                    // 节拍器模式：大底座纯白稳定机身
                    Image(
                        painter = painterResource(Res.drawable.ic_metronome_body),
                        contentDescription = "节拍器机身",
                        modifier = Modifier.fillMaxSize()
                    )
                    // 中间粗线条摆针，以底部支点 (0.5f, 0.74f) 为旋转中心左右乒乓摆动，严格收纳在白色区域内
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
                        val strokeW = size.width * 0.022f // ~5.3dp, 醒目且完全在白色机身内
                        drawLine(
                            color = Color(0xFF111111),
                            start = Offset(pivotX, pivotY),
                            end = Offset(pivotX, topY),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
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

        // -------------------------------------------------------------
        // 4. 软件主界面 6 个自动敲击快捷直控按钮（加大按钮尺寸与触控面积）
        // -------------------------------------------------------------
        var showPomodoroCustomDialog by remember { mutableStateOf(false) }
        var showQuickCustomBpmDialog by remember { mutableStateOf(false) }
        var quickCustomBpmText by remember { mutableStateOf(state.customBpm.toString()) }

        val pomodoroPresets = remember {
            listOf(
                "2分钟" to listOf(2),
                "5分钟" to listOf(5),
                "10分钟" to listOf(10),
                "25+5分钟" to listOf(25, 5),
                "50+10分钟" to listOf(50, 10)
            )
        }
        val pomodoroTopPresets = remember { pomodoroPresets.take(3) }
        val pomodoroBottomPresets = remember { pomodoroPresets.drop(3) }

        val tempoPresets = remember(state.currentMode) {
            state.currentMode.getTempoPresets()
        }
        val topRowPresets = remember(tempoPresets) { tempoPresets.take(3) }
        val bottomRowPresets = remember(tempoPresets) { tempoPresets.drop(3) }

        // -------------------------------------------------------------
        // 清屏模式专属：屏幕下方中间低干扰"暂停/继续"按钮
        // 短按暂停/继续 乒乓切换，长按重新开始计时
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = state.isZenMode,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isLandscape) 60.dp else 92.dp)
        ) {
            val isRunning = if (state.currentMode == AppMode.POMODORO) state.isPomodoroRunning else state.isAutoKnockEnabled
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
                        text = if (isRunning) "暂停" else "继续",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRunning) Color.White else Color(0xFFB0B0B0)
                    )
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
                    start = if (isLandscape) 48.dp else 18.dp,
                    end = if (isLandscape) 48.dp else 18.dp,
                    bottom = if (isLandscape) 24.dp else 28.dp
                )
        ) {
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
                        val isCustomSelected = state.pomodoroActivePreset == "自定义"
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
                                                    viewModel.startPomodoro("自定义", stages)
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
                                    "自定义 ${state.pomodoroCustomSequence}"
                                } else {
                                    "自定义"
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
                        val isCustomSelected = state.tempoActivePreset == "自定义"
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
                                val customText = if (isCustomSelected) "自定义 ${state.customBpm}" else "自定义"
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
                            val isCustomSelected = state.pomodoroActivePreset == "自定义"
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
                                                        viewModel.startPomodoro("自定义", stages)
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
                                        "自定义 ${state.pomodoroCustomSequence}"
                                    } else {
                                        "自定义"
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
                            val isCustomSelected = state.tempoActivePreset == "自定义"
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
                                    val customText = if (isCustomSelected) "自定义 ${state.customBpm}" else "自定义"
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
                        text = "自定义节拍速度 (BPM)",
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
                                text = "约 ${String.format("%.2f", interval)} 秒/拍 · 调节范围 30~300",
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
                            label = { Text("直接输入数值", color = Color.Gray, fontSize = 12.sp) },
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
                        Text(text = "开始", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showQuickCustomBpmDialog = false }) {
                        Text(text = "取消", color = Color.Gray)
                    }
                }
            )
        }

        // 5. 自定义番茄钟倒计时弹窗（支持多阶段如 15+5）
        if (showPomodoroCustomDialog) {
            var tempSeq by remember(showPomodoroCustomDialog) {
                mutableStateOf(if (state.pomodoroCustomSequence.isNotBlank()) state.pomodoroCustomSequence else "15+5")
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
                        text = "自定义番茄钟倒计时",
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
                            text = "支持单阶段或多阶段连续倒计时（用 + 连接，到时间自动衔接下一阶段）：",
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
                                Text(
                                    text = "倒计时规划预览 (共 ${totalMinutes} 分钟)：",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val previewStr = parsedStages.mapIndexed { idx, m ->
                                    "阶段${idx + 1}: ${m}分"
                                }.joinToString(" ➔ ")
                                Text(
                                    text = previewStr,
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
                        Text(text = "规划", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPomodoroCustomDialog = false }) {
                        Text(text = "取消", color = Color.Gray)
                    }
                }
            )
        }

        // -------------------------------------------------------------
        // 6. 自动节奏/BPM调节弹窗 (横屏下直接作为完整全屏界面呈现)
        // -------------------------------------------------------------
        if (state.showAutoKnockDialog) {
            AutoKnockDialog(
                state = state,
                streamProvider = viewModel.audioStreamProvider,
                isLandscape = isLandscape,
                onDismiss = { viewModel.toggleAutoKnockDialog(false) },
                onToggleAutoKnock = { viewModel.toggleAutoKnock(it) },
                onBpmChange = { viewModel.setBpm(it) },
                onSubtitleChange = { viewModel.updateSubtitle(it) },
                onTogglePomodoro = {
                    if (state.isPomodoroRunning) {
                        viewModel.pausePomodoro()
                    } else if (state.pomodoroRemainingSeconds > 0L) {
                        viewModel.resumePomodoro()
                    } else {
                        val stages = viewModel.parsePomodoroSequence(state.pomodoroCustomSequence)
                        viewModel.startPomodoro("自定义", stages)
                    }
                },
                onPomodoroPresetClick = { label, stages ->
                    viewModel.onPomodoroPresetClick(label, stages)
                },
                onPomodoroCustomSeqChange = { seq ->
                    viewModel.setPomodoroCustomSequence(seq)
                },
                onTogglePomodoroSound = {
                    viewModel.togglePomodoroSound()
                },
                onParsePomodoroSeq = { seq ->
                    viewModel.parsePomodoroSequence(seq)
                }
            )
        }

        // -------------------------------------------------------------
        // 7. 软件设置弹窗 (横屏下直接作为完整全屏界面呈现)
        // -------------------------------------------------------------
        if (state.showSettings) {
            SettingsDialog(
                state = state,
                isLandscape = isLandscape,
                onDismiss = { viewModel.toggleSettingsDialog(false) },
                onModeChange = { viewModel.setAppMode(it) },
                onVolumeChange = { viewModel.updateBgmVolume(it) },
                onVibrationChange = { viewModel.updateVibrationMs(it) },
                onFullScreenTapChange = { viewModel.setFullScreenTap(it) },
                onResetCount = { viewModel.resetCount() },
                onPickBgm = { onPickCustomBgm() },
                onClearBgm = { viewModel.clearCustomBgm() }
            )
        }
    }
}


