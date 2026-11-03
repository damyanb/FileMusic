package com.d100tific.filemusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d100tific.filemusic.ui.theme.BevelStyle
import com.d100tific.filemusic.ui.theme.LocalWinColors
import com.d100tific.filemusic.ui.theme.win2kBevel

data class Win2kNavItem(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun Win2kBottomNav(
    items: List<Win2kNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWinColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.controlFace)
            // Reserves space so the bar sits above 3-button navigation without
            // wasting space on gesture-nav devices, which report a tiny inset.
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(64.dp)
            .win2kBevel(colors, BevelStyle.RAISED),
    ) {
        items.forEachIndexed { index, item ->
            Win2kNavTab(
                item = item,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RowScope.Win2kNavTab(
    item: Win2kNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWinColors.current
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .background(if (selected) colors.accent else colors.controlFace)
            .win2kBevel(colors, if (selected) BevelStyle.SUNKEN else BevelStyle.RAISED)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (selected) colors.onAccent else colors.controlText,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = item.label,
            color = if (selected) colors.onAccent else colors.controlText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
}
