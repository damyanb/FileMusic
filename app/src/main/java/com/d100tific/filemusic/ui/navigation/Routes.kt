package com.d100tific.filemusic.ui.navigation

import android.net.Uri

const val FILES_ROOT_ID = "root"

object Routes {
    const val NOW_PLAYING = "now_playing"
    const val PLAYLISTS = "playlists"
    const val SETTINGS = "settings"

    const val FOLDER_PATTERN = "folder/{folderId}"
    const val PLAYLIST_PATTERN = "playlist/{playlistId}"

    fun folder(folderId: String): String = "folder/${Uri.encode(folderId)}"

    fun playlist(playlistId: Long): String = "playlist/$playlistId"
}
