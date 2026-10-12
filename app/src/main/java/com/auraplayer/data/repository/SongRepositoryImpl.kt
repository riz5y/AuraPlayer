package com.auraplayer.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.auraplayer.data.local.db.AuraDatabaseHelper
import com.auraplayer.domain.model.Song
import com.auraplayer.domain.repository.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.provider.OpenableColumns

class SongRepositoryImpl(
    private val context: Context,
    private val dbHelper: AuraDatabaseHelper
) : SongRepository {

    override fun getAllSongs(): Flow<List<Song>> = dbHelper.getAllSongs()

    override fun search(query: String): Flow<List<Song>> = dbHelper.searchSongs(query)

    override fun getSongsByAlbum(album: String): Flow<List<Song>> = dbHelper.getSongsByAlbum(album)

    override suspend fun syncWithMediaStore() = withContext(Dispatchers.IO) {
        val songs = scanMediaStore()
        dbHelper.upsertAll(songs)
        dbHelper.deleteStale(songs.map { it.id })
    }

    override suspend fun importSongsFromUris(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        val importedList = mutableListOf<Song>()
        val mmr = MediaMetadataRetriever()
        for (uri in uris) {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                var displayName = "Imported Track"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        cursor.getString(nameIdx)?.let { displayName = it }
                    }
                }

                mmr.setDataSource(context, uri)
                val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: displayName.substringBeforeLast(".")
                val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: "Unknown Artist"
                val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?: "Imported"
                val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationStr?.toLongOrNull() ?: 0L
                val trackNumber = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull() ?: 0
                val year = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull() ?: 0

                val generatedId = (uri.toString().hashCode().toLong() and 0x7FFFFFFFL) + 1_000_000_000L

                importedList.add(
                    Song(
                        id = generatedId,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = (album.hashCode().toLong() and 0x7FFFFFFFL),
                        duration = duration,
                        uri = uri,
                        artworkUri = null,
                        trackNumber = trackNumber,
                        year = year,
                        dateAdded = System.currentTimeMillis() / 1000
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            mmr.release()
        } catch (_: Exception) {}

        if (importedList.isNotEmpty()) {
            dbHelper.upsertAll(importedList)
        }
        importedList.size
    }

    private fun scanMediaStore(): List<Song> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%') " +
                "AND ${MediaStore.Audio.Media.DURATION} >= 5000"

        val songs = mutableListOf<Song>()
        context.contentResolver.query(
            collection, projection, selection, null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                songs += Song(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown",
                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                    album = cursor.getString(albumCol) ?: "Unknown Album",
                    albumId = albumId,
                    duration = cursor.getLong(durationCol),
                    uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    ),
                    artworkUri = Uri.parse(
                        "content://media/external/audio/albumart/$albumId"
                    ),
                    trackNumber = cursor.getInt(trackCol),
                    year = cursor.getInt(yearCol),
                    dateAdded = cursor.getLong(dateCol)
                )
            }
        }
        return songs
    }
}
