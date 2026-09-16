package com.woofish

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

class AndroidMainViewModel(
    application: Application,
    val audioRecorder: AndroidAudioRecorder
) : AndroidViewModel(application) {
    val audioPlayer = AudioPlayer(application)
    val preferences = AndroidPreferences(application)
    val sharedViewModel = MainViewModel(
        audioPlayer = audioPlayer,
        prefs = preferences,
        audioStreamProvider = audioRecorder,
        coroutineScope = viewModelScope
    )

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        sharedViewModel.onCleared()
    }
}
