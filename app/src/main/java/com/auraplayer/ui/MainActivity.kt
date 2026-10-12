package com.auraplayer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.auraplayer.navigation.AuraNavigation
import com.auraplayer.service.AuraMediaControllerManager
import com.auraplayer.ui.screens.player.ExpandablePlayerSheet
import com.auraplayer.ui.theme.AuraTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var controllerManager: AuraMediaControllerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        controllerManager.connect()

        setContent {
            AuraTheme {
                // Sheet wraps all navigation content
                ExpandablePlayerSheet {
                    AuraNavigation(controllerManager = controllerManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerManager.disconnect()
    }
}
