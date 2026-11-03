package com.d100tific.filemusic.data.settings

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.d100tific.filemusic.data.model.QueueKind
import com.d100tific.filemusic.data.model.QueueSource
import com.d100tific.filemusic.data.model.SortField
import com.d100tific.filemusic.data.model.SortOrder
import com.d100tific.filemusic.data.model.SortSpec
import com.d100tific.filemusic.ui.theme.DefaultAccent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Every list that remembers its own sort order, independently of the others. */
enum class SortScope { PLAYLISTS, FILES, PLAYLIST_SONGS }

enum class WaveformStyle { COSINE, OSCILLOSCOPE }

/** What was playing when the app was last used, so the first tab can show it again. */
data class SavedSession(
    val source: QueueSource?,
    val songKey: String,
    val positionMs: Long,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val ACCENT_COLOR = intPreferencesKey("accent_color")
        val WAVEFORM = stringPreferencesKey("waveform_style")
        val SHUFFLE = booleanPreferencesKey("shuffle")
        val REPEAT = intPreferencesKey("repeat_mode")

        val SESSION_KIND = stringPreferencesKey("session_kind")
        val SESSION_ID = stringPreferencesKey("session_id")
        val SESSION_NAME = stringPreferencesKey("session_name")
        val SESSION_SORT_FIELD = stringPreferencesKey("session_sort_field")
        val SESSION_SORT_ORDER = stringPreferencesKey("session_sort_order")
        val SESSION_SONG = stringPreferencesKey("session_song")
        val SESSION_POSITION = longPreferencesKey("session_position")

        fun sortField(scope: SortScope) = stringPreferencesKey("sort_field_${scope.name}")
        fun sortOrder(scope: SortScope) = stringPreferencesKey("sort_order_${scope.name}")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: false }

    val accentColorArgb: Flow<Int> = context.dataStore.data.map { it[Keys.ACCENT_COLOR] ?: DefaultAccent.toArgb() }

    val waveformStyle: Flow<WaveformStyle> = context.dataStore.data.map { prefs ->
        prefs[Keys.WAVEFORM]?.let { runCatching { WaveformStyle.valueOf(it) }.getOrNull() } ?: WaveformStyle.COSINE
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setAccentColor(argb: Int) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = argb }
    }

    suspend fun setWaveformStyle(style: WaveformStyle) {
        context.dataStore.edit { it[Keys.WAVEFORM] = style.name }
    }

    // --- Sorting ------------------------------------------------------------------------------

    fun sortSpec(scope: SortScope): Flow<SortSpec> = context.dataStore.data.map { prefs ->
        val default = defaultSort(scope)
        val field = prefs[Keys.sortField(scope)]?.let { runCatching { SortField.valueOf(it) }.getOrNull() }
            ?: default.field
        val order = prefs[Keys.sortOrder(scope)]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
            ?: default.order
        SortSpec(field, order)
    }

    suspend fun setSortSpec(scope: SortScope, sort: SortSpec) {
        context.dataStore.edit {
            it[Keys.sortField(scope)] = sort.field.name
            it[Keys.sortOrder(scope)] = sort.order.name
        }
    }

    suspend fun currentSortSpec(scope: SortScope): SortSpec = sortSpec(scope).first()

    // --- Player preferences -------------------------------------------------------------------

    suspend fun loadPlayerModes(): Pair<Boolean, Int> {
        val prefs = context.dataStore.data.first()
        return (prefs[Keys.SHUFFLE] ?: false) to (prefs[Keys.REPEAT] ?: 0)
    }

    suspend fun savePlayerModes(shuffle: Boolean, repeatMode: Int) {
        context.dataStore.edit {
            it[Keys.SHUFFLE] = shuffle
            it[Keys.REPEAT] = repeatMode
        }
    }

    // --- Last played session ------------------------------------------------------------------

    suspend fun saveSession(source: QueueSource?, songKey: String, positionMs: Long) {
        context.dataStore.edit {
            it[Keys.SESSION_SONG] = songKey
            it[Keys.SESSION_POSITION] = positionMs
            if (source != null) {
                it[Keys.SESSION_KIND] = source.kind.name
                it[Keys.SESSION_ID] = source.id
                it[Keys.SESSION_NAME] = source.name
                it[Keys.SESSION_SORT_FIELD] = source.sort.field.name
                it[Keys.SESSION_SORT_ORDER] = source.sort.order.name
            } else {
                it.remove(Keys.SESSION_KIND)
            }
        }
    }

    suspend fun loadSession(): SavedSession? {
        val prefs = context.dataStore.data.first()
        val songKey = prefs[Keys.SESSION_SONG] ?: return null
        val kind = prefs[Keys.SESSION_KIND]?.let { runCatching { QueueKind.valueOf(it) }.getOrNull() }
        val source = if (kind == null) null else QueueSource(
            kind = kind,
            id = prefs[Keys.SESSION_ID] ?: "",
            name = prefs[Keys.SESSION_NAME] ?: "",
            sort = SortSpec(
                prefs[Keys.SESSION_SORT_FIELD]?.let { runCatching { SortField.valueOf(it) }.getOrNull() }
                    ?: SortField.NAME,
                prefs[Keys.SESSION_SORT_ORDER]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
                    ?: SortOrder.ASCENDING,
            ),
        )
        return SavedSession(source, songKey, prefs[Keys.SESSION_POSITION] ?: 0L)
    }

    companion object {
        fun defaultSort(scope: SortScope): SortSpec = when (scope) {
            SortScope.PLAYLIST_SONGS -> SortSpec(SortField.ADDED, SortOrder.ASCENDING)
            else -> SortSpec.DEFAULT
        }
    }
}
