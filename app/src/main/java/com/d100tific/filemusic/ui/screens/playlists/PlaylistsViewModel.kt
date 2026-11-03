package com.d100tific.filemusic.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d100tific.filemusic.data.media.LibraryRepository
import com.d100tific.filemusic.data.model.Playlist
import com.d100tific.filemusic.data.model.SortSpec
import com.d100tific.filemusic.data.model.sortedPlaylistsBySpec
import com.d100tific.filemusic.data.playlists.PlaylistRepository
import com.d100tific.filemusic.data.settings.SettingsRepository
import com.d100tific.filemusic.data.settings.SortScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaylistItem(val playlist: Playlist, val songCount: Int)

data class PlaylistsUiState(
    val items: List<PlaylistItem> = emptyList(),
    val sort: SortSpec = SortSpec.DEFAULT,
)

class PlaylistsViewModel(
    library: LibraryRepository,
    private val playlistRepo: PlaylistRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<PlaylistsUiState> = combine(
        playlistRepo.playlists,
        library.state,
        settingsRepo.sortSpec(SortScope.PLAYLISTS),
    ) { playlists, libraryState, sort ->
        // Count only songs that still exist, so a playlist never promises more than it can play.
        val counts = playlists.associate { playlist ->
            playlist.id to if (libraryState.isLoading) {
                playlist.entries.size
            } else {
                playlist.entries.count { it.songKey in libraryState.byKey }
            }
        }
        val sorted = playlists.sortedPlaylistsBySpec(sort) { counts[it.id] ?: 0 }
        PlaylistsUiState(sorted.map { PlaylistItem(it, counts[it.id] ?: 0) }, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistsUiState())

    fun setSort(sort: SortSpec) {
        viewModelScope.launch { settingsRepo.setSortSpec(SortScope.PLAYLISTS, sort) }
    }

    fun create(name: String) {
        viewModelScope.launch { playlistRepo.create(name) }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { playlistRepo.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { playlistRepo.delete(id) }
    }
}
