package com.d100tific.filemusic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.d100tific.filemusic.data.settings.WaveformStyle
import com.d100tific.filemusic.playback.WaveformTap
import com.d100tific.filemusic.ui.theme.BevelStyle
import com.d100tific.filemusic.ui.theme.LocalWinColors
import com.d100tific.filemusic.ui.theme.win2kBevel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private const val SCOPE_SAMPLES = 1536
private const val SCOPE_VISIBLE = 768
private const val TWO_PI = (2.0 * PI).toFloat()

/** Animation state kept outside Compose's snapshot system: it changes every frame. */
private class WaveState {
    var amplitude = 0f
    var brightness = 0f
    var phase = 0f
    var peak = 0.25f

    fun step(dt: Float, playing: Boolean, style: WaveformStyle) {
        val target = when {
            !playing -> 0f
            style == WaveformStyle.COSINE -> (WaveformTap.level * 3.4f).coerceIn(0f, 1f).pow(0.7f)
            else -> 1f
        }
        // Rise quickly with the beat, fall back a little slower.
        val rate = if (target > amplitude) 18f else 7f
        amplitude += (target - amplitude) * min(1f, dt * rate)
        brightness += (WaveformTap.brightness - brightness) * min(1f, dt * 5f)
        phase += dt * (0.55f + 1.1f * brightness)
        if (phase > 1000f) phase -= 1000f
    }

    val settled: Boolean get() = amplitude < 0.004f
}

/**
 * An oscilloscope screen. In [WaveformStyle.COSINE] mode it draws a smooth moving cosine whose
 * height follows the loudness of the music and whose pitch follows how bright the sound is; in
 * [WaveformStyle.OSCILLOSCOPE] mode it draws the actual audio signal being played. Everything
 * uses the app's colors: page background, accent trace.
 */
@Composable
fun WaveformView(
    isPlaying: Boolean,
    style: WaveformStyle,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWinColors.current
    val state = remember { WaveState() }
    val samples = remember { FloatArray(SCOPE_SAMPLES) }
    var tick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying, style) {
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000f).coerceIn(0f, 0.1f)
            last = now
            state.step(dt, isPlaying, style)
            tick = now
            // Once paused and faded out there is nothing left to animate.
            if (!isPlaying && state.settled) break
        }
    }

    Box(
        modifier = modifier
            .background(colors.page)
            .win2kBevel(colors, BevelStyle.SUNKEN)
            .clipToBounds(),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_VARIABLE")
            val frame = tick // reading it makes the canvas redraw every frame

            val w = size.width
            val h = size.height
            val midY = h / 2f
            val gridColor = colors.disabled.copy(alpha = 0.35f)

            // Graticule, like the screen of a real oscilloscope.
            for (i in 1 until 8) {
                val x = w * i / 8f
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            }
            for (i in 1 until 6) {
                val y = h * i / 6f
                val isCenter = i == 3
                drawLine(
                    color = if (isCenter) colors.disabled else gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (isCenter) 2f else 1f,
                )
            }

            val path = Path()
            val steps = max(24, (w / 4f).toInt())
            val usable = h * 0.42f

            if (style == WaveformStyle.COSINE) {
                val a = state.amplitude
                val cycles = 1.5f + 2.5f * state.brightness
                for (i in 0..steps) {
                    val x = i.toFloat() / steps
                    val main = cos(TWO_PI * (cycles * x - state.phase))
                    val overtone = cos(TWO_PI * (cycles * 2.07f * x + state.phase * 1.7f))
                    val y = midY - usable * a * (0.72f * main + 0.28f * overtone)
                    if (i == 0) path.moveTo(0f, y) else path.lineTo(x * w, y)
                }
            } else {
                WaveformTap.snapshot(samples)
                // Trigger on a rising zero crossing so the picture holds still.
                var start = 0
                val limit = SCOPE_SAMPLES - SCOPE_VISIBLE - 1
                for (i in 0 until limit) {
                    if (samples[i] <= 0f && samples[i + 1] > 0f) {
                        start = i
                        break
                    }
                }
                var windowPeak = 0f
                for (i in 0 until SCOPE_VISIBLE) windowPeak = max(windowPeak, abs(samples[start + i]))
                state.peak += (max(windowPeak, 0.05f) - state.peak) * 0.08f
                val gain = 0.95f / max(state.peak, 0.05f)
                for (i in 0..steps) {
                    val x = i.toFloat() / steps
                    val sample = samples[start + (x * (SCOPE_VISIBLE - 1)).toInt()]
                    val y = midY - usable * state.amplitude * (sample * gain).coerceIn(-1f, 1f)
                    if (i == 0) path.moveTo(0f, y) else path.lineTo(x * w, y)
                }
            }

            drawPath(
                path = path,
                color = colors.accent,
                style = Stroke(width = 3.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}