package com.animevost.app.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.animevost.app.feature.auth.LoginScreen
import com.animevost.app.feature.auth.RegisterScreen
import com.animevost.app.feature.catalog.FilteredListScreen
import com.animevost.app.feature.detail.DetailScreen
import com.animevost.app.feature.home.HomeScreen
import com.animevost.app.feature.player.PlayerScreen
import com.animevost.app.feature.profile.ProfileScreen
import com.animevost.app.feature.schedule.ScheduleScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = Screen.bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy
                            ?.any { it.route == screen.route } == true

                        NavigationBarItem(
                            icon = {
                                Box {
                                    Icon(
                                        imageVector = if (isSelected) screen.icon else screen.iconOutlined,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onAnimeClick = { url ->
                        navController.navigate(NavRoutes.animeDetail(url))
                    },
                    onNavigateToFilteredList = { filterType, filterValue, filterLabel ->
                        navController.navigate(
                            NavRoutes.filteredList(filterType, filterValue, filterLabel),
                        )
                    },
                )
            }
            composable(Screen.Schedule.route) {
                ScheduleScreen(
                    onAnimeClick = { url ->
                        navController.navigate(NavRoutes.animeDetail(url))
                    },
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToLogin = { navController.navigate(NavRoutes.LOGIN) },
                    onAnimeClick = { url -> navController.navigate(NavRoutes.animeDetail(url)) },
                )
            }
            composable(
                route = NavRoutes.FILTERED_LIST,
                arguments = listOf(
                    navArgument("filterType") { type = NavType.StringType },
                    navArgument("filterValue") { type = NavType.StringType },
                    navArgument("filterLabel") { type = NavType.StringType },
                ),
            ) {
                FilteredListScreen(
                    onAnimeClick = { url ->
                        navController.navigate(NavRoutes.animeDetail(url))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = NavRoutes.ANIME_DETAIL,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val url = Uri.decode(backStackEntry.arguments?.getString("url") ?: "")
                DetailScreen(
                    animeUrl = url,
                    onBack = { navController.popBackStack() },
                    onPlayEpisode = { episode, _, _ ->
                        navController.navigate(NavRoutes.player(episode.videoId, episode.name, url))
                    },
                    onGenreClick = { genre ->
                        navController.navigate(NavRoutes.filteredList("genre", genre, genre))
                    },
                    onRelatedClick = { relatedUrl ->
                        navController.navigate(NavRoutes.animeDetail(relatedUrl))
                    },
                )
            }
            composable(
                route = NavRoutes.PLAYER,
                arguments = listOf(
                    navArgument("videoId") { type = NavType.StringType },
                    navArgument("episodeName") { type = NavType.StringType },
                    navArgument("animeUrl") { type = NavType.StringType },
                ),
            ) {
                PlayerScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { navController.popBackStack() },
                    onNavigateToRegister = {
                        navController.navigate(NavRoutes.REGISTER) {
                            popUpTo(NavRoutes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }
            composable(NavRoutes.REGISTER) {
                RegisterScreen(
                    onRegisterSuccess = { navController.popBackStack() },
                    onNavigateToLogin = {
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(NavRoutes.REGISTER) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}