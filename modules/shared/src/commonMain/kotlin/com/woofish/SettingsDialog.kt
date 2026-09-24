package com.woofish

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import org.jetbrains.compose.resources.painterResource
import woofish.shared.generated.resources.Res
import woofish.shared.generated.resources.wepay
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsContent(
    state: WoodenFishUiState,
    isLandscape: Boolean = false,
    onDismiss: () -> Unit,
    onSubtitleChange: (String) -> Unit,
    onVibrationChange: (Int) -> Unit,
    onOrientationChange: (ScreenOrientationSetting) -> Unit = {},
    onFullScreenTapChange: (Boolean) -> Unit,
    onResetCount: () -> Unit
) {
    var subtitleInput by remember { mutableStateOf(state.subtitle) }
    LaunchedEffect(state.subtitle) {
        subtitleInput = state.subtitle
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
                        Text("← 返回", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "软件设置", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "完成", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                // 左侧列：文案设置 + 敲击震动
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. 显示文案自定义 (纯输入框，去除标签选择)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "显示文案", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                                .background(Color(0xFF242424), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (subtitleInput.isEmpty()) {
                                Text("例如：正念、功德、专注、节拍", fontSize = 13.sp, color = Color(0xFF666666))
                            }
                            BasicTextField(
                                value = subtitleInput,
                                onValueChange = {
                                    subtitleInput = it
                                    onSubtitleChange(it)
                                },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 2. 屏幕方向设置 (自动旋转 / 锁定竖屏 / 锁定横屏)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "屏幕方向", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScreenOrientationSetting.entries.forEach { setting ->
                                val isSelected = state.screenOrientation == setting
                                Surface(
                                    onClick = { onOrientationChange(setting) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF242424),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) Color.White else Color(0xFF444444)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = setting.displayName,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFFB0B0B0)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. 敲击震动强度 (范围 0~120ms，带两侧加减微调按钮)
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
                                color = if (state.vibrationMs == 0) Color.Gray else Color.White
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledIconButton(
                                onClick = {
                                    if (state.vibrationMs > 0) {
                                        onVibrationChange((state.vibrationMs - 1).coerceAtLeast(0))
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
                                value = state.vibrationMs.toFloat(),
                                onValueChange = { onVibrationChange(it.toInt()) },
                                valueRange = 0f..120f,
                                steps = 119,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color(0xFF333333)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            FilledIconButton(
                                onClick = {
                                    if (state.vibrationMs < 120) {
                                        onVibrationChange((state.vibrationMs + 1).coerceAtMost(120))
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

                // 右侧列：全屏敲击 + 统计清零 + 随喜赞助
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 4. 全屏敲击模式
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

                    // 5. 统计清零
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
                            onClick = onResetCount,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A1D1D)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "清零", color = Color(0xFFFF5252), fontSize = 13.sp)
                        }
                    }

                    // 6. 随喜赞助
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF202020), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            modifier = Modifier.size(90.dp)
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.wepay),
                                contentDescription = "微信赞助收款码",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "💖 随喜赞助", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "若正念对您有所助益，欢迎自愿赞助支持持续维护更新", fontSize = 11.5.sp, color = Color.Gray, lineHeight = 16.sp)
                            Text(text = "微信扫一扫 · 感恩有您 🙏", fontSize = 11.sp, color = Color.LightGray)
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
                    // 1. 显示文案自定义 (纯输入框，去除标签选择)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "显示文案", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                                .background(Color(0xFF242424), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (subtitleInput.isEmpty()) {
                                Text("例如：正念、功德、专注、节拍", fontSize = 13.sp, color = Color(0xFF666666))
                            }
                            BasicTextField(
                                value = subtitleInput,
                                onValueChange = {
                                    subtitleInput = it
                                    onSubtitleChange(it)
                                },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 2. 屏幕方向设置 (自动旋转 / 锁定竖屏 / 锁定横屏)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "屏幕方向", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScreenOrientationSetting.entries.forEach { setting ->
                                val isSelected = state.screenOrientation == setting
                                Surface(
                                    onClick = { onOrientationChange(setting) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF242424),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) Color.White else Color(0xFF444444)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = setting.displayName,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFFB0B0B0)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. 敲击震动强度 (范围 0~120ms，带两侧加减微调按钮)
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
                                color = if (state.vibrationMs == 0) Color.Gray else Color.White
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledIconButton(
                                onClick = {
                                    if (state.vibrationMs > 0) {
                                        onVibrationChange((state.vibrationMs - 1).coerceAtLeast(0))
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
                                value = state.vibrationMs.toFloat(),
                                onValueChange = { onVibrationChange(it.toInt()) },
                                valueRange = 0f..120f,
                                steps = 119,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color(0xFF333333)
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            FilledIconButton(
                                onClick = {
                                    if (state.vibrationMs < 120) {
                                        onVibrationChange((state.vibrationMs + 1).coerceAtMost(120))
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

                    // 4. 全屏敲击模式
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

                    // 5. 统计清零
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
                            onClick = onResetCount,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A1D1D)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "清零", color = Color(0xFFFF5252), fontSize = 13.sp)
                        }
                    }

                    // 6. 随喜赞助
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF262626), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "💖 随喜赞助", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "若正念对您有所助益，欢迎自愿赞助支持持续维护更新",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.wepay),
                                contentDescription = "微信赞助收款码",
                                modifier = Modifier
                                    .size(160.dp)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }

                        Text(
                            text = "微信扫一扫 · 随喜随缘 · 感恩有您 🙏",
                            fontSize = 11.5.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
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
}

@Composable
fun SettingsDialog(
    state: WoodenFishUiState,
    isLandscape: Boolean = false,
    onDismiss: () -> Unit,
    onSubtitleChange: (String) -> Unit,
    onVibrationChange: (Int) -> Unit,
    onOrientationChange: (ScreenOrientationSetting) -> Unit = {},
    onFullScreenTapChange: (Boolean) -> Unit,
    onResetCount: () -> Unit
) {
    SettingsContent(
        state = state,
        isLandscape = isLandscape,
        onDismiss = onDismiss,
        onSubtitleChange = onSubtitleChange,
        onVibrationChange = onVibrationChange,
        onOrientationChange = onOrientationChange,
        onFullScreenTapChange = onFullScreenTapChange,
        onResetCount = onResetCount
    )
}
