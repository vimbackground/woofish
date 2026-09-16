package com.woofish

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

@Composable
fun AutoKnockDialog(
    state: WoodenFishUiState,
    onDismiss: () -> Unit,
    onToggleAutoKnock: (Boolean) -> Unit,
    onBpmChange: (Int) -> Unit,
    onToggleTimer: (Boolean) -> Unit,
    onSetTimerDuration: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 智能环境音乐节奏侦测器
    val beatDetector = remember { AudioBeatDetector() }
    val detectorState by beatDetector.state.collectAsState()
    var autoSyncTempo by remember { mutableStateOf(false) }

    // 手动轻敲测速器 (Tap Tempo 辅助)
    val tapTempoTracker = remember { TapTempoTracker() }
    var tapFeedbackBpm by remember { mutableStateOf<Int?>(null) }

    // 麦克风录音权限管理
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            beatDetector.start(coroutineScope)
        }
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
                text = when (state.currentMode) {
                    AppMode.METRONOME -> "自动节拍器"
                    AppMode.DRUM -> "自动鼓点节奏"
                    AppMode.WOODEN_FISH -> "自动敲击木鱼"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
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
                            color = if (state.isAutoKnockEnabled) Color(0xFFFFD54F) else Color.Gray
                        )
                    }
                    Switch(
                        checked = state.isAutoKnockEnabled,
                        onCheckedChange = onToggleAutoKnock,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFFD54F),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF333333)
                        )
                    )
                }

                // 2. 倒计时器 (定时停止)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "倒计时器", fontSize = 15.sp, color = Color.White)
                            val timerStatusText = when {
                                !state.isTimerEnabled -> "已关闭 · 敲击不限时"
                                state.isAutoKnockEnabled -> {
                                    val m = state.timerRemainingSeconds / 60
                                    val s = state.timerRemainingSeconds % 60
                                    "倒计中 · 剩余 ${String.format("%02d:%02d", m, s)} (到期停止)"
                                }
                                else -> "设为 ${state.timerDurationMinutes} 分钟 · 启动后自动倒计"
                            }
                            Text(
                                text = timerStatusText,
                                fontSize = 12.sp,
                                color = if (state.isTimerEnabled && state.isAutoKnockEnabled) Color(0xFFFFD54F) else if (state.isTimerEnabled) Color.LightGray else Color.Gray
                            )
                        }
                        Switch(
                            checked = state.isTimerEnabled,
                            onCheckedChange = onToggleTimer,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFFD54F),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF333333)
                            )
                        )
                    }

                    // 倒计时预设选择 (开启时展示)
                    AnimatedVisibility(visible = state.isTimerEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "常用专注时长：", fontSize = 12.sp, color = Color.Gray)
                            val timerPresets = listOf(5, 10, 15, 20, 30, 45, 60)
                            val row1 = timerPresets.take(4) // 5, 10, 15, 20
                            val row2 = timerPresets.drop(4) // 30, 45, 60

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row1.forEach { minutes ->
                                    val isSelected = state.timerDurationMinutes == minutes
                                    Surface(
                                        onClick = { onSetTimerDuration(minutes) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF3A301D) else Color(0xFF282828),
                                        border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                            Text(
                                                text = "${minutes}分",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color(0xFFFFD54F) else Color(0xFFCCCCCC)
                                            )
                                        }
                                    }
                                }
                            }

                            var showCustomTimerDialog by remember { mutableStateOf(false) }
                            var customTimerText by remember { mutableStateOf(state.timerDurationMinutes.toString()) }
                            val isCustomDuration = state.timerDurationMinutes !in timerPresets

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row2.forEach { minutes ->
                                    val isSelected = state.timerDurationMinutes == minutes
                                    Surface(
                                        onClick = { onSetTimerDuration(minutes) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF3A301D) else Color(0xFF282828),
                                        border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                            Text(
                                                text = "${minutes}分",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color(0xFFFFD54F) else Color(0xFFCCCCCC)
                                            )
                                        }
                                    }
                                }

                                // 第 4 项：自定义时长输入项
                                Surface(
                                    onClick = {
                                        customTimerText = state.timerDurationMinutes.toString()
                                        showCustomTimerDialog = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCustomDuration) Color(0xFF3A301D) else Color(0xFF2D2A22),
                                    border = if (isCustomDuration) ButtonDefaults.outlinedButtonBorder else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                        Text(
                                            text = if (isCustomDuration) "${state.timerDurationMinutes}分 ✍️" else "自定义 ✍️",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFFD54F)
                                        )
                                    }
                                }
                            }

                            if (showCustomTimerDialog) {
                                AlertDialog(
                                    onDismissRequest = { showCustomTimerDialog = false },
                                    containerColor = Color(0xFF262626),
                                    title = {
                                        Text(text = "自定义专注时长 (分钟)", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(text = "请输入 1 到 180 之间的倒计时分钟数：", fontSize = 13.sp, color = Color.LightGray)
                                            OutlinedTextField(
                                                value = customTimerText,
                                                onValueChange = {
                                                    if (it.length <= 3 && it.all { ch -> ch.isDigit() }) {
                                                        customTimerText = it
                                                    }
                                                },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        val p = customTimerText.toIntOrNull()
                                                        if (p != null && p in 1..180) {
                                                            onSetTimerDuration(p)
                                                            showCustomTimerDialog = false
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
                                                val p = customTimerText.toIntOrNull()
                                                if (p != null && p in 1..180) {
                                                    onSetTimerDuration(p)
                                                    showCustomTimerDialog = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
                                        ) {
                                            Text(text = "确定", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showCustomTimerDialog = false }) {
                                            Text(text = "取消", color = Color.Gray)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. 环境音乐节奏智能分析测速 (NEW)
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

                            // 启动 / 停止测速按钮
                            Surface(
                                onClick = {
                                    if (detectorState.isListening) {
                                        beatDetector.stop()
                                    } else {
                                        if (hasAudioPermission) {
                                            beatDetector.start(coroutineScope)
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
                                    color = if (detectorState.isListening) Color(0xFFFF8A80) else Color(0xFFFFD54F),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // 状态与动态电平条
                        AnimatedVisibility(visible = detectorState.isListening) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = detectorState.statusText,
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )

                                // 实时音频跳动指示条 (5根动感脉冲柱)
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
                                                .background(Color(0xFFFFD54F))
                                        )
                                    }
                                }

                                // 测得的 BPM 结果卡片
                                if (detectorState.detectedBpm != null) {
                                    val detected = detectorState.detectedBpm!!
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF2E2A1E), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "$detected BPM",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFD54F)
                                            )
                                            Text(
                                                text = if (detectorState.confidence > 0.4f) "置信度: 优" else "置信度: 良好",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                onClick = { onBpmChange(detected) },
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFFD54F)
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

                        // 辅助工具：手动轻敲测速 (Tap Tempo)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (tapFeedbackBpm != null) "轻敲测得: ${tapFeedbackBpm} BPM" else "或跟随音乐节拍点击测速：",
                                fontSize = 12.sp,
                                color = if (tapFeedbackBpm != null) Color(0xFFFFD54F) else Color.Gray
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
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
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
