package com.animevost.app.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val iconOutlined: ImageVector,
) {
    data object Home : Screen("home", "Главная", Icons.Filled.Home, Icons.Outlined.Home)
    data object Catalog : Screen("catalog", "Каталог", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary)
    data object Search : Screen("search", "Поиск", Icons.Filled.Search, Icons.Outlined.Search)
    data object Schedule : Screen("schedule", "Расписание", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    data object Profile : Screen("profile", "Профиль", Icons.Filled.Person, Icons.Outlined.Person)

    companion object {
        val bottomNavItems = listOf(Home, Catalog, Search, Schedule, Profile)
    }
}

object NavRoutes {
    const val FILTERED_LIST = "filtered_list/{filterType}/{filterValue}/{filterLabel}"
    const val ANIME_DETAIL = "anime_detail/{url}"
    const val PLAYER = "player/{videoId}/{episodeName}/{animeUrl}"
    const val LOGIN = "login"
    const val REGISTER = "register"

    fun filteredList(filterType: String, filterValue: String, filterLabel: String): String =
        "filtered_list/$filterType/${Uri.encode(filterValue)}/${Uri.encode(filterLabel)}"

    fun animeDetail(url: String): String =
        "anime_detail/${Uri.encode(url)}"

    fun player(videoId: String, episodeName: String, animeUrl: String): String =
        "player/${Uri.encode(videoId)}/${Uri.encode(episodeName)}/${Uri.encode(animeUrl)}"
}
