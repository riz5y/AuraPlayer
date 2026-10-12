package com.auraplayer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.auraplayer.ads.AdManager
import com.auraplayer.navigation.AuraNavigation
import com.auraplayer.service.AuraMediaControllerManager
import com.auraplayer.ui.components.AudioPermissionGate
import com.auraplayer.ui.screens.player.ExpandablePlayerSheet
import com.auraplayer.ui.screens.splash.SplashScreen
import com.auraplayer.ui.theme.AuraTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var controllerManager: AuraMediaControllerManager
    @Inject lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        adManager.initialize()
        controllerManager.connect()

        setContent {
            AuraTheme {
                var isSplashFinished by remember { mutableStateOf(false) }

                if (!isSplashFinished) {
                    SplashScreen(
                        adManager = adManager,
                        onComplete = { isSplashFinished = true }
                    )
                } else {
                    AudioPermissionGate {
                        ExpandablePlayerSheet {
                            AuraNavigation(controllerManager = controllerManager)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerManager.disconnect()
    }
}
