package com.d100tific.filemusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.d100tific.filemusic.R
import com.d100tific.filemusic.data.model.Playlist
import com.d100tific.filemusic.ui.theme.BevelStyle
import com.d100tific.filemusic.ui.theme.LocalWinColors
import com.d100tific.filemusic.ui.theme.win2kBevel

/** A confirmation dialog in the square, beveled style used all over the app. */
@Composable
fun Win2kConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWinColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = colors.controlFace,
        title = { Text(title, color = colors.controlText) },
        text = { Text(message, color = colors.controlText) },
        confirmButton = { Win2kTextButton(text = confirmText, onClick = onConfirm) },
        dismissButton = { Win2kTextButton(text = stringResource(R.string.action_cancel), onClick = onDismiss) },
    )
}

/** A sunken single-line text box. */
@Composable
fun Win2kTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
    requestFocus: Boolean = false,
) {
    val colors = LocalWinColors.current
    val focusRequester = remember { FocusRequester() }
    if (requestFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
    Box(
        modifier = modifier
            .background(colors.page)
            .win2kBevel(colors, BevelStyle.SUNKEN)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = colors.disabled)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onPage),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
    }
}

/** Asks for a playlist name: used to create and to rename playlists. */
@Composable
fun Win2kNameDialog(
    title: String,
    initialName: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWinColors.current
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = colors.controlFace,
        title = { Text(title, color = colors.controlText) },
        text = {
            Win2kTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.playlist_name_hint),
                onDone = { if (name.isNotBlank()) onConfirm(name) },
                requestFocus = true,
            )
        },
        confirmButton = {
            Win2kTextButton(text = confirmText, enabled = name.isNotBlank(), onClick = { onConfirm(name) })
        },
        dismissButton = { Win2kTextButton(text = stringResource(R.string.action_cancel), onClick = onDismiss) },
    )
}

/**
 * The "save to playlist" menu: every playlist with a checkbox that says whether the song is in
 * it (tap to add or remove right away), plus a way to start a new playlist containing the song.
 */
@Composable
fun PlaylistPickerDialog(
    songTitle: String,
    playlists: List<Playlist>,
    memberOf: Set<Long>,
    onToggle: (Playlist, Boolean) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalWinColors.current
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(colors.page)
                .win2kBevel(colors, BevelStyle.RAISED)
                .padding(2.dp),
        ) {
            Win2kTitleBar(title = stringResource(R.string.playlist_picker_title))
            Text(
                text = songTitle,
                color = colors.disabled,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider(color = colors.onPage, thickness = 1.dp)

            if (playlists.isEmpty() && !creating) {
                Text(
                    text = stringResource(R.string.playlist_picker_empty),
                    color = colors.disabled,
                    modifier = Modifier.padding(16.dp),
                )
            }
            LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                items(playlists, key = { it.id }) { playlist ->
                    val checked = playlist.id in memberOf
                    Win2kCheckboxRow(
                        label = playlist.name,
                        checked = checked,
                        onCheckedChange = { onToggle(playlist, it) },
                    )
                }
            }
            HorizontalDivider(color = colors.onPage, thickness = 1.dp)

            if (creating) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Win2kTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = stringResource(R.string.playlist_name_hint),
                        onDone = {
                            if (newName.isNotBlank()) {
                                onCreate(newName)
                                newName = ""
                                creating = false
                            }
                        },
                        requestFocus = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        Win2kTextButton(
                            text = stringResource(R.string.action_cancel),
                            onClick = {
                                newName = ""
                                creating = false
                            },
                        )
                        Win2kTextButton(
                            text = stringResource(R.string.action_create),
                            enabled = newName.isNotBlank(),
                            onClick = {
                                onCreate(newName)
                                newName = ""
                                creating = false
                            },
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { creating = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(R.string.action_new_playlist),
                        color = colors.onPage,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                HorizontalDivider(color = colors.onPage, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Win2kTextButton(text = stringResource(R.string.action_done), onClick = onDismiss)
                }
            }
        }
    }
}