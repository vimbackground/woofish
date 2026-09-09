package com.woofish

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsDialog(
    state: WoodenFishUiState,
    onDismiss: () -> Unit,
    onModeChange: (AppMode) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVibrationChange: (Int) -> Unit,
    onFullScreenTapChange: (Boolean) -> Unit,
    onResetCount: () -> Unit,
    onPickBgm: () -> Unit,
    onClearBgm: () -> Unit
) {
    var subtitleInput by remember { mutableStateOf(state.subtitle) }

    LaunchedEffect(state.subtitle) {
        subtitleInput = state.subtitle
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFCCCCCC),
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(text = "软件设置", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. 运行模式切换 (木鱼 / 节拍器 / 电子鼓)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "运行模式", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppMode.entries.forEach { mode ->
                            val isSelected = state.currentMode == mode
                            Surface(
                                onClick = { onModeChange(mode) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF3A301D) else Color(0xFF282828),
                                border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = mode.displayName.replace("模式", ""),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFFFFD54F) else Color(0xFFCCCCCC)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. 计时/计数文案自定义 (默认正念，强化计时标签)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "计时显示文案", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    OutlinedTextField(
                        value = subtitleInput,
                        onValueChange = {
                            subtitleInput = it
                            onSubtitleChange(it)
                        },
                        singleLine = true,
                        placeholder = { Text("例如：正念、计时、功德、节拍", fontSize = 13.sp, color = Color.DarkGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD54F),
                            unfocusedBorderColor = Color(0xFF444444)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 常用快捷选项：突出强化“计时”，移除“加油”
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("正念", "计时", "功德", "节拍", "律动", "计数").forEach { tag ->
                            Surface(
                                onClick = {
                                    subtitleInput = tag
                                    onSubtitleChange(tag)
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (subtitleInput == tag) Color(0xFF3A301D) else Color(0xFF262626)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    fontWeight = if (tag == "计时") FontWeight.Bold else FontWeight.Normal,
                                    color = if (subtitleInput == tag) Color(0xFFFFD54F) else if (tag == "计时") Color.White else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 3. 敲击震动强度调节滑块 (0~80ms，默认40ms)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "敲击震动强度", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Text(
                            text = if (state.vibrationMs == 0) "已关闭" else "${state.vibrationMs} ms",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.vibrationMs == 0) Color.Gray else Color(0xFFFFD54F)
                        )
                    }
                    Slider(
                        value = state.vibrationMs.toFloat(),
                        onValueChange = { onVibrationChange(it.toInt()) },
                        valueRange = 0f..80f,
                        steps = 79,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD54F),
                            activeTrackColor = Color(0xFFFFD54F),
                            inactiveTrackColor = Color(0xFF333333)
                        )
                    )
                    Text(
                        text = "默认 40ms，支持 0~80ms 自定义；满振幅强劲输出，手机置于桌面轻点亦有清脆震感",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // 4. 本地背景音乐选择与管理
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "背景音乐", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onPickBgm,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF333333),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (state.customBgmUri == null) "选择音乐" else "更换音乐",
                                    fontSize = 12.sp
                                )
                            }
                            if (state.customBgmUri != null) {
                                OutlinedButton(
                                    onClick = onClearBgm,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "清除", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Text(
                        text = if (state.customBgmUri != null) {
                            "当前：${state.customBgmTitle ?: "已选择本地音频"}"
                        } else {
                            "未设置（可自选手机内任意音频循环播放）"
                        },
                        fontSize = 12.sp,
                        color = if (state.customBgmUri != null) Color(0xFFFFD54F) else Color.Gray,
                        maxLines = 1
                    )
                }

                // 5. 背景音乐音量调节滑块
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "背景音乐音量", fontSize = 14.sp)
                        Text(text = "${(state.bgmVolume * 100).toInt()}%", fontSize = 14.sp, color = Color.Gray)
                    }
                    Slider(
                        value = state.bgmVolume,
                        onValueChange = onVolumeChange,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0xFF333333)
                        )
                    )
                }

                // 6. 全屏敲击模式
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "全屏敲击模式", fontSize = 15.sp, color = Color.White)
                        Text(text = "点击屏幕任意区域均可触发击打", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = state.isFullScreenTapEnabled,
                        onCheckedChange = onFullScreenTapChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4CAF50),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF333333)
                        )
                    )
                }

                // 7. 统计清零
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "统计清零", fontSize = 15.sp, color = Color.White)
                        Text(text = "重置已敲击的计数总数", fontSize = 12.sp, color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            onResetCount()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A1D1D)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "清零", color = Color(0xFFFF5252), fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "确定", color = Color.White)
            }
        }
    )
}
