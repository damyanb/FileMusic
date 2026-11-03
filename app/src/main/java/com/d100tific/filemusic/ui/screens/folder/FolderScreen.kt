package com.d100tific.filemusic.ui.screens.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d100tific.filemusic.R
import com.d100tific.filemusic.data.model.VolumeType
import com.d100tific.filemusic.ui.components.FolderRow
import com.d100tific.filemusic.ui.components.RowIcon
import com.d100tific.filemusic.ui.components.SongRow
import com.d100tific.filemusic.ui.components.SongSortFields
import com.d100tific.filemusic.ui.components.SortMenuAction
import com.d100tific.filemusic.ui.components.Win2kIconButton
import com.d100tific.filemusic.ui.components.Win2kTitleBar
import com.d100tific.filemusic.ui.songactions.LocalSongActions
import com.d100tific.filemusic.ui.theme.LocalWinColors

@Composable
fun FolderScreen(
    viewModel: FolderViewModel,
    currentSongKey: String?,
    onBack: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onSongPlayed: () -> Unit,
    onSettingsClick: () -> Unit = {},
) {
    val colors = LocalWinColors.current
    val songActions = LocalSongActions.current
    val state by viewModel.state.collectAsState()

    val title = if (state.isRoot) stringResource(R.string.root_files_title) else state.title

    Column(modifier = Modifier.fillMaxSize().background(colors.page)) {
        Win2kTitleBar(title = title, onBack = if (state.isRoot) null else onBack) {
            if (state.songs.isNotEmpty()) {
                Win2kIconButton(
                    icon = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.action_play_all),
                    onClick = {
                        viewModel.play(0)
                        onSongPlayed()
                    },
                )
            }
            SortMenuAction(current = state.sort, onChange = viewModel::setSort, fields = SongSortFields)
            if (state.isRoot) {
                Win2kIconButton(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.action_settings),
                    onClick = onSettingsClick,
                )
            }
        }
        if (state.subfolders.isEmpty() && state.songs.isEmpty() && !state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.empty_folder),
                    color = colors.disabled,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.subfolders.size, key = { "folder:" + state.subfolders[it].ref.id }) { index ->
                    val folder = state.subfolders[index]
                    val displayName = if (state.isRoot && folder.ref.relativePath.isEmpty()) {
                        if (folder.ref.volumeType == VolumeType.INTERNAL) {
                            stringResource(R.string.volume_internal)
                        } else {
                            stringResource(R.string.volume_sdcard)
                        }
                    } else {
                        folder.ref.name
                    }
                    FolderRow(
                        name = displayName,
                        songCount = folder.songCount,
                        onClick = { onOpenFolder(folder.ref.id) },
                    )
                }
                itemsIndexed(state.songs, key = { _, song -> "song:" + song.key }) { index, song ->
                    SongRow(
                        song = song,
                        isCurrent = song.key == currentSongKey,
                        inAnyPlaylist = song.key in state.songsInPlaylists,
                        onClick = {
                            viewModel.play(index)
                            onSongPlayed()
                        },
                        onManagePlaylists = { songActions.managePlaylists(song) },
                        trailing = {
                            RowIcon(
                                icon = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = colors.disabled,
                                onClick = { songActions.confirmDelete(listOf(song)) },
                            )
                        },
                    )
                }
            }
        }
    }
}
