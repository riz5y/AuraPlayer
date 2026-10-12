package com.auraplayer.core.di

import android.content.Context
import com.auraplayer.ads.AdManager
import com.auraplayer.data.local.db.AuraDatabaseHelper
import com.auraplayer.data.repository.SongRepositoryImpl
import com.auraplayer.domain.repository.SongRepository
import com.auraplayer.service.AuraMediaControllerManager

/**
 * Standard AppContainer for clean dependency injection.
 * Follows official Android recommended architecture.
 */
class AuraContainer(private val context: Context) {

    val databaseHelper: AuraDatabaseHelper by lazy {
        AuraDatabaseHelper(context)
    }

    val songRepository: SongRepository by lazy {
        SongRepositoryImpl(context, databaseHelper)
    }

    val mediaControllerManager: AuraMediaControllerManager by lazy {
        AuraMediaControllerManager(context)
    }

    val billingManager: com.auraplayer.core.billing.BillingManager by lazy {
        com.auraplayer.core.billing.BillingManager(context)
    }

    val adManager: AdManager by lazy {
        AdManager(context).apply {
            isProUser = { billingManager.isProUser.value }
        }
    }

    val wifiTransferServer: com.auraplayer.core.transfer.WifiTransferServer by lazy {
        com.auraplayer.core.transfer.WifiTransferServer(context)
    }

    val equalizerManager: com.auraplayer.core.audio.EqualizerManager by lazy {
        com.auraplayer.core.audio.EqualizerManager(context)
    }
}
