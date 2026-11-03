package com.d100tific.filemusic.data.media

import android.content.Context
import androidx.core.content.ContextCompat

/** Shared helpers for deriving folder-relative paths on API < 29 devices, where MediaStore
 * has no RELATIVE_PATH column and everything must be worked out from the absolute file path. */
object StorageVolumes {

    fun legacyVolumeRoots(context: Context): List<String> =
        ContextCompat.getExternalFilesDirs(context, null)
            .filterNotNull()
            .mapNotNull { dir ->
                val marker = "/Android/data/"
                val path = dir.absolutePath
                val idx = path.indexOf(marker)
                if (idx >= 0) path.substring(0, idx) else null
            }

    fun legacyRelativeFolder(context: Context, absolutePath: String): String {
        val roots = legacyVolumeRoots(context)
        for (root in roots) {
            if (absolutePath.startsWith(root)) {
                val remainder = absolutePath.removePrefix(root).trimStart('/')
                val folder = remainder.substringBeforeLast('/', missingDelimiterValue = "")
                return if (folder.isEmpty()) "" else "$folder/"
            }
        }
        val folder = absolutePath.substringBeforeLast('/', missingDelimiterValue = "")
        return if (folder.isEmpty()) "" else folder.trimStart('/') + "/"
    }

    /** Builds an absolute path for saving a new file into [relativeFolder] on the primary volume. */
    fun legacyAbsolutePathFor(context: Context, relativeFolder: String, fileName: String): String {
        val root = legacyVolumeRoots(context).firstOrNull()
            ?: android.os.Environment.getExternalStorageDirectory().absolutePath
        return "$root/$relativeFolder$fileName"
    }
}
