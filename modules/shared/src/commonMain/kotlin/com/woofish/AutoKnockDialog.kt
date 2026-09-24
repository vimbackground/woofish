package com.woofish

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun AutoKnockDialog(
    state: WoodenFishUiState,
    isLandscape: Boolean = false,
    onDismiss: () -> Unit,
    onToggleAutoKnock: (Boolean) -> Unit,
    onBpmChange: (Int) -> Unit,
    onToggleTimer: (Boolean) -> Unit = {},
    onTimerDurationChange: (Int) -> Unit = {},
    onTogglePomodoro: () -> Unit = {},
    onPomodoroPresetClick: (String, List<Int>) -> Unit = { _, _ -> },
    onPomodoroCustomSeqChange: (String) -> Unit = {},
    onParsePomodoroSeq: (String) -> List<Int> = { listOf(25) },
    onTogglePomodoroSound: () -> Unit = {},
    onSoundIndexChange: (Int) -> Unit = {},
    onTempoPresetClick: (String, Int) -> Unit = { _, bpm -> onBpmChange(bpm) },
    onTempoCustomClick: () -> Unit = {},
    onSetCustomBpm: (Int) -> Unit = onBpmChange
) {
    var pomodoroCustomInput by remember(state.pomodoroCustomSequence) {
        mutableStateOf(
            if (state.pomodoroCustomSequence.isNotBlank() && state.pomodoroCustomSequence !in listOf("15+5", "25+5", "45+15", "50+10")) {
                state.pomodoroCustomSequence
            } else {
                "5+2+1"
            }
        )
    }
    var showCustomBpmDialog by remember { mutableStateOf(false) }
    var customBpmInputText by remember { mutableStateOf(state.customBpm.toString()) }
    val parsedStages = remember(pomodoroCustomInput) {
        onParsePomodoroSeq(pomodoroCustomInput)
    }
    val totalMinutes = remember(parsedStages) {
        parsedStages.sum()
    }

    val strings = LocalAppStrings.current
    val intervalSec = 60f / state.bpm.coerceAtLeast(1)
    val titleText = strings.rhythmSettingTitle(state.currentMode.getLocalizedDisplayName(strings))

    val tempoPresets = remember(state.currentMode, strings) {
        state.currentMode.getLocalizedTempoPresets(strings)
    }

    val pomodoroPresets = remember(strings) {
        listOf(
            "2${strings.minuteUnit}" to listOf(2),
            "1+1${strings.minuteUnit}" to listOf(1, 1),
            "15+5${strings.minuteUnit}" to listOf(15, 5),
            "5${strings.minuteUnit}" to listOf(5),
            "4+1${strings.minuteUnit}" to listOf(4, 1),
            "25+5${strings.minuteUnit}" to listOf(25, 5),
            "8${strings.minuteUnit}" to listOf(8),
            "6+2${strings.minuteUnit}" to listOf(6, 2),
            "50+10${strings.minuteUnit}" to listOf(50, 10)
        )
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
                    TextButton(onClick = onDismiss) {
                        Text(strings.back, color = Color(0xFFB0B0B0), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = titleText, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = strings.done, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.currentMode == AppMode.POMODORO) {
                // 番茄钟横屏
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
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
                                Text(text = strings.enablePomodoroCountdown, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                val remM = state.pomodoroRemainingSeconds / 60
                                val remS = state.pomodoroRemainingSeconds % 60
                                val timeStr = String.format("%02d:%02d", remM, remS)
                                Text(
                                    text = if (state.isPomodoroRunning) {
                                        strings.pomodoroRunningStatus(state.currentPomodoroStageIndex + 1, state.pomodoroStages.size, timeStr)
                                    } else {
                                        strings.pomodoroPausedStatus(timeStr)
                                    },
                                    fontSize = 12.sp,
                                    color = if (state.isPomodoroRunning) Color.White else Color(0xFF888888)
                                )
                            }
                            Switch(
                                checked = state.isPomodoroRunning,
                                onCheckedChange = { onTogglePomodoro() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4CAF50),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF333333)
                                )
                            )
                        }

                        // 2. 常用时长预设 (3x3 矩阵)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = strings.presetFocusDuration, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            val chunks = pomodoroPresets.chunked(3)
                            chunks.forEach { rowPresets ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPresets.forEach { (label, stages) ->
                                        val isSelected = state.pomodoroActivePreset == label
                                        Surface(
                                            onClick = { onPomodoroPresetClick(label, stages) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF262626),
                                            border = if (isSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = label,
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
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 3. 自定义连续多阶段规划
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF242424),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(text = strings.customPomodoroDialogTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = strings.customPomodoroDialogDesc, fontSize = 12.sp, color = Color.Gray)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = pomodoroCustomInput,
                                            onValueChange = { pomodoroCustomInput = it },
                                            singleLine = true,
                                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                            cursorBrush = SolidColor(Color.White),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Button(
                                        onClick = { onPomodoroCustomSeqChange(pomodoroCustomInput) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                                    ) {
                                        Text(text = strings.startPlan, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                val previewStr = parsedStages.mapIndexed { idx, m -> strings.stageLabel(idx + 1, parsedStages.size, m) }.joinToString(" ➔ ")
                                Text(
                                    text = strings.planPreview(totalMinutes, previewStr),
                                    fontSize = 11.5.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            } else {
                // 常规模式横屏 (左右双栏：左侧自动节奏与预设，右侧倒计时与精确调节)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. 自动节奏开关
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = strings.autoContinuousRhythm, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (state.isAutoKnockEnabled) strings.runningInterval(String.format("%.2f", intervalSec)) else strings.paused,
                                    fontSize = 12.sp,
                                    color = if (state.isAutoKnockEnabled) Color.White else Color(0xFF888888)
                                )
                            }
                            Switch(
                                checked = state.isAutoKnockEnabled,
                                onCheckedChange = onToggleAutoKnock,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4CAF50),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF333333)
                                )
                            )
                        }

                        // 2. 自动节奏预设按钮 (5个常规预设 + 1个自定义按钮)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = strings.presetRhythm, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            
                            // 第一排 (前3个预设)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tempoPresets.take(3).forEach { (label, presetBpm) ->
                                    val isSelected = state.tempoActivePreset == label && state.bpm == presetBpm
                                    Surface(
                                        onClick = { onTempoPresetClick(label, presetBpm) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF262626),
                                        border = if (isSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // 第二排 (后2个预设 + 自定义)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tempoPresets.drop(3).take(2).forEach { (label, presetBpm) ->
                                    val isSelected = state.tempoActivePreset == label && state.bpm == presetBpm
                                    Surface(
                                        onClick = { onTempoPresetClick(label, presetBpm) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF262626),
                                        border = if (isSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                // 自定义按钮
                                val isCustomSelected = state.tempoActivePreset == "自定义" || state.tempoActivePreset == strings.custom
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCustomSelected) Color(0x33FFFFFF) else Color(0xFF262626),
                                    border = if (isCustomSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .pointerInput(isCustomSelected, state.customBpm) {
                                            detectTapGestures(
                                                onTap = {
                                                    if (!isCustomSelected) {
                                                        customBpmInputText = state.customBpm.toString()
                                                        showCustomBpmDialog = true
                                                    } else {
                                                        onTempoCustomClick()
                                                    }
                                                },
                                                onLongPress = {
                                                    customBpmInputText = state.customBpm.toString()
                                                    showCustomBpmDialog = true
                                                }
                                            )
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isCustomSelected) "${strings.custom} ${state.customBpm}" else strings.custom,
                                            fontSize = if (isCustomSelected) 11.5.sp else 12.sp,
                                            fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCustomSelected) Color.White else Color(0xFFB0B0B0),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // 3. 自动节奏频率滑块与左右加减微调
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = strings.tempoFrequency, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                Text(
                                    text = "${state.bpm} BPM (${strings.intervalSeconds(String.format("%.2f", intervalSec))})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledIconButton(
                                    onClick = {
                                        if (state.bpm > 30) {
                                            onBpmChange(state.bpm - 1)
                                        }
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
                                    value = state.bpm.toFloat(),
                                    onValueChange = { onBpmChange(it.roundToInt()) },
                                    valueRange = 30f..300f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color(0xFF333333)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                FilledIconButton(
                                    onClick = {
                                        if (state.bpm < 300) {
                                            onBpmChange(state.bpm + 1)
                                        }
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

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 4. 倒计时定时功能
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF242424),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(text = strings.autoKnockDuration, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        val remM = state.timerRemainingSeconds / 60
                                        val remS = state.timerRemainingSeconds % 60
                                        Text(
                                            text = if (state.isTimerEnabled) {
                                                strings.countdownActive(String.format("%02d:%02d", remM, remS))
                                            } else {
                                                strings.unlimitedContinuous
                                            },
                                            fontSize = 12.sp,
                                            color = if (state.isTimerEnabled) Color.White else Color.Gray
                                        )
                                    }
                                    Switch(
                                        checked = state.isTimerEnabled,
                                        onCheckedChange = onToggleTimer,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF4CAF50),
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color(0xFF333333)
                                        )
                                    )
                                }

                                AnimatedVisibility(visible = state.isTimerEnabled, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // 预设时长快捷按钮
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(5, 10, 15, 30, 60).forEach { mins ->
                                                val isSelected = state.timerDurationMinutes == mins
                                                Surface(
                                                    onClick = { onTimerDurationChange(mins) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF333333),
                                                    border = if (isSelected) BorderStroke(1.dp, Color.White) else null,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = "${mins}${strings.minuteUnit}",
                                                            fontSize = 12.sp,
                                                            color = if (isSelected) Color.White else Color(0xFFB0B0B0)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // 滑块精确调节与左右加减微调
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilledIconButton(
                                                onClick = {
                                                    if (state.timerDurationMinutes > 1) {
                                                        onTimerDurationChange(state.timerDurationMinutes - 1)
                                                    }
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
                                                value = state.timerDurationMinutes.toFloat(),
                                                onValueChange = { onTimerDurationChange(it.roundToInt()) },
                                                valueRange = 1f..120f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color.White,
                                                    activeTrackColor = Color.White,
                                                    inactiveTrackColor = Color(0xFF444444)
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )

                                            FilledIconButton(
                                                onClick = {
                                                    if (state.timerDurationMinutes < 120) {
                                                        onTimerDurationChange(state.timerDurationMinutes + 1)
                                                    }
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
                    }
                }
            }
        }
    } else {
        // 竖屏 AlertDialog
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCCCCCC),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(text = titleText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.currentMode == AppMode.POMODORO) {
                        // 1. 番茄钟开关
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = strings.enablePomodoroCountdown, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                val remM = state.pomodoroRemainingSeconds / 60
                                val remS = state.pomodoroRemainingSeconds % 60
                                val timeStr = String.format("%02d:%02d", remM, remS)
                                Text(
                                    text = if (state.isPomodoroRunning) {
                                        strings.pomodoroRunningStatus(state.currentPomodoroStageIndex + 1, state.pomodoroStages.size, timeStr)
                                    } else {
                                        strings.pomodoroPausedStatus(timeStr)
                                    },
                                    fontSize = 12.sp,
                                    color = if (state.isPomodoroRunning) Color.White else Color(0xFF888888)
                                )
                            }
                            Switch(
                                checked = state.isPomodoroRunning,
                                onCheckedChange = { onTogglePomodoro() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4CAF50),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF333333)
                                )
                            )
                        }

                        // 2. 预设时长
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = strings.presetFocusDuration, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            val chunks = pomodoroPresets.chunked(3)
                            chunks.forEach { rowPresets ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPresets.forEach { (label, stages) ->
                                        val isSelected = state.pomodoroActivePreset == label
                                        Surface(
                                            onClick = { onPomodoroPresetClick(label, stages) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF262626),
                                            border = if (isSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = label,
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

                        // 3. 自定义连续规划
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF262626),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = strings.customPomodoroDialogTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = strings.customPomodoroDialogDesc, fontSize = 11.sp, color = Color.Gray)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .border(1.dp, Color(0xFF444444), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = pomodoroCustomInput,
                                            onValueChange = { pomodoroCustomInput = it },
                                            singleLine = true,
                                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                            cursorBrush = SolidColor(Color.White),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Button(
                                        onClick = { onPomodoroCustomSeqChange(pomodoroCustomInput) },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = strings.setButton, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                val previewStr = parsedStages.mapIndexed { idx, m -> strings.stageLabel(idx + 1, parsedStages.size, m) }.joinToString(" ➔ ")
                                Text(
                                    text = strings.planPreview(totalMinutes, previewStr),
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    } else {
                        // 1. 自动连续节奏开关
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = strings.autoContinuousRhythm, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (state.isAutoKnockEnabled) strings.runningInterval(String.format("%.2f", intervalSec)) else strings.paused,
                                    fontSize = 12.sp,
                                    color = if (state.isAutoKnockEnabled) Color.White else Color(0xFF888888)
                                )
                            }
                            Switch(
                                checked = state.isAutoKnockEnabled,
                                onCheckedChange = onToggleAutoKnock,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4CAF50),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF333333)
                                )
                            )
                        }

                        // 2. 自动节奏预设按钮 (5个常规预设 + 1个自定义按钮)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = strings.presetRhythm, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                            
                            // 第一排 (前3个预设)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tempoPresets.take(3).forEach { (label, presetBpm) ->
                                    val isSelected = state.tempoActivePreset == label && state.bpm == presetBpm
                                    Surface(
                                        onClick = { onTempoPresetClick(label, presetBpm) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF262626),
                                        border = if (isSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // 第二排 (后2个预设 + 自定义)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tempoPresets.drop(3).take(2).forEach { (label, presetBpm) ->
                                    val isSelected = state.tempoActivePreset == label && state.bpm == presetBpm
                                    Surface(
                                        onClick = { onTempoPresetClick(label, presetBpm) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF262626),
                                        border = if (isSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Color(0xFFB0B0B0),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                // 自定义按钮
                                val isCustomSelected = state.tempoActivePreset == "自定义" || state.tempoActivePreset == strings.custom
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCustomSelected) Color(0x33FFFFFF) else Color(0xFF262626),
                                    border = if (isCustomSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, Color(0xFF3E3E3E)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .pointerInput(isCustomSelected, state.customBpm) {
                                            detectTapGestures(
                                                onTap = {
                                                    if (!isCustomSelected) {
                                                        customBpmInputText = state.customBpm.toString()
                                                        showCustomBpmDialog = true
                                                    } else {
                                                        onTempoCustomClick()
                                                    }
                                                },
                                                onLongPress = {
                                                    customBpmInputText = state.customBpm.toString()
                                                    showCustomBpmDialog = true
                                                }
                                            )
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isCustomSelected) "${strings.custom} ${state.customBpm}" else strings.custom,
                                            fontSize = if (isCustomSelected) 11.5.sp else 12.sp,
                                            fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCustomSelected) Color.White else Color(0xFFB0B0B0),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // 3. 自动节奏频率滑块与左右加减微调
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = strings.tempoFrequency, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                Text(
                                    text = "${state.bpm} BPM (${strings.intervalSeconds(String.format("%.2f", intervalSec))})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledIconButton(
                                    onClick = {
                                        if (state.bpm > 30) {
                                            onBpmChange(state.bpm - 1)
                                        }
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
                                    value = state.bpm.toFloat(),
                                    onValueChange = { onBpmChange(it.roundToInt()) },
                                    valueRange = 30f..300f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color(0xFF333333)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                FilledIconButton(
                                    onClick = {
                                        if (state.bpm < 300) {
                                            onBpmChange(state.bpm + 1)
                                        }
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

                        // 4. 定时倒计时
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF262626),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(text = strings.autoKnockDuration, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        val remM = state.timerRemainingSeconds / 60
                                        val remS = state.timerRemainingSeconds % 60
                                        Text(
                                            text = if (state.isTimerEnabled) {
                                                strings.countdownActive(String.format("%02d:%02d", remM, remS))
                                            } else {
                                                strings.unlimitedContinuous
                                            },
                                            fontSize = 11.sp,
                                            color = if (state.isTimerEnabled) Color.White else Color.Gray
                                        )
                                    }
                                    Switch(
                                        checked = state.isTimerEnabled,
                                        onCheckedChange = onToggleTimer,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF4CAF50),
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color(0xFF333333)
                                        )
                                    )
                                }

                                AnimatedVisibility(visible = state.isTimerEnabled, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(5, 10, 15, 30, 60).forEach { mins ->
                                                val isSelected = state.timerDurationMinutes == mins
                                                Surface(
                                                    onClick = { onTimerDurationChange(mins) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF333333),
                                                    border = if (isSelected) BorderStroke(1.dp, Color.White) else null,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            text = "${mins}${strings.minuteUnit}",
                                                            fontSize = 11.sp,
                                                            color = if (isSelected) Color.White else Color(0xFFB0B0B0)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // 滑块精确调节与左右加减微调
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilledIconButton(
                                                onClick = {
                                                    if (state.timerDurationMinutes > 1) {
                                                        onTimerDurationChange(state.timerDurationMinutes - 1)
                                                    }
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
                                                value = state.timerDurationMinutes.toFloat(),
                                                onValueChange = { onTimerDurationChange(it.roundToInt()) },
                                                valueRange = 1f..120f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color.White,
                                                    activeTrackColor = Color.White,
                                                    inactiveTrackColor = Color(0xFF444444)
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )

                                            FilledIconButton(
                                                onClick = {
                                                    if (state.timerDurationMinutes < 120) {
                                                        onTimerDurationChange(state.timerDurationMinutes + 1)
                                                    }
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
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = strings.done, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 快捷自定义 BPM 弹窗
    if (showCustomBpmDialog) {
        var tempBpm by remember(showCustomBpmDialog) {
            mutableIntStateOf(state.bpm)
        }

        AlertDialog(
            onDismissRequest = { showCustomBpmDialog = false },
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
                        val approxText = if (strings == StringsZh) "约 ${String.format("%.2f", interval)} 秒/拍 · 调节范围 30~300" else "approx ${String.format("%.2f", interval)} s/beat · Range 30~300"
                        Text(
                            text = approxText,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledIconButton(
                            onClick = {
                                if (tempBpm > 30) {
                                    tempBpm -= 1
                                    customBpmInputText = tempBpm.toString()
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

                        Slider(
                            value = tempBpm.toFloat(),
                            onValueChange = {
                                val rounded = it.roundToInt().coerceIn(30, 300)
                                tempBpm = rounded
                                customBpmInputText = rounded.toString()
                            },
                            valueRange = 30f..300f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color(0xFF383838)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FilledIconButton(
                            onClick = {
                                if (tempBpm < 300) {
                                    tempBpm += 1
                                    customBpmInputText = tempBpm.toString()
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

                    OutlinedTextField(
                        value = customBpmInputText,
                        onValueChange = { text ->
                            if (text.length <= 4 && text.all { it.isDigit() }) {
                                customBpmInputText = text
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
                                val parsed = customBpmInputText.toIntOrNull()?.coerceIn(30, 300) ?: tempBpm
                                onSetCustomBpm(parsed)
                                showCustomBpmDialog = false
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
                        val parsed = customBpmInputText.toIntOrNull()?.coerceIn(30, 300) ?: tempBpm
                        onSetCustomBpm(parsed)
                        showCustomBpmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text(text = strings.start, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomBpmDialog = false }) {
                    Text(text = strings.cancel, color = Color.Gray)
                }
            }
        )
    }
}
