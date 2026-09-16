package com.woofish

import kotlinx.coroutines.CoroutineScope

interface IAudioStreamProvider {
    val isSupported: Boolean get() = true
    val hasPermission: Boolean get() = true
    fun requestPermission(onResult: (Boolean) -> Unit) { onResult(true) }
    fun start(scope: CoroutineScope, sampleRate: Int, frameSize: Int, onFrame: (ShortArray, Int) -> Unit)
    fun stop()
}
