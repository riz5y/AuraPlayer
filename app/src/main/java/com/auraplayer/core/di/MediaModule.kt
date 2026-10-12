package com.auraplayer.core.di

import com.auraplayer.service.AuraMediaControllerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides @Singleton
    fun provideControllerManager(
        @ApplicationContext ctx: Context
    ): AuraMediaControllerManager = AuraMediaControllerManager(ctx)
}
