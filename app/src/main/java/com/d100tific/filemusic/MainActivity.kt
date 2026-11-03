package com.d100tific.filemusic

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.d100tific.filemusic.core.locale.LocaleManager
import com.d100tific.filemusic.core.permissions.StoragePermission
import com.d100tific.filemusic.ui.navigation.FileMusicNavHost
import com.d100tific.filemusic.ui.screens.permission.PermissionScreen
import com.d100tific.filemusic.ui.songactions.LocalSongActions
import com.d100tific.filemusic.ui.songactions.SongActionsHost
import com.d100tific.filemusic.ui.songactions.rememberSongActions
import com.d100tific.filemusic.ui.theme.DefaultAccent
import com.d100tific.filemusic.ui.theme.FileMusicTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FileMusicApp

        setContent {
            val isDarkMode by app.settingsRepository.isDarkMode.collectAsState(initial = false)
            val accentArgb by app.settingsRepository.accentColorArgb.collectAsState(initial = DefaultAccent.toArgb())

            FileMusicTheme(
                darkMode = isDarkMode,
                accentColor = Color(accentArgb),
            ) {
                PermissionGatedContent(app = app, onLanguageChanged = { recreate() })
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    @Composable
    private fun PermissionGatedContent(app: FileMusicApp, onLanguageChanged: () -> Unit) {
        val context = LocalContext.current
        var hasPermission by remember { mutableStateOf(StoragePermission.isGranted(context)) }
        var wasDenied by remember { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            val granted = StoragePermission.isGranted(context)
            hasPermission = granted
            if (!granted) wasDenied = true
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasPermission = StoragePermission.isGranted(context)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        if (hasPermission) {
            // Nothing can be listed or played before the permission exists, so start then.
            LaunchedEffect(Unit) {
                app.libraryRepository.start()
                app.playerConnection.start()
            }
            val songActions = rememberSongActions(app)
            CompositionLocalProvider(LocalSongActions provides songActions) {
                FileMusicNavHost(app = app, onLanguageChanged = onLanguageChanged)
                SongActionsHost(app = app, actions = songActions)
            }
        } else {
            PermissionScreen(
                wasDenied = wasDenied,
                onRequestPermission = { permissionLauncher.launch(StoragePermission.permissionNames) },
                onOpenSettings = { openAppSettings() },
            )
        }
    }
}
