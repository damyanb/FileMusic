package com.d100tific.filemusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d100tific.filemusic.ui.theme.LocalWinColors

/**
 * A caption-bar-style top bar reminiscent of a Windows 2000 window title bar: a solid
 * block of the accent color with bold white text, square corners, optional back arrow
 * and trailing action icons.
 */
@Composable
fun Win2kTitleBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalWinColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.accent)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            val interactionSource = remember { MutableInteractionSource() }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onBack,
                    )
                    .padding(end = 8.dp),
            )
        }
        Text(
            text = title,
            color = colors.onAccent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}
