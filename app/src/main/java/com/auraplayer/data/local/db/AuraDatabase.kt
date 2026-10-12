package com.auraplayer.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.auraplayer.data.local.entity.SongEntity

@Database(entities = [SongEntity::class], version = 1, exportSchema = false)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
