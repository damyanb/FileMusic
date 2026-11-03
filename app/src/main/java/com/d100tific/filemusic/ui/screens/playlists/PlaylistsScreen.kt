package com.d100tific.filemusic.ui.screens.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d100tific.filemusic.R
import com.d100tific.filemusic.data.model.Playlist
import com.d100tific.filemusic.ui.components.PlaylistListSortFields
import com.d100tific.filemusic.ui.components.PlaylistRow
import com.d100tific.filemusic.ui.components.RowIcon
import com.d100tific.filemusic.ui.components.SortMenuAction
import com.d100tific.filemusic.ui.components.Win2kConfirmDialog
import com.d100tific.filemusic.ui.components.Win2kIconButton
import com.d100tific.filemusic.ui.components.Win2kNameDialog
import com.d100tific.filemusic.ui.components.Win2kTitleBar
import com.d100tific.filemusic.ui.theme.LocalWinColors

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    onOpenPlaylist: (Long) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val colors = LocalWinColors.current
    val state by viewModel.state.collectAsState()

    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(colors.page)) {
        Win2kTitleBar(title = stringResource(R.string.tab_playlists)) {
            Win2kIconButton(
                icon = Icons.Filled.Add,
                contentDescription = stringResource(R.string.action_new_playlist),
                onClick = { creating = true },
            )
            SortMenuAction(current = state.sort, onChange = viewModel::setSort, fields = PlaylistListSortFields)
            Win2kIconButton(
                icon = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.action_settings),
                onClick = onSettingsClick,
            )
        }
        if (state.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.empty_playlists),
                    color = colors.disabled,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.items, key = { it.playlist.id }) { item ->
                    PlaylistRow(
                        name = item.playlist.name,
                        songCount = item.songCount,
                        onClick = { onOpenPlaylist(item.playlist.id) },
                        trailing = {
                            RowIcon(
                                icon = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.action_rename),
                                tint = colors.disabled,
                                onClick = { renaming = item.playlist },
                            )
                            RowIcon(
                                icon = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = colors.disabled,
                                onClick = { deleting = item.playlist },
                            )
                        },
                    )
                }
            }
        }
    }

    if (creating) {
        Win2kNameDialog(
            title = stringResource(R.string.action_new_playlist),
            initialName = "",
            confirmText = stringResource(R.string.action_create),
            onConfirm = {
                viewModel.create(it)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }

    renaming?.let { playlist ->
        Win2kNameDialog(
            title = stringResource(R.string.action_rename),
            initialName = playlist.name,
            confirmText = stringResource(R.string.action_ok),
            onConfirm = {
                viewModel.rename(playlist.id, it)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }

    deleting?.let { playlist ->
        Win2kConfirmDialog(
            title = stringResource(R.string.delete_playlist_title),
            message = stringResource(R.string.delete_playlist_message, playlist.name),
            confirmText = stringResource(R.string.action_delete),
            onConfirm = {
                viewModel.delete(playlist.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}
