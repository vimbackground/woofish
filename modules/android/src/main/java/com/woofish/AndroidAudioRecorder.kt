package com.woofish

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AndroidAudioRecorder(
    private val context: Context,
    private val onPermissionRequest: ((onGranted: (Boolean) -> Unit) -> Unit)? = null
) : IAudioStreamProvider {

    private var audioRecord: AudioRecord? = null
    private var job: Job? = null

    override val isSupported: Boolean get() = true

    override val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        if (hasPermission) {
            onResult(true)
        } else {
            onPermissionRequest?.invoke(onResult) ?: onResult(false)
        }
    }

    @SuppressLint("MissingPermission")
    override fun start(
        scope: CoroutineScope,
        sampleRate: Int,
        frameSize: Int,
        onFrame: (ShortArray, Int) -> Unit
    ) {
        stop()
        if (!hasPermission) return

        job = scope.launch(Dispatchers.IO) {
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSizeInBytes = maxOf(minBufSize, frameSize * 2 * 2)

            try {
                val record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSizeInBytes
                )
                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    record.release()
                    return@launch
                }
                record.startRecording()
                audioRecord = record

                val buffer = ShortArray(frameSize)
                while (isActive && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readCount = record.read(buffer, 0, frameSize)
                    if (readCount > 0) {
                        onFrame(buffer, readCount)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
                audioRecord = null
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }
}
