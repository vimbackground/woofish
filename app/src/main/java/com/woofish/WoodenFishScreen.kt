package com.woofish

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
            // 当启用全屏点击有效模式时，点击屏幕任意空白区域均可敲击木鱼
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = state.isFullScreenTapEnabled
            ) {
                viewModel.onKnock()
            }
    ) {
        // -------------------------------------------------------------
        // 1. 顶栏全部保留：左侧【BGM + 动效 + 音效】，右侧仅保留【清屏 + 设置】
        // -------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ◀ 左上角：【BGM 开关】 + 【木鱼动效开关】 + 【音效选择】
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 背景音乐开关
                IconButton(
                    onClick = { viewModel.toggleBgm() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "背景音乐",
                        tint = if (state.isBgmPlaying) Color.White else Color(0xFF555555)
                    )
                }

                // 2. 木鱼物理打击动效开关
                IconButton(
                    onClick = { viewModel.toggleAnimation() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (state.isAnimationEnabled) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                        contentDescription = "动效开关",
                        tint = if (state.isAnimationEnabled) Color(0xFFFFD54F) else Color(0xFF555555)
                    )
                }

                // 3. 音效切换胶囊按钮（位于动效开关右侧）
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
            }

            // ▶ 右上角：仅保留【清屏开关】 + 【调节设置】两个按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 清屏开关按钮
                IconButton(
                    onClick = { viewModel.toggleZenMode() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (state.isZenMode) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = if (state.isZenMode) "退出清屏" else "进入清屏",
                        tint = if (state.isZenMode) Color(0xFFFFD54F) else Color(0xFF999999)
                    )
                }

                // 2. 调节设置按钮（位于最右侧）
                IconButton(
                    onClick = { viewModel.toggleSettingsDialog(true) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "调节设置",
                        tint = Color(0xFF999999)
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 2. 功德大计数与文字（清屏状态下仅清除此处的数字与文字）
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = !state.isZenMode,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp)
                .align(Alignment.TopCenter)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
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
        // 3. 居中木鱼主体（敲击时根据动效开关产生物理受力弹性反馈）
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

        // -------------------------------------------------------------
        // 4. 调节弹窗
        // -------------------------------------------------------------
        if (state.showSettings) {
            SettingsDialog(
                state = state,
                onDismiss = { viewModel.toggleSettingsDialog(false) },
                onVolumeChange = { viewModel.updateBgmVolume(it) },
                onFullScreenTapChange = { viewModel.setFullScreenTap(it) },
                onResetCount = { viewModel.resetCount() }
            )
        }
    }
}
