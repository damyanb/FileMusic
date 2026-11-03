package com.d100tific.filemusic.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Process-wide window onto the audio that is being played: the most recent mono samples plus
 * a couple of cheap measurements. The player thread writes it (through
 * [WaveformAudioProcessor]) and the UI reads it a few times per frame to animate the
 * oscilloscope. Reads are deliberately lock-free; an occasional torn sample is invisible.
 */
@OptIn(UnstableApi::class)
object WaveformTap {
    private const val SIZE = 4096
    private const val MASK = SIZE - 1

    private val ring = FloatArray(SIZE)
    @Volatile private var writeIndex = 0
    private var previousSample = 0f

    /** Root-mean-square loudness of the latest audio chunk, 0..1. */
    @Volatile var level: Float = 0f
        private set

    /** How "bright" (high pitched / noisy) the latest chunk was, 0..1. Derived from zero crossings. */
    @Volatile var brightness: Float = 0f
        private set

    fun clear() {
        level = 0f
        brightness = 0f
    }

    /** Copies the latest `dest.size` samples (oldest first) into [dest]. */
    fun snapshot(dest: FloatArray) {
        val count = min(dest.size, SIZE)
        val start = (writeIndex - count) and MASK
        for (i in 0 until count) dest[i] = ring[(start + i) and MASK]
    }

    internal fun consume(buffer: ByteBuffer, encoding: Int, channelCount: Int) {
        val bytesPerSample = when (encoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_24BIT -> 3
            else -> 4 // 32-bit integer or float
        }
        val frameBytes = bytesPerSample * channelCount
        val frames = buffer.remaining() / frameBytes
        if (frames <= 0) return

        var position = buffer.position()
        var index = writeIndex
        var sumSquares = 0.0
        var crossings = 0
        var previous = previousSample

        for (frame in 0 until frames) {
            var sum = 0f
            for (channel in 0 until channelCount) {
                sum += readSample(buffer, position + channel * bytesPerSample, encoding)
            }
            position += frameBytes
            val mono = sum / channelCount
            ring[index] = mono
            index = (index + 1) and MASK
            sumSquares += (mono * mono).toDouble()
            if ((mono >= 0f) != (previous >= 0f)) crossings++
            previous = mono
        }

        previousSample = previous
        writeIndex = index
        level = min(1f, sqrt(sumSquares / frames).toFloat())
        brightness = min(1f, crossings.toFloat() / frames * 10f)
    }

    private fun readSample(buffer: ByteBuffer, at: Int, encoding: Int): Float = when (encoding) {
        C.ENCODING_PCM_16BIT -> buffer.getShort(at) / 32768f
        C.ENCODING_PCM_24BIT -> {
            val b0 = buffer.get(at).toInt() and 0xFF
            val b1 = buffer.get(at + 1).toInt() and 0xFF
            val b2 = buffer.get(at + 2).toInt()
            ((b2 shl 16) or (b1 shl 8) or b0) / 8388608f
        }
        C.ENCODING_PCM_32BIT -> buffer.getInt(at) / 2147483648f
        else -> buffer.getFloat(at)
    }
}

/**
 * A pass-through [AudioProcessor]: audio goes out exactly as it came in, but on the way
 * it is copied into [WaveformTap] so the UI can draw it.
 */
@UnstableApi
class WaveformAudioProcessor : BaseAudioProcessor() {
    private var encoding = C.ENCODING_PCM_16BIT
    private var channelCount = 2

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val supported = when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_24BIT, C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> true
            else -> false
        }
        // Unsupported formats simply disable the processor; the music keeps playing untouched.
        if (!supported || inputAudioFormat.channelCount <= 0) return AudioProcessor.AudioFormat.NOT_SET
        encoding = inputAudioFormat.encoding
        channelCount = inputAudioFormat.channelCount
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        WaveformTap.consume(inputBuffer.asReadOnlyBuffer().order(ByteOrder.nativeOrder()), encoding, channelCount)
        val output = replaceOutputBuffer(size)
        output.put(inputBuffer)
        output.flip()
    }

    override fun onFlush() {
        WaveformTap.clear()
    }

    override fun onReset() {
        WaveformTap.clear()
    }
}
