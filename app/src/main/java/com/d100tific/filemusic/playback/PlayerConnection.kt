package com.d100tific.filemusic.playback

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.d100tific.filemusic.data.media.FolderTree
import com.d100tific.filemusic.data.media.LibraryRepository
import com.d100tific.filemusic.data.media.LibraryState
import com.d100tific.filemusic.data.model.FolderRef
import com.d100tific.filemusic.data.model.QueueKind
import com.d100tific.filemusic.data.model.QueueSource
import com.d100tific.filemusic.data.model.Song
import com.d100tific.filemusic.data.model.SortSpec
import com.d100tific.filemusic.data.model.sortedBySpec
import com.d100tific.filemusic.data.playlists.PlaylistRepository
import com.d100tific.filemusic.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Everything the UI needs to know about the player, in one immutable snapshot. */
data class PlayerUiState(
    val connected: Boolean = false,
    /** True when a song is loaded (playing, paused, or restored from the last session). */
    val hasSong: Boolean = false,
    val songKey: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffle: Boolean = false,
    val hasNext: Boolean = false,
    val source: QueueSource? = null,
)

/**
 * The app-side handle on [PlaybackService]. It turns library songs into a play queue, mirrors
 * the player into [state], and remembers what was playing so the next launch can show it again.
 */
