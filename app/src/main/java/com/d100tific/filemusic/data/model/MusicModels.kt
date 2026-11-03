package com.d100tific.filemusic.data.model

import android.net.Uri

enum class SortField { NAME, ARTIST, ALBUM, DATE, SIZE, DURATION, ADDED }
enum class SortOrder { ASCENDING, DESCENDING }

data class SortSpec(val field: SortField, val order: SortOrder) {
    companion object {
        val DEFAULT = SortSpec(SortField.NAME, SortOrder.ASCENDING)
    }
}

/** Which storage volume a folder/song lives on. There is no cloud storage in this app. */
enum class VolumeType { INTERNAL, SD_CARD }

data class StorageVolume(
    val name: String,
    val type: VolumeType,
    val rootPath: String,
)

/**
 * Identifies a folder uniquely across volumes. [relativePath] is always normalized with a
 * trailing "/", and is empty ("") for the root of a volume.
 */
data class FolderRef(
    val volumeName: String,
    val volumeType: VolumeType,
    val relativePath: String,
) {
    val id: String get() = "$volumeName|$relativePath"

    val name: String get() = relativePath.trimEnd('/').substringAfterLast('/')

    fun child(childName: String): FolderRef = FolderRef(volumeName, volumeType, "$relativePath$childName/")

    companion object {
        fun root(volumeName: String, volumeType: VolumeType) = FolderRef(volumeName, volumeType, "")

        fun fromId(id: String): FolderRef {
            val parts = id.split("|", limit = 2)
            val volumeName = parts[0]
            val relativePath = if (parts.size > 1) parts[1] else ""
            val type = if (volumeName.contains("primary", ignoreCase = true) || volumeName == "external") {
                VolumeType.INTERNAL
            } else {
                VolumeType.SD_CARD
            }
            return FolderRef(volumeName, type, relativePath)
        }
    }
}

/** One audio file. [artist] and [album] are empty strings when the file has no such tag. */
data class Song(
    val id: Long,
    val uri: Uri,
    /** Stable identifier (volume + path + file name) used by playlists and the saved session. */
    val key: String,
    val title: String,
    val artist: String,
    val album: String,
    val displayName: String,
    val durationMs: Long,
    val dateModified: Long,
    val size: Long,
    val folder: FolderRef,
)

data class MediaFolder(
    val ref: FolderRef,
    val songCount: Int,
    val latestDateModified: Long,
    val totalSize: Long,
    val totalDurationMs: Long,
)

data class FolderListing(
    val folder: FolderRef,
    val subfolders: List<MediaFolder>,
    val songs: List<Song>,
)

/** A user-made playlist. [entries] keeps the order in which songs were added. */
data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val entries: List<PlaylistEntry>,
)

data class PlaylistEntry(val songKey: String, val addedAt: Long)

/** Where the current play queue came from; decides what "next" and "previous" mean. */
enum class QueueKind { FOLDER, PLAYLIST, SINGLE }

data class QueueSource(
    val kind: QueueKind,
    /** Folder id for [QueueKind.FOLDER], playlist id for [QueueKind.PLAYLIST]. */
    val id: String,
    /** Human readable name (folder or playlist name) shown in the player. */
    val name: String,
    val sort: SortSpec,
)
