package com.woofish

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    onVolumeChange: (Float) -> Unit,
    onFullScreenTapChange: (Boolean) -> Unit,
    onResetCount: () -> Unit,
    onPickBgm: () -> Unit,
    onClearBgm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFCCCCCC),
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(text = "调节", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. 本地背景音乐选择与管理
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "背景音乐", fontSize = 15.sp, color = Color.White)
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
                            "未设置（点击右上角按钮打开手机音频）"
                        },
                        fontSize = 12.sp,
                        color = if (state.customBgmUri != null) Color(0xFFFFD54F) else Color.Gray,
                        maxLines = 1
                    )
                }

                // 2. 背景音乐音量调节滑块
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

                // 2. 全屏点击有效模式
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "全屏敲击模式", fontSize = 15.sp, color = Color.White)
                        Text(text = "点击屏幕任意区域均可敲击木鱼", fontSize = 12.sp, color = Color.Gray)
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

                // 3. 功德统计清零
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "统计清零", fontSize = 15.sp, color = Color.White)
                        Text(text = "重置已敲击的功德总数", fontSize = 12.sp, color = Color.Gray)
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
