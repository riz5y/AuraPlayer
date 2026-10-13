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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import com.auraplayer.ads.AdManager
import com.auraplayer.ads.BannerAdView
import com.auraplayer.core.audio.EqualizerManager
import com.auraplayer.core.billing.BillingManager
import com.auraplayer.domain.repository.SongRepository
import com.auraplayer.service.AuraMediaControllerManager
import com.auraplayer.ui.screens.library.LibraryScreen
import com.auraplayer.ui.screens.nowplaying.NowPlayingScreen
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = DeepBlack,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface)
                ) {
                    // ── 1. Floating Spotify MiniPlayer (when track is active) ──
                    AnimatedVisibility(
                        visible = playerState.currentMediaItem != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        MiniPlayerBar(
                            playerState = playerState,
                            dynamicColors = dynamicColors,
                            sheetProgress = 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            onExpand = { showNowPlaying = true },
                            onPlayPause = { nowPlayingViewModel.playPause() },
                            onSkipNext = { nowPlayingViewModel.skipNext() }
                        )
                    }

                    // ── 2. Navigation Bar (Always visible, clean, Spotify floating look) ──
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

        // ── 4. Full-screen Smooth Animated Now Playing Screen Overlay ────────
        AnimatedVisibility(
            visible = showNowPlaying,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut()
        ) {
            BackHandler(enabled = showNowPlaying) {
                showNowPlaying = false
            }
            NowPlayingScreen(
                viewModel = nowPlayingViewModel,
                onCollapse = { showNowPlaying = false },
                equalizerManager = equalizerManager,
                adManager = adManager,
                billingManager = billingManager,
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepBlack)
            )
        }
    }
}
