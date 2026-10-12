package com.auraplayer.domain.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val uri: Uri,
    val artworkUri: Uri?,
    val trackNumber: Int,
    val year: Int,
    val dateAdded: Long
)
