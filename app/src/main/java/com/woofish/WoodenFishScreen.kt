package com.woofish

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// 触碰即响 (Touch DOWN 零延迟) 辅助修饰符
fun Modifier.detectInstantTap(
    enabled: Boolean = true,
    onDown: () -> Unit,
    onUp: () -> Unit
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

@Composable
fun WoodenFishScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 手指瞬时触控状态（用于按下瞬间立刻发声与下压动画）
    var isTouchDown by remember { mutableStateOf(false) }

    // 本地音频选择器 (SAF)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            var displayName = "本地音乐"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.setCustomBgm(uri.toString(), displayName)
        }
    }

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
        12.5f
    } else {
        -12.5f
    }
    val metronomeAnimDuration = (state.autoKnockIntervalMs * 0.85f).toInt().coerceIn(120, 500)
    val metronomeAngle by animateFloatAsState(
        targetValue = metronomeTargetAngle,
        animationSpec = tween(durationMillis = metronomeAnimDuration, easing = FastOutSlowInEasing),
        label = "metronomeAngle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .systemBarsPadding()
    ) {
        // 全屏点击响应区域
        // 需求5：自动敲击状态下点击屏幕不发声且立即停止自动敲击；非自动敲击状态下才为手动敲击发声
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
                .detectInstantTap(
                    enabled = state.isFullScreenTapEnabled,
                    onDown = {
                        if (state.isAutoKnockEnabled) {
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
                            audioPickerLauncher.launch(arrayOf("audio/*"))
                        } else {
                            viewModel.toggleBgm()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_music_note),
                        contentDescription = if (state.customBgmUri == null) "选择本地背景音乐" else "背景音乐开关",
                        tint = when {
                            state.isBgmPlaying -> Color(0xFFFFD54F)
                            state.customBgmUri != null -> Color.White
                            else -> Color(0xFF555555)
                        }
                    )
                }

                // 2. 物理打击动效开关
                IconButton(
                    onClick = { viewModel.toggleAnimation() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (state.isAnimationEnabled) R.drawable.ic_auto_awesome else R.drawable.ic_auto_awesome_outline
                        ),
                        contentDescription = "动效开关",
                        tint = if (state.isAnimationEnabled) Color(0xFFFFD54F) else Color(0xFF555555)
                    )
                }

                // 3. 当前模式下的专属音效切换胶囊按钮
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
                        color = Color(0xFFCCCCCC),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // ▶ 右上角：【自动节奏/节拍器设置】 + 【清屏开关】 + 【软件设置】
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 自动节奏设置按钮 (包含清屏模式下的极简倒计时微显示)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.isTimerEnabled && state.isZenMode) {
                        val m = state.timerRemainingSeconds / 60
                        val s = state.timerRemainingSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", m, s),
                            color = if (state.isAutoKnockEnabled) Color(0xFFFFD54F) else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleAutoKnockDialog(true) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (state.isAutoKnockEnabled) R.drawable.ic_timer else R.drawable.ic_timer_outline
                            ),
                            contentDescription = "自动节奏设置",
                            tint = if (state.isAutoKnockEnabled) Color(0xFFFFD54F) else Color(0xFF999999)
                        )
                    }
                }

                // 2. 清屏开关按钮
                IconButton(
                    onClick = { viewModel.toggleZenMode() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (state.isZenMode) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                        ),
                        contentDescription = if (state.isZenMode) "退出清屏" else "进入清屏",
                        tint = if (state.isZenMode) Color(0xFFFFD54F) else Color(0xFF999999)
                    )
                }

                // 3. 软件设置按钮（固定位于最右侧）
                IconButton(
                    onClick = { viewModel.toggleSettingsDialog(true) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "软件设置",
                        tint = Color(0xFF999999)
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
                .fillMaxWidth()
                .padding(top = 112.dp)
                .align(Alignment.TopCenter)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${state.count}",
                    color = Color.White,
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = TextStyle(
                        fontFeatureSettings = "tnum",
                        textAlign = TextAlign.Center
                    ),
                    letterSpacing = 2.sp,
                    modifier = Modifier.pointerInput(state.count) {
                        detectTapGestures(
                            onLongPress = {
                                if (state.count > 0L) {
                                    showResetConfirmDialog = true
                                }
                            }
                        )
                    }
                )
                Text(
                    text = state.subtitle,
                    color = Color(0xFF555555),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                // 首页倒计时浮动微光药丸胶囊
                if (state.isTimerEnabled) {
                    val m = state.timerRemainingSeconds / 60
                    val s = state.timerRemainingSeconds % 60
                    val timeStr = String.format("%02d:%02d", m, s)
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = { viewModel.toggleAutoKnockDialog(true) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (state.isAutoKnockEnabled) Color(0x33FFD54F) else Color(0xFF222222),
                        border = BorderStroke(
                            1.dp,
                            if (state.isAutoKnockEnabled) Color(0x88FFD54F) else Color(0xFF444444)
                        )
                    ) {
                        Text(
                            text = if (state.isAutoKnockEnabled) "⏱️ 倒计时 $timeStr" else "⏱️ 定时 $timeStr (待开始)",
                            color = if (state.isAutoKnockEnabled) Color(0xFFFFD54F) else Color.LightGray,
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
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
        // 3. 居中乐器主体
        // 节拍器模式呈现稳定加宽底座机身 + 中间粗线条乒乓反复摆动（绝不超出白色色块区域）
        // 其他模式（木鱼、电子鼓）呈现纯白实心剪影与真实固态打击动效
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.Center)
                .detectInstantTap(
                    enabled = !state.isFullScreenTapEnabled,
                    onDown = {
                        if (state.isAutoKnockEnabled) {
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
                    painter = painterResource(id = R.drawable.ic_metronome_body),
                    contentDescription = "节拍器机身",
                    modifier = Modifier.fillMaxSize()
                )
                // 中间粗线条摆针，左右乒乓摆动，严格收纳在白色区域内
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = metronomeAngle
                            transformOrigin = TransformOrigin(0.5f, 0.74f)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 58.dp)
                            .width(5.5.dp)
                            .height(115.dp)
                            .background(Color(0xFF111111), RoundedCornerShape(3.dp))
                    )
                }
            } else {
                // 木鱼 / 电子鼓模式：纯白实心剪影 + 真实固态打击动效
                Image(
                    painter = painterResource(id = state.currentMode.iconResId),
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

        // -------------------------------------------------------------
        // 4. 软件主界面 6 个自动敲击快捷直控按钮（加大按钮尺寸与触控面积）
        // -------------------------------------------------------------
        var showQuickCustomBpmDialog by remember { mutableStateOf(false) }
        var quickCustomBpmText by remember { mutableStateOf(state.bpm.toString()) }

        val tempoPresets = remember(state.currentMode) {
            state.currentMode.getTempoPresets()
        }
        val topRowPresets = tempoPresets.take(3)
        val bottomRowPresets = tempoPresets.drop(3)
        val isCustomBpm = state.bpm !in listOf(30, 60, 90, 120, 150)

        AnimatedVisibility(
            visible = !state.isZenMode,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 18.dp, end = 18.dp, bottom = 28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 上排 3 档速度 (30, 60, 90)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    topRowPresets.forEach { (label, value) ->
                        val isPlayingThis = state.isAutoKnockEnabled && state.bpm == value
                        Surface(
                            onClick = {
                                if (isPlayingThis) {
                                    viewModel.toggleAutoKnock(false)
                                } else {
                                    viewModel.setBpm(value)
                                    viewModel.toggleAutoKnock(true)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isPlayingThis) Color(0x33FFD54F) else Color.Transparent,
                            border = BorderStroke(
                                width = 1.2.dp,
                                color = if (isPlayingThis) Color(0xFFFFD54F) else Color(0xFF555555)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = if (isPlayingThis) "⏸ $label" else label,
                                    color = if (isPlayingThis) Color(0xFFFFD54F) else Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 下排 3 档速度 (120, 150, 自定义)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    bottomRowPresets.forEach { (label, value) ->
                        val isPlayingThis = state.isAutoKnockEnabled && state.bpm == value
                        Surface(
                            onClick = {
                                if (isPlayingThis) {
                                    viewModel.toggleAutoKnock(false)
                                } else {
                                    viewModel.setBpm(value)
                                    viewModel.toggleAutoKnock(true)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isPlayingThis) Color(0x33FFD54F) else Color.Transparent,
                            border = BorderStroke(
                                width = 1.2.dp,
                                color = if (isPlayingThis) Color(0xFFFFD54F) else Color(0xFF555555)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = if (isPlayingThis) "⏸ $label" else label,
                                    color = if (isPlayingThis) Color(0xFFFFD54F) else Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 第 6 档：自定义
                    val isPlayingCustom = state.isAutoKnockEnabled && isCustomBpm
                    Surface(
                        onClick = {
                            if (isPlayingCustom) {
                                viewModel.toggleAutoKnock(false)
                            } else {
                                quickCustomBpmText = state.bpm.toString()
                                showQuickCustomBpmDialog = true
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPlayingCustom) Color(0x33FFD54F) else Color.Transparent,
                        border = BorderStroke(
                            width = 1.2.dp,
                            color = if (isPlayingCustom) Color(0xFFFFD54F) else Color(0xFF555555)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val customText = if (isCustomBpm) "自定义 ${state.bpm}" else "自定义 ✍️"
                            Text(
                                text = if (isPlayingCustom) "⏸ $customText" else customText,
                                color = if (isPlayingCustom) Color(0xFFFFD54F) else Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = if (isPlayingCustom) FontWeight.Bold else FontWeight.Medium
                            )
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
                                    color = Color(0xFFFFD54F)
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
                                    thumbColor = Color(0xFFFFD54F),
                                    activeTrackColor = Color(0xFFFFD54F),
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
                                    viewModel.setBpm(parsed)
                                    viewModel.toggleAutoKnock(true)
                                    showQuickCustomBpmDialog = false
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD54F),
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
                            viewModel.setBpm(parsed)
                            viewModel.toggleAutoKnock(true)
                            showQuickCustomBpmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
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

        // -------------------------------------------------------------
        // 5. 自动节奏/BPM调节弹窗
        // -------------------------------------------------------------
        if (state.showAutoKnockDialog) {
            AutoKnockDialog(
                state = state,
                onDismiss = { viewModel.toggleAutoKnockDialog(false) },
                onToggleAutoKnock = { viewModel.toggleAutoKnock(it) },
                onBpmChange = { viewModel.setBpm(it) },
                onToggleTimer = { viewModel.toggleTimer(it) },
                onSetTimerDuration = { viewModel.setTimerDuration(it) }
            )
        }

        // -------------------------------------------------------------
        // 6. 软件设置弹窗
        // -------------------------------------------------------------
        if (state.showSettings) {
            SettingsDialog(
                state = state,
                onDismiss = { viewModel.toggleSettingsDialog(false) },
                onModeChange = { viewModel.setAppMode(it) },
                onSubtitleChange = { viewModel.updateSubtitle(it) },
                onVolumeChange = { viewModel.updateBgmVolume(it) },
                onVibrationChange = { viewModel.updateVibrationMs(it) },
                onFullScreenTapChange = { viewModel.setFullScreenTap(it) },
                onResetCount = { viewModel.resetCount() },
                onPickBgm = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                onClearBgm = { viewModel.clearCustomBgm() }
            )
        }
    }
}
