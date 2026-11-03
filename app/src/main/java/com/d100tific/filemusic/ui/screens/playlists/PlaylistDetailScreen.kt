package com.d100tific.filemusic.ui.screens.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d100tific.filemusic.R
import com.d100tific.filemusic.ui.components.PlaylistSongSortFields
import com.d100tific.filemusic.ui.components.RowIcon
import com.d100tific.filemusic.ui.components.SongRow
import com.d100tific.filemusic.ui.components.SortMenuAction
import com.d100tific.filemusic.ui.components.Win2kIconButton
import com.d100tific.filemusic.ui.components.Win2kTitleBar
import com.d100tific.filemusic.ui.songactions.LocalSongActions
import com.d100tific.filemusic.ui.theme.LocalWinColors

@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel,
    currentSongKey: String?,
    onBack: () -> Unit,
    onSongPlayed: () -> Unit,
) {
    val colors = LocalWinColors.current
    val songActions = LocalSongActions.current
    val state by viewModel.state.collectAsState()

    // The playlist was deleted from somewhere else: nothing left to show here.
    LaunchedEffect(state.missing) {
        if (state.missing) onBack()
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.page)) {
        Win2kTitleBar(title = state.name, onBack = onBack) {
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
            SortMenuAction(current = state.sort, onChange = viewModel::setSort, fields = PlaylistSongSortFields)
        }
        if (state.songs.isEmpty() && !state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.empty_playlist),
                    color = colors.disabled,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(state.songs, key = { _, song -> song.key }) { index, song ->
                    SongRow(
                        song = song,
                        isCurrent = song.key == currentSongKey,
                        inAnyPlaylist = true,
                        onClick = {
                            viewModel.play(index)
                            onSongPlayed()
                        },
                        onManagePlaylists = { songActions.managePlaylists(song) },
                        trailing = {
                            RowIcon(
                                icon = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_remove_from_playlist),
                                tint = colors.disabled,
                                onClick = { viewModel.removeSong(song) },
                            )
                        },
                    )
                }
            }
        }
    }
}
