package com.woofish

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun WoodenFishScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current

    // 本地音频选择器 (SAF)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            var displayName = "本地音乐"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.setCustomBgm(uri.toString(), displayName)
        }
    }

    // 自动敲击时驱动木鱼下压回弹动画
    var isAutoBouncing by remember { mutableStateOf(false) }
    LaunchedEffect(state.knockTrigger) {
        if (state.knockTrigger > 0L) {
            isAutoBouncing = true
            delay(100)
            isAutoBouncing = false
        }
    }

    val isKnocked = isPressed || isAutoBouncing
    val shouldAnimate = state.isAnimationEnabled && isKnocked

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
            // 当启用全屏敲击模式时，点击屏幕任意空白区域均可敲击木鱼
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = state.isFullScreenTapEnabled
            ) {
                viewModel.onKnock()
            }
    ) {
        // -------------------------------------------------------------
        // 1. 顶栏全部保留：
        // 左侧【BGM + 动效 + 音效】，右侧【自动敲击设置 + 清屏 + 软件设置】
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
                // 1. 背景音乐开关（未选择音频时点击触发选择本地音乐，已选择时点击切换播放/暂停）
                IconButton(
                    onClick = {
                        if (state.customBgmUri == null) {
                            audioPickerLauncher.launch(arrayOf("audio/*"))
                        } else {
                            viewModel.toggleBgm()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_music_note),
                        contentDescription = if (state.customBgmUri == null) "选择本地背景音乐" else "背景音乐开关",
                        tint = when {
                            state.isBgmPlaying -> Color(0xFFFFD54F)
                            state.customBgmUri != null -> Color.White
                            else -> Color(0xFF555555)
                        }
                    )
                }

                // 2. 木鱼物理打击动效开关
                IconButton(
                    onClick = { viewModel.toggleAnimation() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (state.isAnimationEnabled) R.drawable.ic_auto_awesome else R.drawable.ic_auto_awesome_outline
                        ),
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

            // ▶ 右上角：【自动敲击设置】 + 【清屏开关】 + 【软件设置】
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 自动敲击木鱼独立调节按钮
                IconButton(
                    onClick = { viewModel.toggleAutoKnockDialog(true) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (state.isAutoKnockEnabled) R.drawable.ic_timer else R.drawable.ic_timer_outline
                        ),
                        contentDescription = "自动敲击设置",
                        tint = if (state.isAutoKnockEnabled) Color(0xFFFFD54F) else Color(0xFF999999)
                    )
                }

                // 2. 清屏开关按钮
                IconButton(
                    onClick = { viewModel.toggleZenMode() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (state.isZenMode) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                        ),
                        contentDescription = if (state.isZenMode) "退出清屏" else "进入清屏",
                        tint = if (state.isZenMode) Color(0xFFFFD54F) else Color(0xFF999999)
                    )
                }

                // 3. 软件设置按钮（固定位于最右侧）
                IconButton(
                    onClick = { viewModel.toggleSettingsDialog(true) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "软件设置",
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
        // 4. 自动敲击调节弹窗
        // -------------------------------------------------------------
        if (state.showAutoKnockDialog) {
            AutoKnockDialog(
                state = state,
                onDismiss = { viewModel.toggleAutoKnockDialog(false) },
                onToggleAutoKnock = { viewModel.toggleAutoKnock(it) },
                onIntervalChange = { viewModel.setAutoKnockInterval(it) }
            )
        }

        // -------------------------------------------------------------
        // 5. 软件设置弹窗
        // -------------------------------------------------------------
        if (state.showSettings) {
            SettingsDialog(
                state = state,
                onDismiss = { viewModel.toggleSettingsDialog(false) },
                onVolumeChange = { viewModel.updateBgmVolume(it) },
                onFullScreenTapChange = { viewModel.setFullScreenTap(it) },
                onResetCount = { viewModel.resetCount() },
                onPickBgm = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                onClearBgm = { viewModel.clearCustomBgm() }
            )
        }
    }
}
