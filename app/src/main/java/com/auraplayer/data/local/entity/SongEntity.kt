package com.auraplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.net.Uri
import com.auraplayer.domain.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val uriString: String,
    val artworkUriString: String?,
    val trackNumber: Int,
    val year: Int,
    val dateAdded: Long
) {
    fun toDomain() = Song(
        id = id, title = title, artist = artist, album = album,
        albumId = albumId, duration = duration,
        uri = Uri.parse(uriString),
        artworkUri = artworkUriString?.let { Uri.parse(it) },
        trackNumber = trackNumber, year = year, dateAdded = dateAdded
    )
}

fun Song.toEntity() = SongEntity(
    id = id, title = title, artist = artist, album = album,
    albumId = albumId, duration = duration,
    uriString = uri.toString(),
    artworkUriString = artworkUri?.toString(),
    trackNumber = trackNumber, year = year, dateAdded = dateAdded
)
