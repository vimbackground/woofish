package com.woofish

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import woofish.shared.generated.resources.Res
import woofish.shared.generated.resources.ic_music_note
import kotlin.math.roundToInt

@Composable
fun AudioSettingsDialog(
    state: WoodenFishUiState,
    streamProvider: IAudioStreamProvider? = null,
    isLandscape: Boolean = false,
    onDismiss: () -> Unit,
    onToggleBgm: () -> Unit,
    onPickBgm: () -> Unit,
    onClearBgm: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSoundIndexChange: (Int) -> Unit,
    onTogglePomodoroSound: () -> Unit = {},
    onBpmChange: (Int) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val coroutineScope = rememberCoroutineScope()

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
                        Text(strings.back, color = Color(0xFFB0B0B0), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = strings.audioSettingsTitle, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                }
                Button(
                    onClick = {
                        beatDetector.stop()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = strings.done, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 横屏左右双栏布局
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(36.dp)
            ) {
                // 左侧列：敲击音效选择 + 背景音乐播放控制与管理
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. 敲击/专注音效选择（置于背景音乐上方）
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF222222),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = strings.currentModeSoundTitle(state.currentMode.getLocalizedDisplayName(strings)),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (state.currentMode == AppMode.POMODORO) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = strings.pomodoroSoundSwitchTitle,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = strings.pomodoroSoundSwitchDesc,
                                            fontSize = 11.5.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Switch(
                                        checked = state.isPomodoroSoundEnabled,
                                        onCheckedChange = { onTogglePomodoroSound() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF444444),
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color(0xFF222222)
                                        )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.currentMode.getLocalizedSoundNames(strings).forEachIndexed { index, name ->
                                    val isSelected = state.soundIndex == index
                                    Surface(
                                        onClick = { onSoundIndexChange(index) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF2A2A2A),
                                        border = if (isSelected) BorderStroke(1.5.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = name,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Color(0xFFB0B0B0)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. 背景音乐播放控制与设置
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF222222),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 标题行
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_music_note),
                                    contentDescription = null,
                                    tint = if (state.isBgmPlaying) Color.White else Color(0xFF888888),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(text = strings.bgmTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            // 当前曲目说明
                            Text(
                                text = if (state.customBgmUri != null) {
                                    val status = if (state.isBgmPlaying) strings.bgmStatusPlaying else strings.bgmStatusPaused
                                    val prefix = if (strings == StringsZh) "当前曲目：" else "Track: "
                                    val defaultName = if (strings == StringsZh) "本地音频" else "Local Audio"
                                    "$prefix${state.customBgmTitle ?: defaultName} ($status)"
                                } else {
                                    if (strings == StringsZh) "未选择背景音乐（可选择手机内任意音频循环播放）" else "No background music selected (loop any audio file)"
                                },
                                fontSize = 12.sp,
                                color = if (state.customBgmUri != null) Color(0xFFB0B0B0) else Color(0xFF777777),
                                maxLines = 1
                            )

                            // 播放、更换、清除操作按钮（置于背景音乐说明下方）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        if (state.customBgmUri == null) {
                                            onPickBgm()
                                        } else {
                                            onToggleBgm()
                                        }
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (state.isBgmPlaying) Color.White else Color(0xFF333333),
                                        contentColor = if (state.isBgmPlaying) Color.Black else Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (state.customBgmUri == null) strings.selectMusic else if (state.isBgmPlaying) "⏸️ ${strings.pause}" else "▶️ ${strings.play}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (state.customBgmUri != null) {
                                    FilledTonalButton(
                                        onClick = onPickBgm,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFF333333),
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(text = if (strings == StringsZh) "更换" else "Change", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = onClearBgm,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(text = strings.clearMusicShort, fontSize = 12.sp)
                                    }
                                }
                            }

                            // 音量调节滑块与左右加减微调
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = strings.bgmVolume, fontSize = 13.sp, color = Color.White)
                                    Text(text = "${(state.bgmVolume * 100).toInt()}%", fontSize = 13.sp, color = Color.Gray)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledIconButton(
                                        onClick = {
                                            val newVol = ((state.bgmVolume - 0.05f).coerceAtLeast(0f) * 100).roundToInt() / 100f
                                            onVolumeChange(newVol)
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color(0xFF2E2E2E),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text(text = "−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Slider(
                                        value = state.bgmVolume,
                                        onValueChange = onVolumeChange,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color(0xFF333333)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    FilledIconButton(
                                        onClick = {
                                            val newVol = ((state.bgmVolume + 0.05f).coerceAtMost(1f) * 100).roundToInt() / 100f
                                            onVolumeChange(newVol)
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color(0xFF2E2E2E),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 右侧列：音效与节奏测速 (环境音乐测速 + 点击测速)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    TempoSpeedTestContent(
                        detectorState = detectorState,
                        autoSyncTempo = autoSyncTempo,
                        onAutoSyncChange = { autoSyncTempo = it },
                        tapFeedbackBpm = tapFeedbackBpm,
                        onListenToggle = {
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
                        onTapRecord = {
                            val bpm = tapTempoTracker.recordTap()
                            if (bpm != null) {
                                tapFeedbackBpm = bpm
                                onBpmChange(bpm)
                            }
                        },
                        onBpmChange = onBpmChange
                    )
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
                Text(text = strings.audioSettingsTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. 敲击/专注音效选择（置于背景音乐上方）
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF262626),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = strings.currentModeSoundTitle(state.currentMode.getLocalizedDisplayName(strings)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (state.currentMode == AppMode.POMODORO) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = strings.pomodoroSoundSwitchTitle,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = strings.pomodoroSoundSwitchDesc,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Switch(
                                        checked = state.isPomodoroSoundEnabled,
                                        onCheckedChange = { onTogglePomodoroSound() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF444444),
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color(0xFF222222)
                                        )
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.currentMode.getLocalizedSoundNames(strings).forEachIndexed { index, name ->
                                    val isSelected = state.soundIndex == index
                                    Surface(
                                        onClick = { onSoundIndexChange(index) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF303030),
                                        border = if (isSelected) BorderStroke(1.5.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = name,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Color(0xFFB0B0B0)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. 背景音乐播放控制与设置
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF262626),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 标题行
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_music_note),
                                    contentDescription = null,
                                    tint = if (state.isBgmPlaying) Color.White else Color(0xFF888888),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(text = strings.bgmTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            // 当前曲目说明
                            Text(
                                text = if (state.customBgmUri != null) {
                                    val status = if (state.isBgmPlaying) strings.bgmStatusPlaying else strings.bgmStatusPaused
                                    val prefix = if (strings == StringsZh) "当前：" else "Track: "
                                    "$prefix${state.customBgmTitle ?: strings.bgmLocalAudio} · $status"
                                } else {
                                    strings.bgmNotSet
                                },
                                fontSize = 12.sp,
                                color = if (state.customBgmUri != null) Color(0xFFB0B0B0) else Color(0xFF777777),
                                maxLines = 1
                            )

                            // 播放、更换、清除操作按钮（置于背景音乐说明下方）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        if (state.customBgmUri == null) {
                                            onPickBgm()
                                        } else {
                                            onToggleBgm()
                                        }
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (state.isBgmPlaying) Color.White else Color(0xFF383838),
                                        contentColor = if (state.isBgmPlaying) Color.Black else Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (state.customBgmUri == null) strings.selectMusicShort else if (state.isBgmPlaying) "⏸️ ${strings.pause}" else "▶️ ${strings.play}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (state.customBgmUri != null) {
                                    FilledTonalButton(
                                        onClick = onPickBgm,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFF383838),
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(text = strings.changeMusic, fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = onClearBgm,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(text = strings.clearMusicShort, fontSize = 11.sp)
                                    }
                                }
                            }

                            // 音量调节滑块与左右加减微调
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = strings.bgmVolume, fontSize = 13.sp, color = Color.White)
                                    Text(text = "${(state.bgmVolume * 100).toInt()}%", fontSize = 13.sp, color = Color.Gray)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledIconButton(
                                        onClick = {
                                            val newVol = ((state.bgmVolume - 0.05f).coerceAtLeast(0f) * 100).roundToInt() / 100f
                                            onVolumeChange(newVol)
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color(0xFF2E2E2E),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text(text = "−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Slider(
                                        value = state.bgmVolume,
                                        onValueChange = onVolumeChange,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color(0xFF333333)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    FilledIconButton(
                                        onClick = {
                                            val newVol = ((state.bgmVolume + 0.05f).coerceAtMost(1f) * 100).roundToInt() / 100f
                                            onVolumeChange(newVol)
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color(0xFF2E2E2E),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 3. 音效与节奏测速 (环境音乐测速 + 点击测速)
                    TempoSpeedTestContent(
                        detectorState = detectorState,
                        autoSyncTempo = autoSyncTempo,
                        onAutoSyncChange = { autoSyncTempo = it },
                        tapFeedbackBpm = tapFeedbackBpm,
                        onListenToggle = {
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
                        onTapRecord = {
                            val bpm = tapTempoTracker.recordTap()
                            if (bpm != null) {
                                tapFeedbackBpm = bpm
                                onBpmChange(bpm)
                            }
                        },
                        onBpmChange = onBpmChange
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        beatDetector.stop()
                        onDismiss()
                    }
                ) {
                    Text(text = strings.done, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun TempoSpeedTestContent(
    detectorState: BeatDetectionState,
    autoSyncTempo: Boolean,
    onAutoSyncChange: (Boolean) -> Unit,
    tapFeedbackBpm: Int?,
    onListenToggle: () -> Unit,
    onTapRecord: () -> Unit,
    onBpmChange: (Int) -> Unit
) {
    val strings = LocalAppStrings.current
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
                        text = strings.speedDetectionTitle,
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
                    onClick = onListenToggle,
                    shape = RoundedCornerShape(6.dp),
                    color = if (detectorState.isListening) Color(0xFF422020) else Color(0xFF333333)
                ) {
                    Text(
                        text = if (detectorState.isListening) strings.stopDetectionShort else strings.listenDetection,
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
                                    text = if (detectorState.confidence > 0.4f) strings.confidenceHigh else strings.confidenceGood,
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
                                    text = strings.applySpeed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // 自动同步开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = strings.autoSyncBpm, fontSize = 12.sp, color = Color.White)
                        Switch(
                            checked = autoSyncTempo,
                            onCheckedChange = onAutoSyncChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4CAF50),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF333333)
                            )
                        )
                    }
                }
            }

            // 手动轻敲测速 Tap Tempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (tapFeedbackBpm != null) strings.tapTempoResult(tapFeedbackBpm) else strings.tapTempoPrompt,
                    fontSize = 12.sp,
                    color = if (tapFeedbackBpm != null) Color.White else Color(0xFF888888)
                )

                Surface(
                    onClick = onTapRecord,
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF333333)
                ) {
                    Text(
                        text = strings.tapTempoButton,
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
