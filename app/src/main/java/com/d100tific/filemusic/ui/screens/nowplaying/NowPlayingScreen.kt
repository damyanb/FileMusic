package com.d100tific.filemusic.ui.screens.nowplaying

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.d100tific.filemusic.R
import com.d100tific.filemusic.data.model.FolderRef
import com.d100tific.filemusic.data.model.QueueKind
import com.d100tific.filemusic.data.model.QueueSource
import com.d100tific.filemusic.data.model.VolumeType
import com.d100tific.filemusic.playback.PlayerUiState
import com.d100tific.filemusic.ui.components.WaveformView
import com.d100tific.filemusic.ui.components.Win2kIconButton
import com.d100tific.filemusic.ui.components.Win2kTitleBar
import com.d100tific.filemusic.ui.components.Win2kToggleIconButton
import com.d100tific.filemusic.ui.components.formatTime
import com.d100tific.filemusic.ui.songactions.LocalSongActions
import com.d100tific.filemusic.ui.theme.LocalWinColors
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel,
    onSettingsClick: () -> Unit,
) {
    val colors = LocalWinColors.current
    val songActions = LocalSongActions.current
    val state by viewModel.state.collectAsState()
    val player = state.player

    var position by remember { mutableLongStateOf(0L) }
    var dragValue by remember { mutableStateOf<Float?>(null) }

    // Poll the playback position while playing; when paused it is read once and left alone.
    LaunchedEffect(player.songKey, player.isPlaying, player.connected) {
        while (true) {
            position = viewModel.positionMs()
            if (!player.isPlaying) break
            delay(250)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.page)) {
        Win2kTitleBar(title = stringResource(R.string.tab_now_playing)) {
            Win2kIconButton(
                icon = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.action_settings),
                onClick = onSettingsClick,
            )
        }

        if (!player.hasSong) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.empty_now_playing),
                    color = colors.disabled,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }

        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        val waveform: @Composable (Modifier) -> Unit = { modifier ->
            WaveformView(isPlaying = player.isPlaying, style = state.waveform, modifier = modifier)
        }
        val details: @Composable () -> Unit = {
            SongDetails(player = player)
            SeekBar(
                position = dragValue?.toLong() ?: position,
                duration = player.durationMs,
                onDrag = { dragValue = it },
                onDragFinished = {
                    dragValue?.let {
                        viewModel.seekTo(it.toLong())
                        position = it.toLong()
                    }
                    dragValue = null
                },
            )
            TransportControls(
                player = player,
                onShuffle = viewModel::toggleShuffle,
                onPrevious = viewModel::previous,
                onPlayPause = viewModel::togglePlayPause,
                onNext = viewModel::next,
                onRepeat = viewModel::cycleRepeat,
            )
        }
        val songActionBar: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.controlFace)
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Win2kToggleIconButton(
                    icon = if (state.inAnyPlaylist) Icons.AutoMirrored.Filled.PlaylistAddCheck else Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = stringResource(R.string.action_manage_playlists),
                    active = state.inAnyPlaylist,
                    enabled = state.song != null,
                    onClick = { state.song?.let(songActions::managePlaylists) },
                )
                Win2kIconButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    enabled = state.song != null,
                    onClick = { state.song?.let { songActions.confirmDelete(listOf(it)) } },
                )
            }
        }

        if (landscape) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                waveform(Modifier.weight(1f).fillMaxSize())
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize().padding(start = 12.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    details()
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                waveform(Modifier.weight(1f).fillMaxWidth())
                Column(modifier = Modifier.padding(top = 12.dp)) { details() }
            }
        }
        songActionBar()
    }
}

@Composable
private fun SongDetails(player: PlayerUiState) {
    val colors = LocalWinColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = playbackSourceLabel(player.source) ?: "",
            color = colors.accent,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = player.title,
            color = colors.onPage,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = player.artist.ifEmpty { stringResource(R.string.unknown_artist) },
            color = if (player.artist.isEmpty()) colors.disabled else colors.onPage,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = player.album.ifEmpty { stringResource(R.string.unknown_album) },
            color = colors.disabled,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun playbackSourceLabel(source: QueueSource?): String? {
    if (source == null) return null
    return when (source.kind) {
        QueueKind.PLAYLIST -> stringResource(R.string.now_playing_from_playlist, source.name)
        QueueKind.FOLDER -> {
            val name = source.name.ifEmpty {
                if (FolderRef.fromId(source.id).volumeType == VolumeType.INTERNAL) {
                    stringResource(R.string.volume_internal)
                } else {
                    stringResource(R.string.volume_sdcard)
                }
            }
            stringResource(R.string.now_playing_from_folder, name)
        }
        QueueKind.SINGLE -> null
    }
}

@Composable
private fun SeekBar(
    position: Long,
    duration: Long,
    onDrag: (Float) -> Unit,
    onDragFinished: () -> Unit,
) {
    val colors = LocalWinColors.current
    val total = max(duration, 1L).toFloat()
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Slider(
            value = position.toFloat().coerceIn(0f, total),
            onValueChange = onDrag,
            onValueChangeFinished = onDragFinished,
            valueRange = 0f..total,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.controlFace,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position), color = colors.disabled, style = MaterialTheme.typography.bodySmall)
            Text(formatTime(duration), color = colors.disabled, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TransportControls(
    player: PlayerUiState,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Win2kToggleIconButton(
            icon = Icons.Filled.Shuffle,
            contentDescription = stringResource(R.string.action_shuffle),
            active = player.shuffle,
            onClick = onShuffle,
            size = 48.dp,
        )
        Win2kIconButton(
            icon = Icons.Filled.SkipPrevious,
            contentDescription = stringResource(R.string.action_previous),
            onClick = onPrevious,
            size = 56.dp,
        )
        Win2kIconButton(
            icon = if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = stringResource(if (player.isPlaying) R.string.action_pause else R.string.action_play),
            onClick = onPlayPause,
            size = 72.dp,
        )
        Win2kIconButton(
            icon = Icons.Filled.SkipNext,
            contentDescription = stringResource(R.string.action_next),
            onClick = onNext,
            enabled = player.hasNext,
            size = 56.dp,
        )
        Win2kToggleIconButton(
            icon = if (player.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            contentDescription = stringResource(
                when (player.repeatMode) {
                    Player.REPEAT_MODE_ALL -> R.string.repeat_all
                    Player.REPEAT_MODE_ONE -> R.string.repeat_one
                    else -> R.string.repeat_off
                },
            ),
            active = player.repeatMode != Player.REPEAT_MODE_OFF,
            onClick = onRepeat,
            size = 48.dp,
        )
    }
}
