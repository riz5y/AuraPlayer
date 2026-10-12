package com.auraplayer.core.di

import android.content.Context
import androidx.room.Room
import com.auraplayer.data.local.db.AuraDatabase
import com.auraplayer.data.local.db.SongDao
import com.auraplayer.data.repository.SongRepositoryImpl
import com.auraplayer.domain.repository.SongRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AuraDatabase =
        Room.databaseBuilder(ctx, AuraDatabase::class.java, "aura_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideSongDao(db: AuraDatabase): SongDao = db.songDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindSongRepository(impl: SongRepositoryImpl): SongRepository
}
