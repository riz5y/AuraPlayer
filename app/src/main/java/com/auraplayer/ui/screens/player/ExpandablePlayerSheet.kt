package com.auraplayer.ui.screens.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auraplayer.ui.screens.nowplaying.NowPlayingScreen
import com.auraplayer.ui.screens.nowplaying.NowPlayingViewModel

/**
 * The central shell of the app.
 *
 * Wraps all screen content with an expandable bottom sheet that:
 *  - Peeks at 72dp (mini-player height)
 *  - Expands to full screen (now playing)
 *  - sheetProgress (0→1) drives all interpolated values:
 *      corner radius, content alpha, background bleed
 *
 * User-confirmed design:
 *  - Opaque sheet with dynamic album art gradient
 *  - 28dp corners when collapsed, 20dp when expanded
 *  - Glowing progress bar on mini-player
 */
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auraplayer.service.AuraMediaControllerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandablePlayerSheet(
    controllerManager: AuraMediaControllerManager,
    viewModel: NowPlayingViewModel = viewModel(
        factory = NowPlayingViewModel.provideFactory(controllerManager)
    ),
    content: @Composable () -> Unit
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val dynamicColors by viewModel.dynamicColors.collectAsStateWithLifecycle()

    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    // Compute normalized progress: 0f = mini, 1f = full
    val sheetProgress by remember {
        derivedStateOf {
            runCatching {
                val offset = sheetState.bottomSheetState.requireOffset()
                // offset goes from ~maxHeight (collapsed) down to 0 (expanded)
                // We normalize against a rough full-height estimate
                (1f - (offset / 1800f)).coerceIn(0f, 1f)
            }.getOrDefault(0f)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = 72.dp + WindowInsets.navigationBars
            .asPaddingValues().calculateBottomPadding(),
        sheetContainerColor = Color.Transparent,
        sheetShadowElevation = 0.dp,
        sheetDragHandle = null,
        sheetContent = {
            Box(modifier = Modifier.fillMaxSize()) {
                // Full-screen Now Playing (crossfades in as sheet expands)
                if (sheetProgress > 0.02f) {
                    NowPlayingScreen(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = sheetProgress
                            }
                    )
                }

                // Mini-player (crossfades out as sheet expands)
                if (sheetProgress < 0.95f) {
                    MiniPlayerBar(
                        playerState = playerState,
                        dynamicColors = dynamicColors,
                        sheetProgress = sheetProgress,
                        onExpand = {
                            coroutineScope.launch {
                                sheetState.bottomSheetState.expand()
                            }
                        },
                        onPlayPause = { viewModel.playPause() },
                        onSkipNext = { viewModel.skipNext() }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content()
        }
    }
}
