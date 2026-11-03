package com.d100tific.filemusic.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d100tific.filemusic.data.media.LibraryRepository
import com.d100tific.filemusic.data.model.QueueKind
import com.d100tific.filemusic.data.model.QueueSource
import com.d100tific.filemusic.data.model.Song
import com.d100tific.filemusic.data.model.SortSpec
import com.d100tific.filemusic.data.model.sortedBySpec
import com.d100tific.filemusic.data.playlists.PlaylistRepository
import com.d100tific.filemusic.data.settings.SettingsRepository
import com.d100tific.filemusic.data.settings.SortScope
import com.d100tific.filemusic.playback.PlayerConnection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaylistDetailUiState(
    val name: String = "",
    val songs: List<Song> = emptyList(),
    val sort: SortSpec = SettingsRepository.defaultSort(SortScope.PLAYLIST_SONGS),
    val isLoading: Boolean = true,
    /** The playlist no longer exists (it was deleted). */
    val missing: Boolean = false,
)

class PlaylistDetailViewModel(
    private val playlistId: Long,
    library: LibraryRepository,
    private val playlistRepo: PlaylistRepository,
    private val settingsRepo: SettingsRepository,
    private val player: PlayerConnection,
) : ViewModel() {

    val state: StateFlow<PlaylistDetailUiState> = combine(
        playlistRepo.playlists,
        library.state,
        settingsRepo.sortSpec(SortScope.PLAYLIST_SONGS),
    ) { playlists, libraryState, sort ->
        val playlist = playlists.firstOrNull { it.id == playlistId }
        when {
            playlist == null -> PlaylistDetailUiState(sort = sort, isLoading = false, missing = true)
            libraryState.isLoading -> PlaylistDetailUiState(name = playlist.name, sort = sort)
            else -> {
                val addedAt = playlist.entries.associate { it.songKey to it.addedAt }
                PlaylistDetailUiState(
                    name = playlist.name,
                    songs = playlist.entries.mapNotNull { libraryState.byKey[it.songKey] }
                        .sortedBySpec(sort, addedAt),
                    sort = sort,
                    isLoading = false,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistDetailUiState())

    fun setSort(sort: SortSpec) {
        viewModelScope.launch { settingsRepo.setSortSpec(SortScope.PLAYLIST_SONGS, sort) }
    }

    fun removeSong(song: Song) {
        viewModelScope.launch { playlistRepo.removeSong(playlistId, song.key) }
    }

    /** Plays the playlist in the order currently shown, starting at [index]. */
    fun play(index: Int) {
        val current = state.value
        player.play(
            songs = current.songs,
            startIndex = index,
            queueSource = QueueSource(QueueKind.PLAYLIST, playlistId.toString(), current.name, current.sort),
        )
    }
}
