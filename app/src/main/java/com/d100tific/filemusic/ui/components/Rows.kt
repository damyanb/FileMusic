package com.d100tific.filemusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d100tific.filemusic.R
import com.d100tific.filemusic.data.model.Song
import com.d100tific.filemusic.ui.theme.LocalWinColors

/** 3:07 or 1:02:09. */
fun formatTime(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

/** A small tappable icon at the end of a row (the music equivalent of the gallery's favorite star). */
@Composable
fun RowIcon(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .size(36.dp)
            .clickable(onClick = onClick)
            .padding(6.dp),
    )
}

@Composable
fun FolderRow(
    name: String,
    songCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = LocalWinColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(28.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(name, color = colors.onPage, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = subtitle ?: pluralStringResource(R.plurals.songs_count, songCount, songCount),
                color = colors.disabled,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun PlaylistRow(
    name: String,
    songCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalWinColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(28.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(name, color = colors.onPage, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = pluralStringResource(R.plurals.songs_count, songCount, songCount),
                color = colors.disabled,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        trailing()
    }
}

/**
 * One song, as plain text: title, then artist / album / length underneath. The song being
 * played is drawn in the accent color. The first trailing icon shows (like the gallery's star)
 * whether the song is in any playlist and opens the playlist menu; [trailing] adds more icons.
 */
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    inAnyPlaylist: Boolean,
    onClick: () -> Unit,
    onManagePlaylists: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalWinColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isCurrent) Icons.Filled.PlayArrow else Icons.Filled.MusicNote,
            contentDescription = null,
            tint = if (isCurrent) colors.accent else colors.disabled,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                text = song.title,
                color = if (isCurrent) colors.accent else colors.onPage,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val details = listOfNotNull(
                song.artist.ifEmpty { null },
                song.album.ifEmpty { null },
                formatTime(song.durationMs),
            ).joinToString("  \u2022  ")
            Text(
                text = details,
                color = colors.disabled,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RowIcon(
            icon = if (inAnyPlaylist) Icons.AutoMirrored.Filled.PlaylistAddCheck else Icons.AutoMirrored.Filled.PlaylistAdd,
            contentDescription = stringResource(R.string.action_manage_playlists),
            tint = if (inAnyPlaylist) colors.accent else colors.disabled,
            onClick = onManagePlaylists,
        )
        trailing()
    }
}
