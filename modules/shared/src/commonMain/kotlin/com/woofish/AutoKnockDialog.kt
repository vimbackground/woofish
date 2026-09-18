package com.woofish

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun AutoKnockDialog(
    state: WoodenFishUiState,
    streamProvider: IAudioStreamProvider? = null,
    isLandscape: Boolean = false,
    onDismiss: () -> Unit,
    onToggleAutoKnock: (Boolean) -> Unit,
    onBpmChange: (Int) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onTogglePomodoro: () -> Unit = {},
    onPomodoroPresetClick: (String, List<Int>) -> Unit = { _, _ -> },
    onPomodoroCustomSeqChange: (String) -> Unit = {},
    onTogglePomodoroSound: () -> Unit = {},
    onParsePomodoroSeq: (String) -> List<Int> = { listOf(25) }
) {
    val coroutineScope = rememberCoroutineScope()

    var subtitleInput by remember { mutableStateOf(state.subtitle) }
    LaunchedEffect(state.subtitle) {
        subtitleInput = state.subtitle
    }

    var pomodoroCustomInput by remember(state.pomodoroCustomSequence) {
        mutableStateOf(if (state.pomodoroCustomSequence.isNotBlank()) state.pomodoroCustomSequence else "15+5")
    }
    val parsedStages = remember(pomodoroCustomInput) {
        onParsePomodoroSeq(pomodoroCustomInput)
    }
    val totalMinutes = remember(parsedStages) {
        parsedStages.sum()
    }

    // 智能环境音乐节奏侦测器
    val beatDetector = remember { AudioBeatDetector(streamProvider) }
    val detectorState by beatDetector.state.collectAsState()
    var autoSyncTempo by remember { mutableStateOf(false) }

    // 手动轻敲测速器 (Tap Tempo 辅助)
    val tapTempoTracker = remember { TapTempoTracker() }
    var tapFeedbackBpm by remember { mutableStateOf<Int?>(null) }

    // 麦克风录音权限管理
    var hasAudioPermission by remember {
        mutableStateOf(streamProvider?.hasPermission ?: true)
    }

    // 弹窗关闭或离开时安全释放麦克风资源
    DisposableEffect(Unit) {
        onDispose {
            beatDetector.stop()
        }
    }

    // 实时自动同步逻辑
    LaunchedEffect(detectorState.detectedBpm) {
        val detected = detectorState.detectedBpm
        if (autoSyncTempo && detected != null && detectorState.confidence > 0.35f && detected != state.bpm) {
            onBpmChange(detected)
        }
    }

    val intervalSec = 60f / state.bpm.coerceAtLeast(1)
    val titleText = when (state.currentMode) {
        AppMode.METRONOME -> "自动节拍器"
        AppMode.DRUM -> "自动鼓点节奏"
        AppMode.WOODEN_FISH -> "自动敲击木鱼"
        AppMode.POMODORO -> "番茄钟专注设置"
    }

    if (isLandscape) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF161616))
                .padding(horizontal = 32.dp, vertical = 20.dp)
        ) {
            // 顶栏：标题与完成返回
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = {
                        beatDetector.stop()
                        onDismiss()
                    }) {
                        Text("← 返回", color = Color(0xFFB0B0B0), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = titleText, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                }
                Button(
                    onClick = {
                        beatDetector.stop()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "完成", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 横屏左右双栏布局
            if (state.currentMode == AppMode.POMODORO) {
                // 番茄钟模式专属横屏布局
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    // 左侧列：倒计时开关 + 文案自定义 + 声音设置
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. 番茄钟倒计时开关
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "开启番茄钟倒计时", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                val remM = state.pomodoroRemainingSeconds / 60
                                val remS = state.pomodoroRemainingSeconds % 60
                                val timeStr = String.format("%02d:%02d", remM, remS)
                                Text(
                                    text = if (state.isPomodoroRunning) {
                                        "专注中 · 阶段 ${state.currentPomodoroStageIndex + 1}/${state.pomodoroStages.size} · 剩余 $timeStr"
                                    } else {
                                        "已暂停 · 当前阶段剩余 $timeStr"
                                    },
                                    fontSize = 12.sp,
                                    color = if (state.isPomodoroRunning) Color.White else Color(0xFF888888)
                                )
                            }
                            Switch(
                                checked = state.isPomodoroRunning,
                                onCheckedChange = { onTogglePomodoro() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF333333)
                                )
                            )
                        }

                        // 2. 专注文案自定义
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "专注显示文案", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (subtitleInput.isEmpty()) {
                                    Text("例如：专注、学习、工作、冥想", fontSize = 13.sp, color = Color(0xFF666666))
                                }
                                BasicTextField(
                                    value = subtitleInput,
                                    onValueChange = {
                                        subtitleInput = it
                                        onSubtitleChange(it)
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            // 常用快捷选项
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("专注", "学习", "工作", "阅读", "正念", "冥想").forEach { tag ->
                                    val isSelected = subtitleInput == tag
                                    Surface(
                                        onClick = {
                                            subtitleInput = tag
                                            onSubtitleChange(tag)
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF262626),
                                        border = if (isSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E))
                                    ) {
                                        Text(
                                            text = tag,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. 滴答音与到期提示
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "滴答音与到期提示", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                Text(
                                    text = if (state.isPomodoroSoundEnabled) "秒针轻柔滴答声与到期提示音已开启" else "已静音",
                                    fontSize = 12.sp,
                                    color = Color(0xFF888888)
                                )
                            }
                            Switch(
                                checked = state.isPomodoroSoundEnabled,
                                onCheckedChange = { onTogglePomodoroSound() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF333333)
                                )
                            )
                        }
                    }

                    // 右侧列：常用模版与多阶段自定义
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF242424),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "⏱️ 倒计时时段与多阶段规划",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Text(
                                    text = "常用快捷模版：",
                                    fontSize = 12.sp,
                                    color = Color(0xFF888888)
                                )

                                val quickList = listOf(
                                    "2分钟" to listOf(2),
                                    "5分钟" to listOf(5),
                                    "10分钟" to listOf(10),
                                    "25+5分钟" to listOf(25, 5),
                                    "50+10分钟" to listOf(50, 10),
                                    "15+5" to listOf(15, 5),
                                    "45+15" to listOf(45, 15)
                                )
                                val rows = quickList.chunked(3)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    rows.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            rowItems.forEach { (lbl, stages) ->
                                                val isSelected = state.pomodoroActivePreset == lbl
                                                Surface(
                                                    onClick = {
                                                        onPomodoroPresetClick(lbl, stages)
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF1E1E1E),
                                                    border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF444444)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = lbl,
                                                            fontSize = 12.sp,
                                                            color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "自定义连续阶段（用 + 连接，如 15+5、25+5+10）：",
                                    fontSize = 12.sp,
                                    color = Color(0xFF888888)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = pomodoroCustomInput,
                                        onValueChange = { pomodoroCustomInput = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        cursorBrush = SolidColor(Color.White),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                val previewStr = parsedStages.mapIndexed { idx, m ->
                                    "阶段${idx + 1}: ${m}分"
                                }.joinToString(" ➔ ")
                                Text(
                                    text = "规划预览 (共 ${totalMinutes} 分钟)：$previewStr",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )

                                Button(
                                    onClick = {
                                        onPomodoroCustomSeqChange(pomodoroCustomInput)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "应用并开始倒计时", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // 常规节奏模式横屏布局
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    // 左侧列：自动开关 + 文案自定义
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. 自动开关
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "开启自动节奏", fontSize = 15.sp, color = Color.White)
                                Text(
                                    text = if (state.isAutoKnockEnabled) {
                                        "运行中 · ${state.bpm} BPM (${String.format("%.2f", intervalSec)} 秒/拍)"
                                    } else {
                                        "已暂停 · 当前 ${state.bpm} BPM"
                                    },
                                    fontSize = 12.sp,
                                    color = if (state.isAutoKnockEnabled) Color.White else Color(0xFF888888)
                                )
                            }
                            Switch(
                                checked = state.isAutoKnockEnabled,
                                onCheckedChange = onToggleAutoKnock,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF333333)
                                )
                            )
                        }

                        // 2. 计时显示文案自定义
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "计时显示文案", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (subtitleInput.isEmpty()) {
                                    Text("例如：正念、计时、功德、节拍", fontSize = 13.sp, color = Color(0xFF666666))
                                }
                                BasicTextField(
                                    value = subtitleInput,
                                    onValueChange = {
                                        subtitleInput = it
                                        onSubtitleChange(it)
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            // 常用快捷选项
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("正念", "计时", "功德", "节拍", "律动", "计数").forEach { tag ->
                                    val isSelected = subtitleInput == tag
                                    Surface(
                                        onClick = {
                                            subtitleInput = tag
                                            onSubtitleChange(tag)
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF262626),
                                        border = if (isSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E))
                                    ) {
                                        Text(
                                            text = tag,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 右侧列：环境音乐测速 + 点击测速
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF242424),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "🎵 环境音乐测速",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (detectorState.isListening) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFF5252))
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            if (detectorState.isListening) {
                                                beatDetector.stop()
                                            } else {
                                                if (hasAudioPermission) {
                                                    beatDetector.start(coroutineScope)
                                                } else {
                                                    streamProvider?.requestPermission { granted ->
                                                        hasAudioPermission = granted
                                                        if (granted) {
                                                            beatDetector.start(coroutineScope)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (detectorState.isListening) Color(0xFF422020) else Color(0xFF333333)
                                    ) {
                                        Text(
                                            text = if (detectorState.isListening) "⏹️ 停止聆听" else "🎙️ 听音测速",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (detectorState.isListening) Color(0xFFFF8A80) else Color(0xFFB0B0B0),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = detectorState.isListening) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = detectorState.statusText,
                                            fontSize = 12.sp,
                                            color = Color.LightGray
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            val baseAmp = detectorState.amplitude.coerceIn(0.05f, 1f)
                                            val multipliers = listOf(0.7f, 1.0f, 1.3f, 0.9f, 0.6f)
                                            multipliers.forEach { mul ->
                                                val barHeight = (baseAmp * mul * 16).coerceIn(3f, 16f)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(barHeight.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(Color.White)
                                                )
                                            }
                                        }

                                        if (detectorState.detectedBpm != null) {
                                            val detected = detectorState.detectedBpm!!
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "$detected BPM",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = if (detectorState.confidence > 0.4f) "置信度: 优" else "置信度: 良好",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF888888)
                                                    )
                                                }

                                                Surface(
                                                    onClick = { onBpmChange(detected) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color.White
                                                ) {
                                                    Text(
                                                        text = "应用此速度",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Black,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (tapFeedbackBpm != null) "轻敲测得: ${tapFeedbackBpm} BPM" else "或跟随音乐节拍点击测速：",
                                        fontSize = 12.sp,
                                        color = if (tapFeedbackBpm != null) Color.White else Color(0xFF888888)
                                    )

                                    Surface(
                                        onClick = {
                                            val bpm = tapTempoTracker.recordTap()
                                            if (bpm != null) {
                                                tapFeedbackBpm = bpm
                                                onBpmChange(bpm)
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF333333)
                                    ) {
                                        Text(
                                            text = "🖐️ 点击测速",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFB0B0B0),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // 竖屏 AlertDialog
        AlertDialog(
            onDismissRequest = {
                beatDetector.stop()
                onDismiss()
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCCCCCC),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = titleText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                if (state.currentMode == AppMode.POMODORO) {
                    // 番茄钟竖屏设置内容
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. 开关
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "开启番茄钟倒计时", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                val remM = state.pomodoroRemainingSeconds / 60
                                val remS = state.pomodoroRemainingSeconds % 60
                                val timeStr = String.format("%02d:%02d", remM, remS)
                                Text(
                                    text = if (state.isPomodoroRunning) {
                                        "专注中 · 阶段 ${state.currentPomodoroStageIndex + 1}/${state.pomodoroStages.size} · 剩余 $timeStr"
                                    } else {
                                        "已暂停 · 当前阶段剩余 $timeStr"
                                    },
                                    fontSize = 12.sp,
                                    color = if (state.isPomodoroRunning) Color.White else Color(0xFF888888)
                                )
                            }
                            Switch(
                                checked = state.isPomodoroRunning,
                                onCheckedChange = { onTogglePomodoro() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF333333)
                                )
                            )
                        }

                        // 2. 专注文案
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "专注显示文案", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (subtitleInput.isEmpty()) {
                                    Text("例如：专注、学习、工作、冥想", fontSize = 13.sp, color = Color(0xFF666666))
                                }
                                BasicTextField(
                                    value = subtitleInput,
                                    onValueChange = {
                                        subtitleInput = it
                                        onSubtitleChange(it)
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("专注", "学习", "工作", "阅读", "正念", "冥想").forEach { tag ->
                                    val isSelected = subtitleInput == tag
                                    Surface(
                                        onClick = {
                                            subtitleInput = tag
                                            onSubtitleChange(tag)
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF262626),
                                        border = if (isSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E))
                                    ) {
                                        Text(
                                            text = tag,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. 模版与自定义阶段
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF242424),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "⏱️ 倒计时时段与多阶段规划",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val quickList = listOf(
                                    "2分钟" to listOf(2),
                                    "5分钟" to listOf(5),
                                    "10分钟" to listOf(10),
                                    "25+5分钟" to listOf(25, 5),
                                    "50+10分钟" to listOf(50, 10),
                                    "15+5" to listOf(15, 5)
                                )
                                val rows = quickList.chunked(3)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    rows.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            rowItems.forEach { (lbl, stages) ->
                                                val isSelected = state.pomodoroActivePreset == lbl
                                                Surface(
                                                    onClick = {
                                                        onPomodoroPresetClick(lbl, stages)
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF1E1E1E),
                                                    border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF444444)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = lbl,
                                                            fontSize = 11.5.sp,
                                                            color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "自定义连续阶段（用 + 连接，如 15+5、25+5+10）：",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF888888)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = pomodoroCustomInput,
                                        onValueChange = { pomodoroCustomInput = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        cursorBrush = SolidColor(Color.White),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                val previewStr = parsedStages.mapIndexed { idx, m ->
                                    "阶段${idx + 1}: ${m}分"
                                }.joinToString(" ➔ ")
                                Text(
                                    text = "规划预览 (共 ${totalMinutes} 分钟)：$previewStr",
                                    fontSize = 11.5.sp,
                                    color = Color.LightGray
                                )

                                Button(
                                    onClick = {
                                        onPomodoroCustomSeqChange(pomodoroCustomInput)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "应用此规划并开始", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                        }

                        // 4. 声音设置
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "滴答音与到期提示", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                Text(
                                    text = if (state.isPomodoroSoundEnabled) "秒针轻柔滴答声与到期提示音已开启" else "已静音",
                                    fontSize = 12.sp,
                                    color = Color(0xFF888888)
                                )
                            }
                            Switch(
                                checked = state.isPomodoroSoundEnabled,
                                onCheckedChange = { onTogglePomodoroSound() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF333333)
                                )
                            )
                        }
                    }
                } else {
                    // 常规节奏模式竖屏设置内容
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // 1. 自动开关
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "开启自动节奏", fontSize = 15.sp, color = Color.White)
                                Text(
                                    text = if (state.isAutoKnockEnabled) {
                                        "运行中 · ${state.bpm} BPM (${String.format("%.2f", intervalSec)} 秒/拍)"
                                    } else {
                                        "已暂停 · 当前 ${state.bpm} BPM"
                                    },
                                    fontSize = 12.sp,
                                    color = if (state.isAutoKnockEnabled) Color.White else Color(0xFF888888)
                                )
                            }
                            Switch(
                                checked = state.isAutoKnockEnabled,
                                onCheckedChange = onToggleAutoKnock,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF333333)
                                )
                            )
                        }

                        // 2. 计时显示文案自定义
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "计时显示文案", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (subtitleInput.isEmpty()) {
                                    Text("例如：正念、计时、功德、节拍", fontSize = 13.sp, color = Color(0xFF666666))
                                }
                                BasicTextField(
                                    value = subtitleInput,
                                    onValueChange = {
                                        subtitleInput = it
                                        onSubtitleChange(it)
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 13.5.sp),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            // 常用快捷选项
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("正念", "计时", "功德", "节拍", "律动", "计数").forEach { tag ->
                                    val isSelected = subtitleInput == tag
                                    Surface(
                                        onClick = {
                                            subtitleInput = tag
                                            onSubtitleChange(tag)
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF262626),
                                        border = if (isSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E))
                                    ) {
                                        Text(
                                            text = tag,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. 环境音乐节奏智能分析测速
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF242424),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "🎵 环境音乐测速",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (detectorState.isListening) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFF5252))
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = {
                                            if (detectorState.isListening) {
                                                beatDetector.stop()
                                            } else {
                                                if (hasAudioPermission) {
                                                    beatDetector.start(coroutineScope)
                                                } else {
                                                    streamProvider?.requestPermission { granted ->
                                                        hasAudioPermission = granted
                                                        if (granted) {
                                                            beatDetector.start(coroutineScope)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (detectorState.isListening) Color(0xFF422020) else Color(0xFF333333)
                                    ) {
                                        Text(
                                            text = if (detectorState.isListening) "⏹️ 停止聆听" else "🎙️ 听音测速",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (detectorState.isListening) Color(0xFFFF8A80) else Color(0xFFB0B0B0),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = detectorState.isListening) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = detectorState.statusText,
                                            fontSize = 12.sp,
                                            color = Color.LightGray
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            val baseAmp = detectorState.amplitude.coerceIn(0.05f, 1f)
                                            val multipliers = listOf(0.7f, 1.0f, 1.3f, 0.9f, 0.6f)
                                            multipliers.forEach { mul ->
                                                val barHeight = (baseAmp * mul * 16).coerceIn(3f, 16f)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(barHeight.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(Color.White)
                                                )
                                            }
                                        }

                                        if (detectorState.detectedBpm != null) {
                                            val detected = detectorState.detectedBpm!!
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF2E2E2E), RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "$detected BPM",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = if (detectorState.confidence > 0.4f) "置信度: 优" else "置信度: 良好",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF888888)
                                                    )
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        onClick = { onBpmChange(detected) },
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color.White
                                                    ) {
                                                        Text(
                                                            text = "应用此速度",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Black,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (tapFeedbackBpm != null) "轻敲测得: ${tapFeedbackBpm} BPM" else "或跟随音乐节拍点击测速：",
                                        fontSize = 12.sp,
                                        color = if (tapFeedbackBpm != null) Color.White else Color(0xFF888888)
                                    )

                                    Surface(
                                        onClick = {
                                            val bpm = tapTempoTracker.recordTap()
                                            if (bpm != null) {
                                                tapFeedbackBpm = bpm
                                                onBpmChange(bpm)
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF333333)
                                    ) {
                                        Text(
                                            text = "🖐️ 点击测速",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFB0B0B0),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        beatDetector.stop()
                        onDismiss()
                    }
                ) {
                    Text(text = "完成", color = Color.White)
                }
            }
        )
    }
}
