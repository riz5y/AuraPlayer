package com.auraplayer.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.auraplayer.service.AuraMediaControllerManager
import com.auraplayer.ui.screens.library.LibraryScreen
import com.auraplayer.ui.theme.*

import androidx.compose.ui.unit.dp
import com.auraplayer.domain.repository.SongRepository

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Library : Screen("library", "Library", Icons.Rounded.LibraryMusic)
    object Search  : Screen("search",  "Search",  Icons.Rounded.Search)
    object Settings: Screen("settings","Settings",Icons.Rounded.Settings)
}

private val navScreens = listOf(Screen.Library, Screen.Search, Screen.Settings)

@Composable
fun AuraNavigation(
    controllerManager: AuraMediaControllerManager,
    songRepository: SongRepository
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        containerColor = DeepBlack,
        bottomBar = {
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
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Library.route) {
                LibraryScreen(
                    controllerManager = controllerManager,
                    songRepository = songRepository
                )
            }
            composable(Screen.Search.route) {
                // Phase 5 — Search screen placeholder
                LibraryScreen(
                    controllerManager = controllerManager,
                    songRepository = songRepository
                )
            }
            composable(Screen.Settings.route) {
                com.auraplayer.ui.screens.settings.SettingsScreen(
                    songRepository = songRepository
                )
            }
        }
    }
}
