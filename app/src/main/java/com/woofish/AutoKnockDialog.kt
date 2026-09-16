package com.woofish

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
    onSubtitleChange: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var subtitleInput by remember { mutableStateOf(state.subtitle) }
    LaunchedEffect(state.subtitle) {
        subtitleInput = state.subtitle
    }

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
                    AppMode.POMODORO -> "自动节奏设置"
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

                // 2. 计时显示文案自定义 (从软件设置移至此处)
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
                            cursorBrush = SolidColor(Color(0xFFFFD54F)),
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
                                color = if (isSelected) Color(0xFF3A301D) else Color(0xFF262626),
                                border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFFFFD54F) else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
