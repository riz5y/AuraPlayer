package com.auraplayer

import android.app.Application
import com.auraplayer.core.di.AuraContainer

class AuraApplication : Application() {
    lateinit var container: AuraContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AuraContainer(this)
    }
}
