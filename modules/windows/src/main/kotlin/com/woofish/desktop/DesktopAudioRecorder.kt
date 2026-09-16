package com.woofish.desktop

import com.woofish.IAudioStreamProvider
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.*

class DesktopAudioRecorder : IAudioStreamProvider {
    private var line: TargetDataLine? = null
    private var job: Job? = null

    override val isSupported: Boolean
        get() = try {
            val format = AudioFormat(22050f, 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)
            AudioSystem.isLineSupported(info)
        } catch (_: Exception) {
            false
        }

    override val hasPermission: Boolean get() = true

    override fun start(
        scope: CoroutineScope,
        sampleRate: Int,
        frameSize: Int,
        onFrame: (ShortArray, Int) -> Unit
    ) {
        stop()
        job = scope.launch(Dispatchers.IO) {
            try {
                val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
                val info = DataLine.Info(TargetDataLine::class.java, format)
                val targetLine = AudioSystem.getLine(info) as TargetDataLine
                targetLine.open(format, frameSize * 2 * 4)
                targetLine.start()
                line = targetLine

                val byteBuffer = ByteArray(frameSize * 2)
                val shortBuffer = ShortArray(frameSize)

                while (isActive && targetLine.isOpen) {
                    val bytesRead = targetLine.read(byteBuffer, 0, byteBuffer.size)
                    if (bytesRead <= 0) continue

                    val shortsRead = bytesRead / 2
                    val bb = ByteBuffer.wrap(byteBuffer, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until shortsRead) {
                        shortBuffer[i] = bb.short
                    }
                    onFrame(shortBuffer, shortsRead)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stop()
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        try {
            line?.stop()
            line?.close()
        } catch (_: Exception) {}
        line = null
    }
}
