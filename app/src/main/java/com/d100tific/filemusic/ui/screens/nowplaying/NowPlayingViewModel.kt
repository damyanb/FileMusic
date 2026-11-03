package com.d100tific.filemusic.ui.screens.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d100tific.filemusic.data.media.LibraryRepository
import com.d100tific.filemusic.data.model.Song
import com.d100tific.filemusic.data.playlists.PlaylistRepository
import com.d100tific.filemusic.data.settings.SettingsRepository
import com.d100tific.filemusic.data.settings.WaveformStyle
import com.d100tific.filemusic.playback.PlayerConnection
import com.d100tific.filemusic.playback.PlayerUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class NowPlayingUiState(
    val player: PlayerUiState = PlayerUiState(),
    /** The library entry of the loaded song; null if it is no longer on the device. */
    val song: Song? = null,
    val inAnyPlaylist: Boolean = false,
    val waveform: WaveformStyle = WaveformStyle.COSINE,
)

class NowPlayingViewModel(
    private val player: PlayerConnection,
    library: LibraryRepository,
    playlistRepo: PlaylistRepository,
    settingsRepo: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<NowPlayingUiState> = combine(
        player.state,
        library.state,
        playlistRepo.membership,
        settingsRepo.waveformStyle,
    ) { playerState, libraryState, membership, waveform ->
        val key = playerState.songKey
        NowPlayingUiState(
            player = playerState,
            song = key?.let { libraryState.byKey[it] },
            inAnyPlaylist = key != null && membership[key].orEmpty().isNotEmpty(),
            waveform = waveform,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NowPlayingUiState())

    fun togglePlayPause() = player.togglePlayPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeat() = player.cycleRepeat()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun positionMs(): Long = player.currentPositionMs()
}
