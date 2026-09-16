package com.woofish

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

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
            BackHandler(enabled = uiState.isZenMode) {
                viewModel.sharedViewModel.toggleZenMode()
            }

            WoodenFishScreen(
                viewModel = viewModel.sharedViewModel,
                onPickCustomBgm = {
                    audioPickerLauncher.launch(arrayOf("audio/*"))
                }
            )
        }
    }
}
