package woofish

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WoodenFishScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 物理击打动效受 isAnimationEnabled 开关控制
    val shouldAnimate = state.isAnimationEnabled && isPressed

    val scaleX by animateFloatAsState(
        targetValue = if (shouldAnimate) 1.06f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scaleX"
    )
    val scaleY by animateFloatAsState(
        targetValue = if (shouldAnimate) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scaleY"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (shouldAnimate) 12f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offsetY"
    )
    val rotation by animateFloatAsState(
        targetValue = if (shouldAnimate) -2.5f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .systemBarsPadding()
    ) {
        // -------------------------------------------------------------
        // 模式 1：正常显示模式（包含完整顶栏与功德大计数）
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = !state.isZenMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶栏布局
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左上角：【音效切换】 + 【BGM开关】
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { viewModel.toggleSoundEffect() },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF222222),
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = if (state.soundIndex == 0) "🔊 音效 1" else "🔊 音效 2",
                                color = Color(0xFFCCCCCC),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleBgm() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "背景音乐",
                                tint = if (state.isBgmPlaying) Color.White else Color(0xFF555555)
                            )
                        }
                    }

                    // 右上角：【清屏】 + 【动效开关】 + 【调节】
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. 进入清屏模式
                        IconButton(
                            onClick = { viewModel.toggleZenMode() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = "进入清屏",
                                tint = Color(0xFF999999)
                            )
                        }

                        // 2. 木鱼物理打击动效开关
                        IconButton(
                            onClick = { viewModel.toggleAnimation() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isAnimationEnabled) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                contentDescription = "动效开关",
                                tint = if (state.isAnimationEnabled) Color(0xFFFFD54F) else Color(0xFF555555)
                            )
                        }

                        // 3. 调节设置
                        IconButton(
                            onClick = { viewModel.toggleSettingsDialog(true) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "调节设置",
                                tint = Color(0xFF999999)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(44.dp))

                // 功德大计数
                Text(
                    text = "${state.count}",
                    color = Color.White,
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "功德",
                    color = Color(0xFF555555),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // -------------------------------------------------------------
        // 模式 2：清屏状态（隐藏数字与文字，但仍保留 BGM 与木鱼动效极简控制）
        // -------------------------------------------------------------
        if (state.isZenMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 清屏状态下左上角：极简背景音乐开关
                IconButton(
                    onClick = { viewModel.toggleBgm() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "清屏BGM开关",
                        tint = if (state.isBgmPlaying) Color(0xAAFFFFFF) else Color(0x33FFFFFF)
                    )
                }

                // 清屏状态下右上角：极简木鱼动效开关 + 退出清屏按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 木鱼动效开关
                    IconButton(
                        onClick = { viewModel.toggleAnimation() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isAnimationEnabled) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "清屏动效开关",
                            tint = if (state.isAnimationEnabled) Color(0xCCFFD54F) else Color(0x33FFFFFF)
                        )
                    }

                    // 退出清屏按钮
                    IconButton(
                        onClick = { viewModel.toggleZenMode() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = "退出清屏",
                            tint = Color(0x44FFFFFF)
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 居中木鱼主体（敲击时根据动效开关产生物理受力弹性反馈）
        // -------------------------------------------------------------
        Image(
            painter = painterResource(id = R.drawable.ic_wooden_fish),
            contentDescription = "Wooden Fish",
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    this.translationY = offsetY
                    this.rotationZ = rotation
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    viewModel.onKnock()
                }
        )

        // 调节弹窗
        if (state.showSettings) {
            SettingsDialog(
                state = state,
                onDismiss = { viewModel.toggleSettingsDialog(false) },
                onVolumeChange = { viewModel.updateBgmVolume(it) },
                onResetCount = { viewModel.resetCount() }
            )
        }
    }
}
