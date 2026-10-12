package com.auraplayer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.auraplayer.AuraApplication
import com.auraplayer.navigation.AuraNavigation
import com.auraplayer.ui.components.AudioPermissionGate
import com.auraplayer.ui.screens.player.ExpandablePlayerSheet
import com.auraplayer.ui.screens.splash.SplashScreen
import com.auraplayer.ui.theme.AuraTheme

class MainActivity : ComponentActivity() {

    private val container by lazy {
        (application as AuraApplication).container
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val adManager = container.adManager
        val controllerManager = container.mediaControllerManager

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
                        ExpandablePlayerSheet(
                            controllerManager = controllerManager
                        ) {
                            AuraNavigation(
                                controllerManager = controllerManager,
                                songRepository = container.songRepository
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        container.mediaControllerManager.disconnect()
    }
}
