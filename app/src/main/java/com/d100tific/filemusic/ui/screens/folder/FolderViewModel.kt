package com.d100tific.filemusic.ui.screens.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d100tific.filemusic.data.media.FolderTree
import com.d100tific.filemusic.data.media.LibraryRepository
import com.d100tific.filemusic.data.media.LibraryState
import com.d100tific.filemusic.data.model.FolderRef
import com.d100tific.filemusic.data.model.MediaFolder
import com.d100tific.filemusic.data.model.QueueKind
import com.d100tific.filemusic.data.model.QueueSource
import com.d100tific.filemusic.data.model.Song
import com.d100tific.filemusic.data.model.SortSpec
import com.d100tific.filemusic.data.playlists.PlaylistRepository
import com.d100tific.filemusic.data.settings.SettingsRepository
import com.d100tific.filemusic.data.settings.SortScope
import com.d100tific.filemusic.playback.PlayerConnection
import com.d100tific.filemusic.ui.navigation.FILES_ROOT_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FolderUiState(
    val title: String = "",
    val folderRef: FolderRef? = null,
    val isRoot: Boolean = true,
    val subfolders: List<MediaFolder> = emptyList(),
    val songs: List<Song> = emptyList(),
    /** Keys of every song that is in at least one playlist. */
    val songsInPlaylists: Set<String> = emptySet(),
    val sort: SortSpec = SortSpec.DEFAULT,
    val isLoading: Boolean = true,
)

class FolderViewModel(
    private val folderId: String,
    private val library: LibraryRepository,
    playlistRepo: PlaylistRepository,
    private val settingsRepo: SettingsRepository,
    private val player: PlayerConnection,
) : ViewModel() {

    val state: StateFlow<FolderUiState> = combine(
        library.state,
        settingsRepo.sortSpec(SortScope.FILES),
        playlistRepo.membership.map { it.keys },
    ) { libraryState, sort, inPlaylists -> build(libraryState, sort, inPlaylists) }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            // Right title and back arrow from the very first frame, before the library is read.
            if (folderId == FILES_ROOT_ID) {
                FolderUiState()
            } else {
                FolderUiState(title = FolderRef.fromId(folderId).name, isRoot = false)
            },
        )

    fun setSort(sort: SortSpec) {
        viewModelScope.launch { settingsRepo.setSortSpec(SortScope.FILES, sort) }
    }

    /** Plays this folder's songs (in the order currently shown), starting at [index]. */
    fun play(index: Int) {
        val current = state.value
        val ref = current.folderRef ?: return
        player.play(
            songs = current.songs,
            startIndex = index,
            queueSource = QueueSource(QueueKind.FOLDER, ref.id, ref.name, current.sort),
        )
    }

    private fun build(libraryState: LibraryState, sort: SortSpec, inPlaylists: Set<String>): FolderUiState {
        if (libraryState.isLoading) {
            return if (folderId == FILES_ROOT_ID) {
                FolderUiState(sort = sort)
            } else {
                FolderUiState(title = FolderRef.fromId(folderId).name, isRoot = false, sort = sort)
            }
        }
        val songs = libraryState.songs

        if (folderId != FILES_ROOT_ID) {
            val ref = FolderRef.fromId(folderId)
            val listing = FolderTree.listFolder(songs, ref, sort)
            return FolderUiState(
                title = ref.name,
                folderRef = ref,
                isRoot = false,
                subfolders = listing.subfolders,
                songs = listing.songs,
                songsInPlaylists = inPlaylists,
                sort = sort,
                isLoading = false,
            )
        }

        val volumes = library.listVolumes()
        if (volumes.size <= 1) {
            // Only one storage: skip the "pick a storage" step and show its contents directly.
            val ref = volumes.firstOrNull()?.let { FolderRef.root(it.name, it.type) }
                ?: return FolderUiState(sort = sort, isLoading = false)
            val listing = FolderTree.listFolder(songs, ref, sort)
            return FolderUiState(
                folderRef = ref,
                isRoot = true,
                subfolders = listing.subfolders,
                songs = listing.songs,
                songsInPlaylists = inPlaylists,
                sort = sort,
                isLoading = false,
            )
        }

        val volumeFolders = volumes.map { volume ->
            val ref = FolderRef.root(volume.name, volume.type)
            FolderTree.folderMeta(songs, ref) ?: MediaFolder(ref, 0, 0L, 0L, 0L)
        }
        return FolderUiState(
            isRoot = true,
            subfolders = volumeFolders,
            songsInPlaylists = inPlaylists,
            sort = sort,
            isLoading = false,
        )
    }
}
