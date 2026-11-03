package com.d100tific.filemusic.data.playlists

import android.content.Context
import com.d100tific.filemusic.data.model.Playlist
import com.d100tific.filemusic.data.model.PlaylistEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * User playlists, stored as one small JSON file in the app's private storage. A playlist only
 * remembers song keys (see [com.d100tific.filemusic.data.model.Song.key]); the audio files
 * themselves are never copied or touched.
 */
class PlaylistRepository(context: Context) {

    private val file = File(context.filesDir, "playlists.json")
    private val writeLock = Mutex()

    private val _playlists = MutableStateFlow(readFromDisk())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    /** For each song key, the ids of the playlists it belongs to. */
    val membership: Flow<Map<String, Set<Long>>> = _playlists.map { lists ->
        val result = HashMap<String, MutableSet<Long>>()
        for (list in lists) for (entry in list.entries) {
            result.getOrPut(entry.songKey) { mutableSetOf() } += list.id
        }
        result
    }

    fun find(id: Long): Playlist? = _playlists.value.firstOrNull { it.id == id }

    /** Creates a playlist and optionally puts [initialSongKey] in it. Returns the new playlist id. */
    suspend fun create(name: String, initialSongKey: String? = null): Long {
        val now = System.currentTimeMillis()
        val id = (_playlists.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val entries = if (initialSongKey != null) listOf(PlaylistEntry(initialSongKey, now)) else emptyList()
        update { it + Playlist(id, name.trim(), now, entries) }
        return id
    }

    suspend fun rename(id: Long, name: String) {
        update { lists -> lists.map { if (it.id == id) it.copy(name = name.trim()) else it } }
    }

    suspend fun delete(id: Long) {
        update { lists -> lists.filterNot { it.id == id } }
    }

    suspend fun addSong(playlistId: Long, songKey: String) {
        update { lists ->
            lists.map { list ->
                if (list.id != playlistId || list.entries.any { it.songKey == songKey }) list
                else list.copy(entries = list.entries + PlaylistEntry(songKey, System.currentTimeMillis()))
            }
        }
    }

    suspend fun removeSong(playlistId: Long, songKey: String) {
        update { lists ->
            lists.map { list ->
                if (list.id != playlistId) list
                else list.copy(entries = list.entries.filterNot { it.songKey == songKey })
            }
        }
    }

    /** Forgets songs everywhere, used after their files have been deleted. */
    suspend fun removeSongsEverywhere(songKeys: Collection<String>) {
        if (songKeys.isEmpty()) return
        val keys = songKeys.toSet()
        update { lists -> lists.map { list -> list.copy(entries = list.entries.filterNot { it.songKey in keys }) } }
    }

    // ---------------------------------------------------------------------------------------

    private suspend fun update(transform: (List<Playlist>) -> List<Playlist>) {
        writeLock.withLock {
            val updated = transform(_playlists.value)
            _playlists.value = updated
            withContext(Dispatchers.IO) { writeToDisk(updated) }
        }
    }

    private fun readFromDisk(): List<Playlist> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val root = JSONArray(file.readText())
            (0 until root.length()).map { index ->
                val obj = root.getJSONObject(index)
                val songs = obj.getJSONArray("songs")
                Playlist(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    createdAt = obj.optLong("createdAt", 0L),
                    entries = (0 until songs.length()).map { i ->
                        val song = songs.getJSONObject(i)
                        PlaylistEntry(song.getString("key"), song.optLong("addedAt", 0L))
                    },
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun writeToDisk(lists: List<Playlist>) {
        val root = JSONArray()
        for (list in lists) {
            val songs = JSONArray()
            for (entry in list.entries) {
                songs.put(JSONObject().put("key", entry.songKey).put("addedAt", entry.addedAt))
            }
            root.put(
                JSONObject()
                    .put("id", list.id)
                    .put("name", list.name)
                    .put("createdAt", list.createdAt)
                    .put("songs", songs),
            )
        }
        // Write to a temp file first so a crash never leaves a half-written playlist file.
        val temp = File(file.parentFile, "playlists.json.tmp")
        temp.writeText(root.toString())
        if (!temp.renameTo(file)) {
            file.writeText(root.toString())
            temp.delete()
        }
    }
}
