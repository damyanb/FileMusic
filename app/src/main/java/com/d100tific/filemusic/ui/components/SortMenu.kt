package com.d100tific.filemusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.d100tific.filemusic.R
import com.d100tific.filemusic.data.model.SortField
import com.d100tific.filemusic.data.model.SortOrder
import com.d100tific.filemusic.data.model.SortSpec
import com.d100tific.filemusic.ui.theme.BevelStyle
import com.d100tific.filemusic.ui.theme.LocalWinColors
import com.d100tific.filemusic.ui.theme.win2kBevel

/** Sort fields offered for lists of songs. [SortField.ADDED] only makes sense inside a playlist. */
val SongSortFields = listOf(
    SortField.NAME,
    SortField.ARTIST,
    SortField.ALBUM,
    SortField.DATE,
    SortField.SIZE,
    SortField.DURATION,
)
val PlaylistSongSortFields = listOf(SortField.ADDED) + SongSortFields
val PlaylistListSortFields = listOf(SortField.NAME, SortField.DATE)

@Composable
fun SortMenuAction(
    current: SortSpec,
    onChange: (SortSpec) -> Unit,
    fields: List<SortField> = SongSortFields,
) {
    var expanded by remember { mutableStateOf(false) }
    Win2kIconButton(
        icon = Icons.AutoMirrored.Filled.Sort,
        contentDescription = stringResource(R.string.action_sort),
        onClick = { expanded = true },
    )
    if (expanded) {
        Popup(alignment = Alignment.TopEnd, onDismissRequest = { expanded = false }) {
            SortMenuPanel(
                current = current,
                fields = fields,
                onChange = {
                    onChange(it)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun sortFieldLabel(field: SortField): String = stringResource(
    when (field) {
        SortField.NAME -> R.string.sort_by_name
        SortField.ARTIST -> R.string.sort_by_artist
        SortField.ALBUM -> R.string.sort_by_album
        SortField.DATE -> R.string.sort_by_date
        SortField.SIZE -> R.string.sort_by_size
        SortField.DURATION -> R.string.sort_by_duration
        SortField.ADDED -> R.string.sort_by_added
    },
)

@Composable
private fun SortMenuPanel(current: SortSpec, fields: List<SortField>, onChange: (SortSpec) -> Unit) {
    val colors = LocalWinColors.current
    Column(
        modifier = Modifier
            .width(200.dp)
            .background(colors.controlFace)
            .win2kBevel(colors, BevelStyle.RAISED)
            .padding(2.dp),
    ) {
        fields.forEach { field ->
            SortRow(sortFieldLabel(field), current.field == field) {
                onChange(current.copy(field = field))
            }
        }
        HorizontalDivider(color = colors.onPage, thickness = 1.dp)
        SortRow(stringResource(R.string.sort_ascending), current.order == SortOrder.ASCENDING) {
            onChange(current.copy(order = SortOrder.ASCENDING))
        }
        SortRow(stringResource(R.string.sort_descending), current.order == SortOrder.DESCENDING) {
            onChange(current.copy(order = SortOrder.DESCENDING))
        }
    }
}

@Composable
private fun SortRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalWinColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.padding(end = 8.dp))
        } else {
            Spacer(modifier = Modifier.width(32.dp))
        }
        Text(label, color = colors.controlText)
    }
}