class PlayerConnection(
    private val context: Context,
    private val library: LibraryRepository,
    private val playlists: PlaylistRepository,
    private val settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var controller: MediaController? = null
    private var started = false
    private var source: QueueSource? = null
    private var consecutiveErrors = 0
    private val whenConnected = mutableListOf<(MediaController) -> Unit>()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish()
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) && player.isPlaying) consecutiveErrors = 0
            if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                )
            ) {
                saveSession()
            }
            if (events.containsAny(Player.EVENT_REPEAT_MODE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
                scope.launch { settings.savePlayerModes(player.shuffleModeEnabled, player.repeatMode) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val c = controller ?: return
            // A file that cannot be played (deleted behind our back, unsupported codec...) must
            // not stop the music: skip to the next one, but give up if every song fails.
            consecutiveErrors++
            if (c.hasNextMediaItem() && consecutiveErrors < c.mediaItemCount) {
                c.seekToNextMediaItem()
                c.prepare()
                c.play()
            } else {
                consecutiveErrors = 0
            }
        }
    }

    /** Idempotent. Connects to the playback service and restores the last session. */
    @OptIn(UnstableApi::class)
    fun start() {
        if (started) return
        started = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val connected = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = connected
                connected.addListener(listener)
                publish()
                whenConnected.forEach { it(connected) }
                whenConnected.clear()
                scope.launch { restoreSession(connected) }
                scope.launch { periodicallySaveSession() }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    // ---------------------------------------------------------------------------------------
    // Commands
    // ---------------------------------------------------------------------------------------

    /** Replaces the queue with [songs] and starts playing [startIndex]. */
    fun play(songs: List<Song>, startIndex: Int, queueSource: QueueSource) {
        if (songs.isEmpty()) return
        runWhenConnected { c ->
            source = queueSource
            c.setMediaItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(songs.indices), 0L)
            c.prepare()
            c.play()
            publish()
            saveSession()
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        val active = c.playWhenReady &&
            (c.playbackState == Player.STATE_READY || c.playbackState == Player.STATE_BUFFERING)
        if (active) {
            c.pause()
            return
        }
        when (c.playbackState) {
            Player.STATE_IDLE -> c.prepare()
            // The whole queue finished (no repeat): start over from its first song.
            Player.STATE_ENDED -> c.seekTo(0, 0L)
            else -> Unit
        }
        c.play()
    }

    fun next() {
        val c = controller ?: return
        if (!c.hasNextMediaItem()) return
        ensurePrepared(c)
        c.seekToNextMediaItem()
    }

    /** Restarts the current song if it has been playing for a few seconds, otherwise goes back one song. */
    fun previous() {
        val c = controller ?: return
        ensurePrepared(c)
        c.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    /** Like VLC: no repeat -> repeat the whole queue -> repeat the current song -> no repeat. */
    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /** Drops the given songs from the queue, e.g. because their files were deleted. */
    fun removeFromQueue(songKeys: Collection<String>) {
        val c = controller ?: return
        val keys = songKeys.toSet()
        for (index in (0 until c.mediaItemCount).reversed()) {
            if (c.getMediaItemAt(index).mediaId in keys) c.removeMediaItem(index)
        }
        publish()
    }

    fun currentPositionMs(): Long = controller?.currentPosition ?: 0L

    // ---------------------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------------------

    private fun ensurePrepared(c: MediaController) {
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
    }

    private fun runWhenConnected(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else whenConnected += block
    }

    private fun publish() {
        val c = controller ?: return
        val item = c.currentMediaItem
        val metadata = item?.mediaMetadata
        val duration = c.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: metadata?.durationMs ?: 0L
        _state.value = PlayerUiState(
            connected = true,
            hasSong = item != null,
            songKey = item?.mediaId,
            title = metadata?.title?.toString().orEmpty(),
            artist = metadata?.artist?.toString().orEmpty(),
            album = metadata?.albumTitle?.toString().orEmpty(),
            isPlaying = c.isPlaying,
            durationMs = duration,
            repeatMode = c.repeatMode,
            shuffle = c.shuffleModeEnabled,
            hasNext = c.hasNextMediaItem(),
            source = source,
        )
    }

    private fun saveSession() {
        val c = controller ?: return
        val key = c.currentMediaItem?.mediaId ?: return
        val position = c.currentPosition
        val currentSource = source
        scope.launch { settings.saveSession(currentSource, key, position) }
    }

    private suspend fun periodicallySaveSession() {
        while (true) {
            delay(SAVE_INTERVAL_MS)
            if (controller?.isPlaying == true) saveSession()
        }
    }

    private suspend fun restoreSession(c: MediaController) {
        val (shuffle, repeat) = settings.loadPlayerModes()
        c.shuffleModeEnabled = shuffle
        c.repeatMode = repeat

        // The service may already be playing (e.g. the activity was recreated): leave it alone.
        if (c.mediaItemCount > 0) return

        val saved = settings.loadSession() ?: return
        val libraryState = library.state.first { !it.isLoading }
        val lastSong = libraryState.byKey[saved.songKey] ?: return

        var queueSource = saved.source
        var queue: List<Song> = queueSource?.let { songsFor(it, libraryState) }.orEmpty()
        var index = queue.indexOfFirst { it.key == saved.songKey }
        if (index < 0) {
            queue = listOf(lastSong)
            index = 0
            queueSource = QueueSource(QueueKind.SINGLE, lastSong.key, "", SortSpec.DEFAULT)
        }

        // Another command may have started playback while we were loading.
        if (c.mediaItemCount > 0) return
        source = queueSource
        // Deliberately not prepared: the song is loaded and shown but nothing starts and no
        // notification appears until the user presses play.
        c.setMediaItems(queue.map { it.toMediaItem() }, index, saved.positionMs)
        publish()
    }

    private fun songsFor(source: QueueSource, state: LibraryState): List<Song> = when (source.kind) {
        QueueKind.FOLDER ->
            FolderTree.listFolder(state.songs, FolderRef.fromId(source.id), source.sort).songs

        QueueKind.PLAYLIST -> {
            val playlist = source.id.toLongOrNull()?.let { playlists.find(it) }
            if (playlist == null) {
                emptyList()
            } else {
                val addedAt = playlist.entries.associate { it.songKey to it.addedAt }
                playlist.entries.mapNotNull { state.byKey[it.songKey] }.sortedBySpec(source.sort, addedAt)
            }
        }

        QueueKind.SINGLE -> emptyList()
    }

    private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(key)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist.ifEmpty { null })
                .setAlbumTitle(album.ifEmpty { null })
                .setDurationMs(durationMs)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build(),
        )
        .build()

    private companion object {
        const val SAVE_INTERVAL_MS = 5_000L
    }
}
