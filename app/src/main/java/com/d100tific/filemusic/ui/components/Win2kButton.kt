package com.d100tific.filemusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.d100tific.filemusic.ui.theme.BevelStyle
import com.d100tific.filemusic.ui.theme.LocalWinColors
import com.d100tific.filemusic.ui.theme.win2kBevel

/** A classic beveled push-button, raised at rest and sunken while pressed. */
@Composable
fun Win2kButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    content: @Composable () -> Unit,
) {
    val colors = LocalWinColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .background(colors.controlFace)
            .win2kBevel(colors, if (pressed) BevelStyle.SUNKEN else BevelStyle.RAISED)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) colors.controlText else colors.disabled,
        ) {
            content()
        }
    }
}

@Composable
fun Win2kIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 40.dp,
) {
    Win2kButton(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        contentPadding = PaddingValues(6.dp),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * An icon button that stays pushed in while [active], painted with the accent color, like the
 * selected tab of the bottom bar. Used for shuffle and repeat.
 */
@Composable
fun Win2kToggleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 40.dp,
) {
    val colors = LocalWinColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val tint = when {
        !enabled -> colors.disabled
        active -> colors.onAccent
        else -> colors.controlText
    }
    Box(
        modifier = modifier
            .size(size)
            .background(if (active) colors.accent else colors.controlFace)
            .win2kBevel(colors, if (active || pressed) BevelStyle.SUNKEN else BevelStyle.RAISED)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun Win2kTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Win2kButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(text)
    }
}
