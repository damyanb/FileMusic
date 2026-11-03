package com.d100tific.filemusic.data.media

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import com.d100tific.filemusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What happened when trying to delete some songs. */
sealed interface DeleteOutcome {
    /** Every requested song is gone. */
    data class Done(val deleted: List<Song>) : DeleteOutcome

    /**
     * Android wants the user to confirm first. Launch [intentSender]; when the user agrees,
     * call [SongDeleter.delete] again with [remaining] on Android 10 (the confirmation only
     * unlocks the file and it must be deleted again). On Android 11+ the system performs the
     * deletion itself, so [remaining] are already deleted once the user agrees.
     */
    data class NeedsConsent(
        val deletedSoFar: List<Song>,
        val remaining: List<Song>,
        val intentSender: IntentSender,
        val systemDeletes: Boolean,
    ) : DeleteOutcome

    data class Failed(val deleted: List<Song>) : DeleteOutcome
}

/** Permanently deletes audio files, going through whatever consent flow the Android version needs. */
class SongDeleter(private val context: Context) {

    suspend fun delete(songs: List<Song>): DeleteOutcome = withContext(Dispatchers.IO) {
        if (songs.isEmpty()) return@withContext DeleteOutcome.Done(emptyList())
        val resolver = context.contentResolver

        // Android 11+: this app does not own the files, so ask the system to delete them (one
        // confirmation dialog for the whole batch).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return@withContext try {
                val request = MediaStore.createDeleteRequest(resolver, songs.map { it.uri })
                DeleteOutcome.NeedsConsent(emptyList(), songs, request.intentSender, systemDeletes = true)
            } catch (e: Exception) {
                DeleteOutcome.Failed(emptyList())
            }
        }

        val deleted = mutableListOf<Song>()
        for ((index, song) in songs.withIndex()) {
            try {
                if (resolver.delete(song.uri, null, null) > 0) deleted += song
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                    return@withContext DeleteOutcome.NeedsConsent(
                        deletedSoFar = deleted,
                        remaining = songs.drop(index),
                        intentSender = e.userAction.actionIntent.intentSender,
                        systemDeletes = false,
                    )
                }
                // Cannot delete this one; carry on with the rest.
            } catch (e: Exception) {
                // Same: skip the file that could not be removed.
            }
        }
        if (deleted.size == songs.size) DeleteOutcome.Done(deleted) else DeleteOutcome.Failed(deleted)
    }
}
