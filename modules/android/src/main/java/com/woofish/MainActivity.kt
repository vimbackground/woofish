package com.woofish

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import android.content.res.Configuration
import android.os.Build
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private var permissionCallback: ((Boolean) -> Unit)? = null
    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionCallback?.invoke(isGranted)
        permissionCallback = null
    }

    private lateinit var audioRecorder: AndroidAudioRecorder

    private val viewModel: AndroidMainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AndroidMainViewModel(application, audioRecorder) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        audioRecorder = AndroidAudioRecorder(applicationContext) { callback ->
            permissionCallback = callback
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        val audioPickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                var displayName = "本地音乐"
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
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
                viewModel.sharedViewModel.setCustomBgm(uri.toString(), displayName)
            }
        }

        setContent {
            val uiState by viewModel.sharedViewModel.uiState.collectAsState()
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            var showExitConfirmDialog by remember { mutableStateOf(false) }

            LaunchedEffect(uiState.screenOrientation) {
                requestedOrientation = when (uiState.screenOrientation) {
                    ScreenOrientationSetting.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    ScreenOrientationSetting.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    ScreenOrientationSetting.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }

            // 需求1：横屏时要求实现全屏（自动隐藏系统状态栏与导航栏，支持轻扫短暂浮出）
            LaunchedEffect(isLandscape) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (isLandscape) {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            // 需求6：防误退出安排，优先关闭弹窗或退出清屏，主界面下弹窗供用户确认退出
            BackHandler(enabled = true) {
                when {
                    showExitConfirmDialog -> showExitConfirmDialog = false
                    uiState.showSettings -> viewModel.sharedViewModel.toggleSettingsDialog(false)
                    uiState.showAudioSettings -> viewModel.sharedViewModel.toggleAudioSettingsDialog(false)
                    uiState.showAutoKnockDialog -> viewModel.sharedViewModel.toggleAutoKnockDialog(false)
                    uiState.isZenMode -> viewModel.sharedViewModel.toggleZenMode()
                    else -> showExitConfirmDialog = true
                }
            }

            WoodenFishScreen(
                viewModel = viewModel.sharedViewModel,
                onPickCustomBgm = {
                    audioPickerLauncher.launch(arrayOf("audio/*"))
                },
                onExitRequest = {
                    showExitConfirmDialog = true
                }
            )

            if (showExitConfirmDialog) {
                val systemLocale = androidx.compose.ui.text.intl.Locale.current.language.lowercase()
                val isZh = when (uiState.appLanguage) {
                    AppLanguage.SYSTEM -> systemLocale.startsWith("zh")
                    AppLanguage.ZH -> true
                    AppLanguage.EN -> false
                }
                val strings = if (isZh) StringsZh else StringsEn

                AlertDialog(
                    onDismissRequest = { showExitConfirmDialog = false },
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    textContentColor = Color(0xFFCCCCCC),
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Text(text = strings.exitAppTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    },
                    text = {
                        Text(text = strings.exitAppConfirmMessage, fontSize = 14.sp)
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExitConfirmDialog = false
                                finish()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = strings.exitAppConfirm, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showExitConfirmDialog = false }
                        ) {
                            Text(text = strings.exitAppCancel, color = Color(0xFFB0B0B0))
                        }
                    }
                )
            }
        }
    }
}
