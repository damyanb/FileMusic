package com.d100tific.filemusic.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class BevelStyle { RAISED, SUNKEN, FLAT }

/**
 * Draws a classic Windows-2000 two-tone 3D bevel border (as used by buttons, panels and
 * text fields in the old "Windows Classic" widget set): a highlight line on the top/left
 * edge and a shadow line on the bottom/right edge, inverted for a sunken (pressed / inset)
 * look. All corners stay perfectly square, matching the requested retro aesthetic.
 */
fun Modifier.win2kBevel(colors: WinColors, style: BevelStyle = BevelStyle.RAISED, thickness: Int = 2): Modifier =
    this.drawWithContent {
        drawContent()
        val t = thickness.dp.toPx()
        if (style == BevelStyle.FLAT) {
            drawRect(color = colors.onPage, size = size, style = Stroke(width = t))
            return@drawWithContent
        }
        val highlight = if (style == BevelStyle.SUNKEN) colors.controlShadow else colors.controlHighlight
        val shadow = if (style == BevelStyle.SUNKEN) colors.controlHighlight else colors.controlShadow
        drawLine(highlight, Offset(0f, t / 2f), Offset(size.width, t / 2f), strokeWidth = t)
        drawLine(highlight, Offset(t / 2f, 0f), Offset(t / 2f, size.height), strokeWidth = t)
        drawLine(shadow, Offset(0f, size.height - t / 2f), Offset(size.width, size.height - t / 2f), strokeWidth = t)
        drawLine(shadow, Offset(size.width - t / 2f, 0f), Offset(size.width - t / 2f, size.height), strokeWidth = t)
    }

/** Plain 1px solid square-corner outline, used for grid thumbnails and dividers. */
fun Modifier.win2kOutline(color: Color, thickness: Int = 1): Modifier =
    this.drawWithContent {
        drawContent()
        drawRect(color = color, size = size, style = Stroke(width = thickness.dp.toPx()))
    }
