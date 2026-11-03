package com.d100tific.filemusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d100tific.filemusic.ui.theme.BevelStyle
import com.d100tific.filemusic.ui.theme.LocalWinColors
import com.d100tific.filemusic.ui.theme.win2kBevel

@Composable
fun Win2kCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWinColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(colors.page)
                .win2kBevel(colors, BevelStyle.SUNKEN),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
            }
        }
        Text(label, color = colors.onPage, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
fun Win2kRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWinColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(colors.page)
                .win2kBevel(colors, BevelStyle.SUNKEN),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(modifier = Modifier.size(10.dp).background(colors.accent))
            }
        }
        Text(label, color = colors.onPage, modifier = Modifier.padding(start = 12.dp))
    }
}
