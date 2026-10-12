package com.auraplayer.data.local.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import com.auraplayer.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * High-performance, zero-annotation-processing SQLite database for AuraPlayer.
 * Completely immune to JDK/KSP version incompatibilities while providing
 * reactive Flow-based updates and atomic batch transactions.
 */
class AuraDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "aura_music.db", null, 1) {

    private val changeNotifier = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply { tryEmit(Unit) }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS songs (
                id INTEGER PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT NOT NULL,
                album_id INTEGER NOT NULL,
                duration INTEGER NOT NULL,
                uri TEXT NOT NULL,
                artwork_uri TEXT,
                track_number INTEGER NOT NULL,
                year INTEGER NOT NULL,
                date_added INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_song_title ON songs(title)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_song_artist ON songs(artist)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS songs")
        onCreate(db)
    }

    fun getAllSongs(): Flow<List<Song>> = flow {
        changeNotifier.collect {
            emit(querySongs(null, null, "title COLLATE NOCASE ASC"))
        }
    }.flowOn(Dispatchers.IO)

    fun searchSongs(query: String): Flow<List<Song>> = flow {
        changeNotifier.collect {
            val q = "%$query%"
            emit(querySongs("title LIKE ? OR artist LIKE ?", arrayOf(q, q), "title COLLATE NOCASE ASC"))
        }
    }.flowOn(Dispatchers.IO)

    fun getSongsByAlbum(album: String): Flow<List<Song>> = flow {
        changeNotifier.collect {
            emit(querySongs("album = ?", arrayOf(album), "track_number ASC"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun upsertAll(songs: List<Song>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (song in songs) {
                val cv = ContentValues().apply {
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("album", song.album)
                    put("album_id", song.albumId)
                    put("duration", song.duration)
                    put("uri", song.uri.toString())
                    put("artwork_uri", song.artworkUri?.toString())
                    put("track_number", song.trackNumber)
                    put("year", song.year)
                    put("date_added", song.dateAdded)
                }
                db.insertWithOnConflict(
                    "songs",
                    null,
                    cv,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        changeNotifier.tryEmit(Unit)
    }

    suspend fun deleteStale(activeIds: List<Long>) = withContext(Dispatchers.IO) {
        if (activeIds.isEmpty()) return@withContext
        val db = writableDatabase
        val placeholders = activeIds.joinToString(",") { "?" }
        val args = activeIds.map { it.toString() }.toTypedArray()
        db.delete("songs", "id NOT IN ($placeholders) AND uri LIKE 'content://media/external/audio/%'", args)
        changeNotifier.tryEmit(Unit)
    }

    private fun querySongs(selection: String?, selectionArgs: Array<String>?, orderBy: String): List<Song> {
        val db = readableDatabase
        val list = mutableListOf<Song>()
        db.query(
            "songs",
            null,
            selection,
            selectionArgs,
            null,
            null,
            orderBy
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow("id")
            val titleCol = cursor.getColumnIndexOrThrow("title")
            val artistCol = cursor.getColumnIndexOrThrow("artist")
            val albumCol = cursor.getColumnIndexOrThrow("album")
            val albumIdCol = cursor.getColumnIndexOrThrow("album_id")
            val durCol = cursor.getColumnIndexOrThrow("duration")
            val uriCol = cursor.getColumnIndexOrThrow("uri")
            val artCol = cursor.getColumnIndexOrThrow("artwork_uri")
            val trackCol = cursor.getColumnIndexOrThrow("track_number")
            val yearCol = cursor.getColumnIndexOrThrow("year")
            val dateCol = cursor.getColumnIndexOrThrow("date_added")

            while (cursor.moveToNext()) {
                list.add(
                    Song(
                        id = cursor.getLong(idCol),
                        title = cursor.getString(titleCol),
                        artist = cursor.getString(artistCol),
                        album = cursor.getString(albumCol),
                        albumId = cursor.getLong(albumIdCol),
                        duration = cursor.getLong(durCol),
                        uri = Uri.parse(cursor.getString(uriCol)),
                        artworkUri = cursor.getString(artCol)?.let { Uri.parse(it) },
                        trackNumber = cursor.getInt(trackCol),
                        year = cursor.getInt(yearCol),
                        dateAdded = cursor.getLong(dateCol)
                    )
                )
            }
        }
        return list
    }
}
