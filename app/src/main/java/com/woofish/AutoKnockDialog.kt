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
import kotlin.math.roundToLong

@Composable
fun AutoKnockDialog(
    state: WoodenFishUiState,
    onDismiss: () -> Unit,
    onToggleAutoKnock: (Boolean) -> Unit,
    onIntervalChange: (Long) -> Unit
) {
    val intervalSec = state.autoKnockIntervalMs / 1000f
    val knocksPerMin = if (intervalSec > 0) (60 / intervalSec).toInt() else 60

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFCCCCCC),
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(text = "自动敲击木鱼", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. 自动敲击总开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "开启自动敲击", fontSize = 15.sp, color = Color.White)
                        Text(
                            text = if (state.isAutoKnockEnabled) "运行中 · $knocksPerMin 次/分钟" else "已暂停",
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

                // 2. 敲击时间频率调节 (0.2s - 3.0s)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "敲击频率", fontSize = 14.sp)
                        Text(
                            text = String.format("%.1f 秒/次 (%d 次/分)", intervalSec, knocksPerMin),
                            fontSize = 14.sp,
                            color = Color(0xFFFFD54F)
                        )
                    }
                    Slider(
                        value = state.autoKnockIntervalMs.toFloat(),
                        onValueChange = { onIntervalChange(it.roundToLong()) },
                        valueRange = 200f..3000f,
                        steps = 27, // 每 100ms 一档
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD54F),
                            activeTrackColor = Color(0xFFFFD54F),
                            inactiveTrackColor = Color(0xFF333333)
                        )
                    )

                    // 常用预设挡位快捷选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            "0.5s (极速)" to 500L,
                            "1.0s (适中)" to 1000L,
                            "2.0s (舒缓)" to 2000L,
                            "3.0s (禅定)" to 3000L
                        ).forEach { (label, value) ->
                            Surface(
                                onClick = { onIntervalChange(value) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (state.autoKnockIntervalMs == value) Color(0xFF3A301D) else Color(0xFF282828),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = label.substring(0, 4),
                                    fontSize = 11.sp,
                                    color = if (state.autoKnockIntervalMs == value) Color(0xFFFFD54F) else Color(0xFF999999),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
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
}
