package com.d100tific.filemusic.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object StoragePermission {
    /**
     * The only permissions this app ever asks for: read access to audio files (plus, on Android 9
     * and older only, write access so a song can be deleted). Nothing else.
     */
    val permissionNames: Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun isGranted(context: Context): Boolean =
        permissionNames.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
}
