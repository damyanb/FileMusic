package com.d100tific.filemusic.data.media

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.d100tific.filemusic.data.model.FolderListing
import com.d100tific.filemusic.data.model.FolderRef
import com.d100tific.filemusic.data.model.MediaFolder
import com.d100tific.filemusic.data.model.Song
import com.d100tific.filemusic.data.model.SortSpec
import com.d100tific.filemusic.data.model.StorageVolume
import com.d100tific.filemusic.data.model.VolumeType
import com.d100tific.filemusic.data.model.sortedBySpec
import com.d100tific.filemusic.data.model.sortedFoldersBySpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
) {
    val byKey: Map<String, Song> by lazy(LazyThreadSafetyMode.PUBLICATION) { songs.associateBy { it.key } }
}

/**
 * Reads audio files from MediaStore only - internal storage and removable SD cards, nothing
 * else (no cloud, no other media types). The whole library is read once into memory and kept
 * up to date by watching MediaStore for changes. This class only ever performs read queries;
 * deleting a song is handled separately by [SongDeleter].
 */
class LibraryRepository(private val context: Context) {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    private var started = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    /** Idempotent. Call once the audio permission has been granted. */
    @Synchronized
    fun start() {
        if (started) return
        started = true
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        scope.launch {
            load()
            for (request in refreshRequests) {
                // Debounce bursts of changes (e.g. while a card is being scanned).
                delay(REFRESH_DEBOUNCE_MS)
                load()
            }
        }
    }

    fun refresh() {
        refreshRequests.trySend(Unit)
    }

    private fun load() {
        val songs = runCatching { queryAll() }.getOrDefault(_state.value.songs)
        _state.value = LibraryState(songs = songs, isLoading = false)
    }

    // ---------------------------------------------------------------------------------------
    // Volumes
    // ---------------------------------------------------------------------------------------

