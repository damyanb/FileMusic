package com.d100tific.filemusic.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Default accent color: a soft pastel peach-pink, matching the app icon. */
val DefaultAccent = Color(0xFFF3A9A0)

/** A curated palette of soft pastel tones users can pick as their accent color, in addition to a custom picker. */
val AccentPresets = listOf(
    Color(0xFFF3A9A0), // peach pink (default)
    Color(0xFFA9C9F0), // pastel blue
    Color(0xFFAEE0B8), // pastel green
    Color(0xFFF6D6A0), // pastel yellow
    Color(0xFFCBB2E0), // pastel lavender
    Color(0xFFA0DCD1), // pastel teal
)

/** Picks readable text/icon color (black or white) for a given background color. */
private fun contrastingOn(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF000000) else Color(0xFFFFFFFF)

/**
 * Bichromatic color set for the "Windows 2000" look: the page itself is strictly
 * black or white, text/icons are the opposite of the page color, and [accent] is the
 * single customizable highlight color used for title bars, selection and focus.
 * [controlFace]/[controlHighlight]/[controlShadow] are the neutral grays needed to draw
 * classic 3D bevel chrome on buttons and panels, matching the requested retro aesthetic.
 */
data class WinColors(
    val isDark: Boolean,
    val page: Color,
    val onPage: Color,
    val controlFace: Color,
    val controlHighlight: Color,
    val controlShadow: Color,
    val controlText: Color,
    val accent: Color,
    val onAccent: Color,
    val divider: Color,
    val disabled: Color,
)

fun lightWinColors(accent: Color): WinColors = WinColors(
    isDark = false,
    page = Color(0xFFFFFFFF),
    onPage = Color(0xFF000000),
    controlFace = Color(0xFFD4D4D4),
    controlHighlight = Color(0xFFFFFFFF),
    controlShadow = Color(0xFF404040),
    controlText = Color(0xFF000000),
    accent = accent,
    onAccent = contrastingOn(accent),
    divider = Color(0xFF000000),
    disabled = Color(0xFF8A8A8A),
)

fun darkWinColors(accent: Color): WinColors = WinColors(
    isDark = true,
    page = Color(0xFF000000),
    onPage = Color(0xFFFFFFFF),
    controlFace = Color(0xFF2B2B2B),
    controlHighlight = Color(0xFF5A5A5A),
    controlShadow = Color(0xFF000000),
    controlText = Color(0xFFFFFFFF),
    accent = accent,
    onAccent = contrastingOn(accent),
    divider = Color(0xFFFFFFFF),
    disabled = Color(0xFF6E6E6E),
)

val LocalWinColors = staticCompositionLocalOf { lightWinColors(DefaultAccent) }
