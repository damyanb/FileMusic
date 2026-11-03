package com.d100tific.filemusic.ui.songactions

import android.app.Activity
import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.d100tific.filemusic.FileMusicApp
import com.d100tific.filemusic.R
import com.d100tific.filemusic.data.media.DeleteOutcome
import com.d100tific.filemusic.data.model.Song
import com.d100tific.filemusic.ui.components.PlaylistPickerDialog
import com.d100tific.filemusic.ui.components.Win2kConfirmDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The two things that can be done to a song from several different screens - managing which
 * playlists it is in, and deleting it - kept in one place so every screen behaves the same.
 * The dialogs themselves are drawn once by [SongActionsHost].
 */
@Stable
class SongActions(
    private val app: FileMusicApp,
    private val scope: CoroutineScope,
) {
    var playlistTarget by mutableStateOf<Song?>(null)
        private set
    var pendingDelete by mutableStateOf<List<Song>?>(null)
        private set

    internal var launchConsent: ((IntentSender) -> Unit)? = null
    private var awaitingConsent: DeleteOutcome.NeedsConsent? = null

    /** Opens the "save to playlist" menu for [song]. */
    fun managePlaylists(song: Song) {
        playlistTarget = song
    }

    /** Asks for confirmation, then permanently deletes the files. */
    fun confirmDelete(songs: List<Song>) {
        if (songs.isNotEmpty()) pendingDelete = songs
    }

    internal fun dismissPlaylists() {
        playlistTarget = null
    }

    internal fun cancelDelete() {
        pendingDelete = null
    }

    internal fun runDelete() {
        val songs = pendingDelete ?: return
        pendingDelete = null
        scope.launch { process(songs, emptyList()) }
    }

    internal fun onConsentResult(approved: Boolean) {
        val consent = awaitingConsent ?: return
        awaitingConsent = null
        when {
            !approved -> finish(consent.deletedSoFar)
            consent.systemDeletes -> finish(consent.deletedSoFar + consent.remaining)
            // Android 10: the user unlocked the file, now it has to be deleted for real.
            else -> scope.launch { process(consent.remaining, consent.deletedSoFar) }
        }
    }

    private suspend fun process(songs: List<Song>, alreadyDeleted: List<Song>) {
        when (val outcome = app.songDeleter.delete(songs)) {
            is DeleteOutcome.Done -> finish(alreadyDeleted + outcome.deleted)
            is DeleteOutcome.Failed -> finish(alreadyDeleted + outcome.deleted)
            is DeleteOutcome.NeedsConsent -> {
                awaitingConsent = outcome.copy(deletedSoFar = alreadyDeleted + outcome.deletedSoFar)
                val launcher = launchConsent
                if (launcher != null) launcher(outcome.intentSender) else finish(alreadyDeleted)
            }
        }
    }

    /** The files are gone: forget them in playlists, the play queue and the library. */
    private fun finish(deleted: List<Song>) {
        if (deleted.isEmpty()) return
        val keys = deleted.map { it.key }
        scope.launch {
            app.playlistRepository.removeSongsEverywhere(keys)
            app.playerConnection.removeFromQueue(keys)
            app.libraryRepository.refresh()
        }
    }
}

val LocalSongActions = compositionLocalOf<SongActions> { error("SongActions not provided") }

/** Draws the dialogs owned by [actions]. Put it once near the root of the UI. */
@Composable
fun SongActionsHost(app: FileMusicApp, actions: SongActions) {
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        actions.onConsentResult(result.resultCode == Activity.RESULT_OK)
    }
    actions.launchConsent = { sender -> consentLauncher.launch(IntentSenderRequest.Builder(sender).build()) }

    val scope = rememberCoroutineScope()

    actions.playlistTarget?.let { song ->
        val playlists by app.playlistRepository.playlists.collectAsState()
        val membership by app.playlistRepository.membership.collectAsState(initial = emptyMap())
        PlaylistPickerDialog(
            songTitle = song.title,
            playlists = playlists,
            memberOf = membership[song.key].orEmpty(),
            onToggle = { playlist, checked ->
                scope.launch {
                    if (checked) app.playlistRepository.addSong(playlist.id, song.key)
                    else app.playlistRepository.removeSong(playlist.id, song.key)
                }
            },
            onCreate = { name -> scope.launch { app.playlistRepository.create(name, song.key) } },
            onDismiss = actions::dismissPlaylists,
        )
    }

    actions.pendingDelete?.let { songs ->
        Win2kConfirmDialog(
            title = if (songs.size == 1) {
                stringResource(R.string.delete_song_title)
            } else {
                pluralStringResource(R.plurals.delete_songs_title, songs.size, songs.size)
            },
            message = if (songs.size == 1) {
                stringResource(R.string.delete_song_message, songs.first().title)
            } else {
                stringResource(R.string.delete_songs_message)
            },
            confirmText = stringResource(R.string.action_delete),
            onConfirm = actions::runDelete,
            onDismiss = actions::cancelDelete,
        )
    }
}

@Composable
fun rememberSongActions(app: FileMusicApp): SongActions {
    val scope = rememberCoroutineScope()
    return remember(app) { SongActions(app, scope) }
}
