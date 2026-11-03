package com.d100tific.filemusic.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Everything in this app uses perfectly square corners, per the Windows 2000 aesthetic. */
private val SquareCorners = RoundedCornerShape(0.dp)
private val Win2kShapes = Shapes(
    extraSmall = SquareCorners,
    small = SquareCorners,
    medium = SquareCorners,
    large = SquareCorners,
    extraLarge = SquareCorners,
)

private val Win2kTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 18.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, fontWeight = FontWeight.Bold),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun FileMusicTheme(
    darkMode: Boolean,
    accentColor: Color,
    content: @Composable () -> Unit,
) {
    val winColors = if (darkMode) darkWinColors(accentColor) else lightWinColors(accentColor)
    val materialScheme = if (darkMode) {
        androidx.compose.material3.darkColorScheme(
            primary = accentColor,
            background = winColors.page,
            onBackground = winColors.onPage,
            surface = winColors.page,
            onSurface = winColors.onPage,
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = accentColor,
            background = winColors.page,
            onBackground = winColors.onPage,
            surface = winColors.page,
            onSurface = winColors.onPage,
        )
    }
    CompositionLocalProvider(LocalWinColors provides winColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = Win2kTypography,
            shapes = Win2kShapes,
            content = content,
        )
    }
}
