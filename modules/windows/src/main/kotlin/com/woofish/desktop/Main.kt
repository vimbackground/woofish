package com.woofish.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.woofish.AppMode
import com.woofish.MainViewModel
import com.woofish.WoodenFishScreen
import org.jetbrains.compose.resources.painterResource
import woofish.shared.generated.resources.Res
import woofish.shared.generated.resources.ic_wooden_fish
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun main() = application {
    val audioPlayer = remember { DesktopAudioPlayer() }
    val preferences = remember { DesktopPreferences() }
    val audioRecorder = remember { DesktopAudioRecorder() }
    val viewModel = remember { MainViewModel(audioPlayer, preferences, audioRecorder) }

    val windowState = rememberWindowState(
        width = 960.dp,
        height = 540.dp,
        position = WindowPosition.Aligned(Alignment.Center)
    )

    fun openAudioFilePicker() {
        try {
            val dialog = FileDialog(null as Frame?, "选择本地背景音乐", FileDialog.LOAD)
            dialog.setFilenameFilter { _, name ->
                val lower = name.lowercase()
                lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac") || lower.endsWith(".ogg")
            }
            dialog.isVisible = true
            val dir = dialog.directory
            val file = dialog.file
            if (!dir.isNullOrBlank() && !file.isNullOrBlank()) {
                val chosen = File(dir, file)
                if (chosen.exists()) {
                    viewModel.setCustomBgm(chosen.absolutePath, chosen.nameWithoutExtension)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val windowTitle = if (uiState.currentMode == AppMode.POMODORO) {
        "正念木鱼 - ${uiState.subtitle}"
    } else {
        "正念木鱼 - ${uiState.subtitle} (${uiState.count})"
    }

    Window(
        onCloseRequest = {
            viewModel.onCleared()
            exitApplication()
        },
        title = windowTitle,
        state = windowState,
        icon = painterResource(Res.drawable.ic_wooden_fish),
        onKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                when (keyEvent.key) {
                    Key.Escape -> {
                        if (viewModel.uiState.value.isZenMode) {
                            viewModel.toggleZenMode()
                            true
                        } else false
                    }
                    Key.Spacebar -> {
                        val state = viewModel.uiState.value
                        if (state.currentMode == AppMode.POMODORO) {
                            viewModel.onPomodoroTap()
                        } else if (state.isAutoKnockEnabled) {
                            viewModel.toggleAutoKnock(false)
                        } else {
                            viewModel.onManualHit()
                        }
                        true
                    }
                    Key.Enter, Key.Backspace -> {
                        viewModel.resetCount()
                        true
                    }
                    Key.Z -> {
                        viewModel.toggleZenMode()
                        true
                    }
                    Key.M -> {
                        viewModel.toggleBgm()
                        true
                    }
                    Key.One -> {
                        viewModel.setBpm(30)
                        true
                    }
                    Key.Two -> {
                        viewModel.setBpm(60)
                        true
                    }
                    Key.Three -> {
                        viewModel.setBpm(90)
                        true
                    }
                    Key.Four -> {
                        viewModel.setBpm(120)
                        true
                    }
                    Key.Five -> {
                        viewModel.setBpm(150)
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    ) {
        WoodenFishScreen(
            viewModel = viewModel,
            onPickCustomBgm = { openAudioFilePicker() }
        )
    }
}
