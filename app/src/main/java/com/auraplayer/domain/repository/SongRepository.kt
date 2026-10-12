package com.auraplayer.domain.repository

import com.auraplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun search(query: String): Flow<List<Song>>
    fun getSongsByAlbum(album: String): Flow<List<Song>>
    suspend fun syncWithMediaStore()
}
