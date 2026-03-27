package com.animevost.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Catalog : Screen("catalog", "Catalog", Icons.Default.VideoLibrary)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object Schedule : Screen("schedule", "Schedule", Icons.Default.CalendarMonth)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)

    companion object {
        val bottomNavItems = listOf(Home, Catalog, Search, Schedule, Profile)
    }
}
