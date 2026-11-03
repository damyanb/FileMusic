package com.d100tific.filemusic

import android.app.Application
import com.d100tific.filemusic.data.media.LibraryRepository
import com.d100tific.filemusic.data.media.SongDeleter
import com.d100tific.filemusic.data.playlists.PlaylistRepository
import com.d100tific.filemusic.data.settings.SettingsRepository
import com.d100tific.filemusic.playback.PlayerConnection

/** Simple manual dependency container; this app is small enough not to need a DI framework. */
class FileMusicApp : Application() {
    val libraryRepository by lazy { LibraryRepository(this) }
    val playlistRepository by lazy { PlaylistRepository(this) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val songDeleter by lazy { SongDeleter(this) }
    val playerConnection by lazy {
        PlayerConnection(this, libraryRepository, playlistRepository, settingsRepository)
    }
}
