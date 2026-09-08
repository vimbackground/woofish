package com.woofish

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun AutoKnockDialog(
    state: WoodenFishUiState,
    onDismiss: () -> Unit,
    onToggleAutoKnock: (Boolean) -> Unit,
    onBpmChange: (Int) -> Unit
) {
    var showCustomInputDialog by remember { mutableStateOf(false) }
    var inputBpmText by remember { mutableStateOf(state.bpm.toString()) }

    val intervalSec = 60f / state.bpm.coerceAtLeast(1)

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    .padding(top = 8.dp),
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
                                "已暂停"
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

                // 2. BPM 节奏滑块调节 (30 - 240 BPM)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "节拍速度 (BPM)", fontSize = 14.sp, color = Color.White)
                        Surface(
                            onClick = {
                                inputBpmText = state.bpm.toString()
                                showCustomInputDialog = true
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF2A2A2A)
                        ) {
                            Text(
                                text = "${state.bpm} BPM ✍️",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Slider(
                        value = state.bpm.toFloat(),
                        onValueChange = { onBpmChange(it.roundToInt()) },
                        valueRange = 30f..240f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD54F),
                            activeTrackColor = Color(0xFFFFD54F),
                            inactiveTrackColor = Color(0xFF333333)
                        )
                    )

                    // 音乐常见节奏预设与自定义输入快捷栏（两排布局）
                    val (topRowPresets, bottomRowPresets) = remember(state.currentMode) {
                        val top = when (state.currentMode) {
                            AppMode.METRONOME -> listOf(
                                "慢板 56" to 56,    // Lento
                                "柔板 72" to 72,    // Adagio
                                "行板 88" to 88     // Andante
                            )
                            AppMode.WOODEN_FISH -> listOf(
                                "禅定 56" to 56,
                                "沉静 72" to 72,
                                "舒缓 88" to 88
                            )
                            AppMode.DRUM -> listOf(
                                "慢摇 56" to 56,
                                "柔和 72" to 72,
                                "律动 88" to 88
                            )
                        }
                        val bottom = when (state.currentMode) {
                            AppMode.METRONOME -> listOf(
                                "中板 108" to 108,  // Moderato
                                "快板 132" to 132   // Allegro
                            )
                            AppMode.WOODEN_FISH -> listOf(
                                "适中 108" to 108,
                                "清心 132" to 132
                            )
                            AppMode.DRUM -> listOf(
                                "中速 108" to 108,
                                "动感 132" to 132
                            )
                        }
                        top to bottom
                    }

                    Text(text = "常用节拍速度：", fontSize = 12.sp, color = Color.Gray)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 上排三项：Lento、Adagio、Andante
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            topRowPresets.forEach { (label, value) ->
                                val isSelected = state.bpm == value
                                Surface(
                                    onClick = { onBpmChange(value) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF3A301D) else Color(0xFF282828),
                                    border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFFFFD54F) else Color(0xFFCCCCCC)
                                        )
                                    }
                                }
                            }
                        }

                        // 下排三项：Moderato、Allegro、自定义
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            bottomRowPresets.forEach { (label, value) ->
                                val isSelected = state.bpm == value
                                Surface(
                                    onClick = { onBpmChange(value) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF3A301D) else Color(0xFF282828),
                                    border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFFFFD54F) else Color(0xFFCCCCCC)
                                        )
                                    }
                                }
                            }

                            // 第六项：自定义输入项
                            Surface(
                                onClick = {
                                    inputBpmText = state.bpm.toString()
                                    showCustomInputDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2D2A22),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        text = "自定义 ✍️",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFFD54F)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "完成", color = Color.White)
            }
        }
    )

    // 手动输入数字弹窗
    if (showCustomInputDialog) {
        AlertDialog(
            onDismissRequest = { showCustomInputDialog = false },
            containerColor = Color(0xFF262626),
            title = {
                Text(text = "自定义节拍速度 (BPM)", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "请输入 30 到 300 之间的每分钟拍数：", fontSize = 13.sp, color = Color.LightGray)
                    OutlinedTextField(
                        value = inputBpmText,
                        onValueChange = {
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                inputBpmText = it
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val parsed = inputBpmText.toIntOrNull()
                                if (parsed != null && parsed in 30..300) {
                                    onBpmChange(parsed)
                                    showCustomInputDialog = false
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
                        val parsed = inputBpmText.toIntOrNull()
                        if (parsed != null && parsed in 30..300) {
                            onBpmChange(parsed)
                            showCustomInputDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F))
                ) {
                    Text(text = "确定", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomInputDialog = false }) {
                    Text(text = "取消", color = Color.Gray)
                }
            }
        )
    }
}

