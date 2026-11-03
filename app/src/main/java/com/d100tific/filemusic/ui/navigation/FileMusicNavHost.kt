package com.d100tific.filemusic.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.d100tific.filemusic.FileMusicApp
import com.d100tific.filemusic.R
import com.d100tific.filemusic.ui.components.Win2kBottomNav
import com.d100tific.filemusic.ui.components.Win2kNavItem
import com.d100tific.filemusic.ui.screens.folder.FolderScreen
import com.d100tific.filemusic.ui.screens.folder.FolderViewModel
import com.d100tific.filemusic.ui.screens.nowplaying.NowPlayingScreen
import com.d100tific.filemusic.ui.screens.nowplaying.NowPlayingViewModel
import com.d100tific.filemusic.ui.screens.playlists.PlaylistDetailScreen
import com.d100tific.filemusic.ui.screens.playlists.PlaylistDetailViewModel
import com.d100tific.filemusic.ui.screens.playlists.PlaylistsScreen
import com.d100tific.filemusic.ui.screens.playlists.PlaylistsViewModel
import com.d100tific.filemusic.ui.screens.settings.SettingsScreen
import com.d100tific.filemusic.ui.screens.settings.SettingsViewModel

private val filesRootRoute = Routes.folder(FILES_ROOT_ID)

@Composable
fun FileMusicNavHost(app: FileMusicApp, onLanguageChanged: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destinationRoute = backStackEntry?.destination?.route
    val isNowPlayingTab = destinationRoute == Routes.NOW_PLAYING
    val isPlaylistsTab = destinationRoute == Routes.PLAYLISTS
    val isFilesRootTab = destinationRoute == Routes.FOLDER_PATTERN && isFilesRootEntry(backStackEntry?.arguments)
    val showBottomBar = isNowPlayingTab || isPlaylistsTab || isFilesRootTab

    // Lists highlight the song that is playing.
    val playerState by app.playerConnection.state.collectAsState()
    val currentSongKey = playerState.songKey

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val selectedIndex = when {
                    isNowPlayingTab -> 0
                    isPlaylistsTab -> 1
                    else -> 2
                }
                Win2kBottomNav(
                    items = listOf(
                        Win2kNavItem(stringResource(R.string.tab_now_playing), Icons.Filled.PlayArrow),
                        Win2kNavItem(stringResource(R.string.tab_playlists), Icons.AutoMirrored.Filled.QueueMusic),
                        Win2kNavItem(stringResource(R.string.tab_files), Icons.Filled.Folder),
                    ),
                    selectedIndex = selectedIndex,
                    onSelect = { index -> navController.switchToTab(index) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.NOW_PLAYING,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.NOW_PLAYING) {
                val vm: NowPlayingViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        NowPlayingViewModel(
                            app.playerConnection,
                            app.libraryRepository,
                            app.playlistRepository,
                            app.settingsRepository,
                        )
                    }
                })
                NowPlayingScreen(
                    viewModel = vm,
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.PLAYLISTS) {
                val vm: PlaylistsViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        PlaylistsViewModel(app.libraryRepository, app.playlistRepository, app.settingsRepository)
                    }
                })
                PlaylistsScreen(
                    viewModel = vm,
                    onOpenPlaylist = { id -> navController.navigate(Routes.playlist(id)) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(
                route = Routes.FOLDER_PATTERN,
                arguments = listOf(navArgument("folderId") { type = NavType.StringType }),
            ) { entry ->
                val folderId = Uri.decode(entry.arguments?.getString("folderId") ?: FILES_ROOT_ID)
                val vm: FolderViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        FolderViewModel(
                            folderId,
                            app.libraryRepository,
                            app.playlistRepository,
                            app.settingsRepository,
                            app.playerConnection,
                        )
                    }
                })
                FolderScreen(
                    viewModel = vm,
                    currentSongKey = currentSongKey,
                    onBack = { navController.popBackStack() },
                    onOpenFolder = { childId -> navController.navigate(Routes.folder(childId)) },
                    onSongPlayed = { navController.switchToTab(0) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(
                route = Routes.PLAYLIST_PATTERN,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
            ) { entry ->
                val playlistId = entry.arguments?.getLong("playlistId") ?: 0L
                val vm: PlaylistDetailViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        PlaylistDetailViewModel(
                            playlistId,
                            app.libraryRepository,
                            app.playlistRepository,
                            app.settingsRepository,
                            app.playerConnection,
                        )
                    }
                })
                PlaylistDetailScreen(
                    viewModel = vm,
                    currentSongKey = currentSongKey,
                    onBack = { navController.popBackStack() },
                    onSongPlayed = { navController.switchToTab(0) },
                )
            }

            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = viewModelFactory {
                    initializer { SettingsViewModel(app.settingsRepository) }
                })
                SettingsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onLanguageChanged = onLanguageChanged,
                )
            }
        }
    }
}

/** Switches between the three main tabs, remembering where each one was left. */
private fun NavHostController.switchToTab(index: Int) {
    val target = when (index) {
        0 -> Routes.NOW_PLAYING
        1 -> Routes.PLAYLISTS
        else -> filesRootRoute
    }
    navigate(target) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun isFilesRootEntry(arguments: android.os.Bundle?): Boolean {
    val folderId = arguments?.getString("folderId")
    return Uri.decode(folderId ?: "") == FILES_ROOT_ID
}
