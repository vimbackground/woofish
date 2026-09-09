package com.woofish

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            awaitFirstDown(requireUnconsumed = false)
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
            delay(100)
            isAutoBouncing = false
        }
    }

    val isKnocked = isTouchDown || isAutoBouncing
    val shouldAnimate = state.isAnimationEnabled && isKnocked

    val scaleX by animateFloatAsState(
        targetValue = if (shouldAnimate) 1.06f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scaleX"
    )
    val scaleY by animateFloatAsState(
        targetValue = if (shouldAnimate) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scaleY"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (shouldAnimate) 12f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offsetY"
    )
    val rotation by animateFloatAsState(
        targetValue = if (shouldAnimate) -2.5f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .systemBarsPadding()
    ) {
        // 全屏点击响应区域（全屏模式下，响应顶栏下方的整个屏幕区域，避开顶栏按钮以免设置时误触发敲击发声）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
                .detectInstantTap(
                    enabled = state.isFullScreenTapEnabled,
                    onDown = {
                        isTouchDown = true
                        viewModel.onHit()
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

                // 2. 物理打击形变动效开关
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
                // 1. 自动节奏设置按钮
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
        // 2. 大计数与文字（等宽排版稳定无抖动，支持自定义副标题）
        // -------------------------------------------------------------
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
                    letterSpacing = 2.sp
                )
                Text(
                    text = state.subtitle,
                    color = Color(0xFF555555),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // -------------------------------------------------------------
        // 3. 居中乐器主体（根据模式呈现木鱼、节拍器、鼓，敲击时下压回弹）
        // -------------------------------------------------------------
        Image(
            painter = painterResource(id = state.currentMode.iconResId),
            contentDescription = state.currentMode.displayName,
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    this.translationY = offsetY
                    this.rotationZ = rotation
                }
                .detectInstantTap(
                    enabled = !state.isFullScreenTapEnabled,
                    onDown = {
                        isTouchDown = true
                        viewModel.onHit()
                    },
                    onUp = {
                        isTouchDown = false
                    }
                )
        )

        // -------------------------------------------------------------
        // 4. 软件主界面 6 个自动敲击快捷直控按钮（灰框、白字、无底色，支持清屏隐藏，视觉均衡）
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
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 上排 3 档速度 (30, 60, 90)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isPlayingThis) Color(0xFFFFD54F) else Color(0xFF666666)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = if (isPlayingThis) "⏸ $label" else label,
                                    color = if (isPlayingThis) Color(0xFFFFD54F) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // 下排 3 档速度 (120, 150, 自定义)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isPlayingThis) Color(0xFFFFD54F) else Color(0xFF666666)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = if (isPlayingThis) "⏸ $label" else label,
                                    color = if (isPlayingThis) Color(0xFFFFD54F) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Normal
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
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isPlayingCustom) Color(0xFFFFD54F) else Color(0xFF666666)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val customText = if (isCustomBpm) "自定义 ${state.bpm}" else "自定义 ✍️"
                            Text(
                                text = if (isPlayingCustom) "⏸ $customText" else customText,
                                color = if (isPlayingCustom) Color(0xFFFFD54F) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isPlayingCustom) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // 快速自定义 BPM 弹窗
        if (showQuickCustomBpmDialog) {
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "请输入 30 到 300 之间的每分钟拍数：",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                        OutlinedTextField(
                            value = quickCustomBpmText,
                            onValueChange = {
                                if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                    quickCustomBpmText = it
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val parsed = quickCustomBpmText.toIntOrNull()
                                    if (parsed != null && parsed in 30..300) {
                                        viewModel.setBpm(parsed)
                                        viewModel.toggleAutoKnock(true)
                                        showQuickCustomBpmDialog = false
                                    }
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
                            val parsed = quickCustomBpmText.toIntOrNull()
                            if (parsed != null && parsed in 30..300) {
                                viewModel.setBpm(parsed)
                                viewModel.toggleAutoKnock(true)
                                showQuickCustomBpmDialog = false
                            }
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
                onBpmChange = { viewModel.setBpm(it) }
            )
        }

        // -------------------------------------------------------------
        // 5. 软件设置弹窗
        // -------------------------------------------------------------
        if (state.showSettings) {
            SettingsDialog(
                state = state,
                onDismiss = { viewModel.toggleSettingsDialog(false) },
                onModeChange = { viewModel.setAppMode(it) },
                onSubtitleChange = { viewModel.updateSubtitle(it) },
                onVolumeChange = { viewModel.updateBgmVolume(it) },
                onFullScreenTapChange = { viewModel.setFullScreenTap(it) },
                onResetCount = { viewModel.resetCount() },
                onPickBgm = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                onClearBgm = { viewModel.clearCustomBgm() }
            )
        }
    }
}