    fun listVolumes(): List<StorageVolume> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return MediaStore.getExternalVolumeNames(context).map { name ->
                val type = if (name == MediaStore.VOLUME_EXTERNAL_PRIMARY) VolumeType.INTERNAL else VolumeType.SD_CARD
                StorageVolume(name, type, "")
            }.sortedBy { if (it.type == VolumeType.INTERNAL) 0 else 1 }
        }
        val roots = StorageVolumes.legacyVolumeRoots(context)
        if (roots.isEmpty()) return listOf(StorageVolume(LEGACY_PRIMARY, VolumeType.INTERNAL, ""))
        return roots.mapIndexed { index, root ->
            if (index == 0) StorageVolume(LEGACY_PRIMARY, VolumeType.INTERNAL, root)
            else StorageVolume("$LEGACY_SD_PREFIX$index", VolumeType.SD_CARD, root)
        }
    }

    // ---------------------------------------------------------------------------------------
    // MediaStore queries
    // ---------------------------------------------------------------------------------------

    private fun queryAll(): List<Song> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(context).flatMap { name ->
                val type = if (name == MediaStore.VOLUME_EXTERNAL_PRIMARY) VolumeType.INTERNAL else VolumeType.SD_CARD
                queryUri(MediaStore.Audio.Media.getContentUri(name), name, type)
            }
        } else {
            queryUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, LEGACY_PRIMARY, VolumeType.INTERNAL)
        }
    }

    private fun queryUri(uri: Uri, volumeName: String, volumeType: VolumeType): List<Song> {
        val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        @Suppress("DEPRECATION")
        val pathColumn = if (modern) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            pathColumn,
        )
        // Ringtones, notification sounds and alarms are not music.
        val selection = "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND " +
            "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
            "${MediaStore.Audio.Media.IS_ALARM} = 0"
        val legacyRoots = if (modern) emptyList() else StorageVolumes.legacyVolumeRoots(context)

        val songs = ArrayList<Song>()
        context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val pathCol = cursor.getColumnIndexOrThrow(pathColumn)
            while (cursor.moveToNext()) {
                val displayName = cursor.getString(nameCol) ?: continue
                val rawPath = cursor.getString(pathCol) ?: ""

                val folder: FolderRef = if (modern) {
                    val relative = if (rawPath.isEmpty() || rawPath.endsWith("/")) rawPath else "$rawPath/"
                    FolderRef(volumeName, volumeType, relative)
                } else {
                    legacyFolderFor(legacyRoots, rawPath)
                }

                val id = cursor.getLong(idCol)
                songs += Song(
                    id = id,
                    uri = ContentUris.withAppendedId(uri, id),
                    key = "${folder.volumeName}|${folder.relativePath}$displayName",
                    title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBeforeLast('.'),
                    artist = cleanTag(cursor.getString(artistCol)),
                    album = cleanTag(cursor.getString(albumCol)),
                    displayName = displayName,
                    durationMs = cursor.getLong(durationCol),
                    dateModified = cursor.getLong(dateCol) * 1000L,
                    size = cursor.getLong(sizeCol),
                    folder = folder,
                )
            }
        }
        return songs
    }

    /** On Android 9 and older there is no RELATIVE_PATH column; work it out from the absolute path. */
    private fun legacyFolderFor(roots: List<String>, absolutePath: String): FolderRef {
        roots.forEachIndexed { index, root ->
            if (absolutePath.startsWith("$root/")) {
                val relative = StorageVolumes.legacyRelativeFolder(context, absolutePath)
                return if (index == 0) {
                    FolderRef(LEGACY_PRIMARY, VolumeType.INTERNAL, relative)
                } else {
                    FolderRef("$LEGACY_SD_PREFIX$index", VolumeType.SD_CARD, relative)
                }
            }
        }
        return FolderRef(LEGACY_PRIMARY, VolumeType.INTERNAL, StorageVolumes.legacyRelativeFolder(context, absolutePath))
    }

    private fun cleanTag(value: String?): String =
        if (value == null || value == MediaStore.UNKNOWN_STRING) "" else value.trim()

    companion object {
        private const val REFRESH_DEBOUNCE_MS = 600L
        private const val LEGACY_PRIMARY = "external_primary"
        private const val LEGACY_SD_PREFIX = "sd"
    }
}

/** Pure helpers that turn the flat song list into a folder tree, like a file explorer would. */
object FolderTree {

    fun listFolder(songs: List<Song>, folder: FolderRef, sort: SortSpec): FolderListing {
        val parent = folder.relativePath
        val direct = ArrayList<Song>()
        val childGroups = LinkedHashMap<String, MutableList<Song>>()

        for (song in songs) {
            if (song.folder.volumeName != folder.volumeName) continue
            val path = song.folder.relativePath
            if (!path.startsWith(parent)) continue
            if (path == parent) {
                direct += song
            } else {
                val childName = path.removePrefix(parent).substringBefore('/')
                if (childName.isNotEmpty()) childGroups.getOrPut(childName) { mutableListOf() } += song
            }
        }

        val subfolders = childGroups.map { (childName, childSongs) ->
            MediaFolder(
                ref = folder.child(childName),
                songCount = childSongs.size,
                latestDateModified = childSongs.maxOf { it.dateModified },
                totalSize = childSongs.sumOf { it.size },
                totalDurationMs = childSongs.sumOf { it.durationMs },
            )
        }
        return FolderListing(
            folder = folder,
            subfolders = subfolders.sortedFoldersBySpec(sort),
            songs = direct.sortedBySpec(sort),
        )
    }

    fun folderMeta(songs: List<Song>, folder: FolderRef): MediaFolder? {
        val inside = songs.filter {
            it.folder.volumeName == folder.volumeName && it.folder.relativePath.startsWith(folder.relativePath)
        }
        if (inside.isEmpty()) return null
        return MediaFolder(
            ref = folder,
            songCount = inside.size,
            latestDateModified = inside.maxOf { it.dateModified },
            totalSize = inside.sumOf { it.size },
            totalDurationMs = inside.sumOf { it.durationMs },
        )
    }
}
