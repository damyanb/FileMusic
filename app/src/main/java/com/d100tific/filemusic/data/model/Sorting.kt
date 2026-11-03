package com.d100tific.filemusic.data.model

/**
 * Sorts songs according to [sort]. [addedAt] maps a song key to the moment it was added to the
 * playlist being shown; it is only used for [SortField.ADDED].
 */
fun List<Song>.sortedBySpec(sort: SortSpec, addedAt: Map<String, Long> = emptyMap()): List<Song> {
    val byTitle = compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
    val primary: Comparator<Song> = when (sort.field) {
        SortField.NAME -> byTitle
        SortField.ARTIST -> compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }.then(byTitle)
        SortField.ALBUM -> compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.album }.then(byTitle)
        SortField.DATE -> compareBy<Song> { it.dateModified }.then(byTitle)
        SortField.SIZE -> compareBy<Song> { it.size }.then(byTitle)
        SortField.DURATION -> compareBy<Song> { it.durationMs }.then(byTitle)
        SortField.ADDED -> compareBy<Song> { addedAt[it.key] ?: it.dateModified }.then(byTitle)
    }
    val comparator = if (sort.order == SortOrder.DESCENDING) primary.reversed() else primary
    return sortedWith(comparator)
}

/** Folders can only be ordered by what they have in common; other fields fall back to the name. */
fun List<MediaFolder>.sortedFoldersBySpec(sort: SortSpec): List<MediaFolder> {
    val byName = compareBy<MediaFolder, String>(String.CASE_INSENSITIVE_ORDER) { it.ref.name }
    val primary: Comparator<MediaFolder> = when (sort.field) {
        SortField.DATE -> compareBy<MediaFolder> { it.latestDateModified }.then(byName)
        SortField.SIZE -> compareBy<MediaFolder> { it.totalSize }.then(byName)
        SortField.DURATION -> compareBy<MediaFolder> { it.totalDurationMs }.then(byName)
        else -> byName
    }
    val comparator = if (sort.order == SortOrder.DESCENDING) primary.reversed() else primary
    return sortedWith(comparator)
}

fun List<Playlist>.sortedPlaylistsBySpec(sort: SortSpec, countOf: (Playlist) -> Int): List<Playlist> {
    val byName = compareBy<Playlist, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
    val primary: Comparator<Playlist> = when (sort.field) {
        SortField.DATE, SortField.ADDED -> compareBy<Playlist> { it.createdAt }.then(byName)
        SortField.SIZE, SortField.DURATION -> compareBy<Playlist> { countOf(it) }.then(byName)
        else -> byName
    }
    val comparator = if (sort.order == SortOrder.DESCENDING) primary.reversed() else primary
    return sortedWith(comparator)
}
