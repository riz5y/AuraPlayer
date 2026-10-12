package com.auraplayer.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.auraplayer.ads.AdManager
import com.auraplayer.ads.BannerAdView
import com.auraplayer.core.audio.EqualizerManager
import com.auraplayer.core.billing.BillingManager
import com.auraplayer.domain.repository.SongRepository
import com.auraplayer.service.AuraMediaControllerManager
import com.auraplayer.ui.screens.library.LibraryScreen
import com.auraplayer.ui.screens.nowplaying.NowPlayingModal
import com.auraplayer.ui.screens.nowplaying.NowPlayingViewModel
import com.auraplayer.ui.screens.player.MiniPlayerBar
import com.auraplayer.ui.screens.settings.SettingsScreen
import com.auraplayer.ui.theme.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Library : Screen("library", "Library", Icons.Rounded.LibraryMusic)
    object Search  : Screen("search",  "Search",  Icons.Rounded.Search)
    object Settings: Screen("settings","Settings",Icons.Rounded.Settings)
}

private val navScreens = listOf(Screen.Library, Screen.Search, Screen.Settings)

@Composable
fun AuraNavigation(
    controllerManager: AuraMediaControllerManager,
    songRepository: SongRepository,
    billingManager: BillingManager,
    adManager: AdManager? = null,
    equalizerManager: EqualizerManager? = null
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val nowPlayingViewModel: NowPlayingViewModel = viewModel(
        factory = NowPlayingViewModel.provideFactory(controllerManager)
    )
    val playerState by nowPlayingViewModel.playerState.collectAsStateWithLifecycle()
    val dynamicColors by nowPlayingViewModel.dynamicColors.collectAsStateWithLifecycle()
    val isPro by billingManager.isProUser.collectAsStateWithLifecycle()

    var showNowPlaying by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DeepBlack,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
            ) {
                // ── 1. Clean Framed Banner Ad (Only for free tier) ────
                if (!isPro) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D0D10))
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BannerAdView()
                    }
                }

                // ── 2. Floating Spotify MiniPlayer (when track is active) ──
                if (playerState.currentMediaItem != null) {
                    MiniPlayerBar(
                        playerState = playerState,
                        dynamicColors = dynamicColors,
                        sheetProgress = 0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        onExpand = { showNowPlaying = true },
                        onPlayPause = { nowPlayingViewModel.playPause() },
                        onSkipNext = { nowPlayingViewModel.skipNext() }
                    )
                }

                // ── 3. Navigation Bar (Always visible, clean, never obscured!) ──
                NavigationBar(
                    containerColor = Surface,
                    tonalElevation = 0.dp
                ) {
                    navScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Library.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(screen.icon, contentDescription = screen.label)
                            },
                            label = { Text(screen.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = White,
                                selectedTextColor = White,
                                indicatorColor = SurfaceElevated,
                                unselectedIconColor = WhiteDim,
                                unselectedTextColor = WhiteDim
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Library.route) {
                LibraryScreen(
                    controllerManager = controllerManager,
                    songRepository = songRepository,
                    billingManager = billingManager,
                    adManager = adManager,
                    equalizerManager = equalizerManager
                )
            }
            composable(Screen.Search.route) {
                LibraryScreen(
                    controllerManager = controllerManager,
                    songRepository = songRepository,
                    billingManager = billingManager,
                    adManager = adManager,
                    equalizerManager = equalizerManager
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    songRepository = songRepository,
                    billingManager = billingManager,
                    equalizerManager = equalizerManager,
                    adManager = adManager
                )
            }
        }
    }

    // ── 4. Full-screen Now Playing Modal Sheet ────────────────────────
    if (showNowPlaying) {
        NowPlayingModal(
            viewModel = nowPlayingViewModel,
            onDismiss = { showNowPlaying = false }
        )
    }
}
