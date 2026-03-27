package com.animevost.app.feature.catalog

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeType
import com.animevost.app.core.domain.model.Genre

data class CatalogUiState(
    val selectedTab: CatalogTab = CatalogTab.GENRES,
    val genres: List<Genre> = CatalogDefaults.genres,
    val types: List<AnimeType> = AnimeType.entries.filter { it != AnimeType.UNKNOWN },
    val years: List<String> = CatalogDefaults.years,
)

enum class CatalogTab(val title: String) {
    GENRES("Жанры"),
    TYPES("Типы"),
    YEARS("Годы"),
}

data class FilteredListUiState(
    val animeList: List<AnimePreview> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true,
    val filterLabel: String = "",
)

sealed interface FilteredListEvent {
    data object LoadMore : FilteredListEvent
    data object Refresh : FilteredListEvent
    data object ClearError : FilteredListEvent
}

object CatalogDefaults {

    val genres = listOf(
        Genre(1, "Боевые искусства", "/zhanr/boevye-iskusstva/"),
        Genre(2, "Вампиры", "/zhanr/vampiry/"),
        Genre(3, "Война", "/zhanr/vojna/"),
        Genre(4, "Гарем", "/zhanr/garem/"),
        Genre(5, "Демоны", "/zhanr/demony/"),
        Genre(6, "Детектив", "/zhanr/detektiv/"),
        Genre(7, "Детское", "/zhanr/detskoe/"),
        Genre(8, "Дзёсэй", "/zhanr/dzyosyej/"),
        Genre(9, "Драма", "/zhanr/drama/"),
        Genre(10, "Игры", "/zhanr/igry/"),
        Genre(11, "Исторический", "/zhanr/istoricheskij/"),
        Genre(12, "Комедия", "/zhanr/komediya/"),
        Genre(13, "Космос", "/zhanr/kosmos/"),
        Genre(14, "Магия", "/zhanr/magiya/"),
        Genre(15, "Махо-сёдзё", "/zhanr/makho-sjodzyo/"),
        Genre(16, "Машины", "/zhanr/mashiny/"),
        Genre(17, "Меха", "/zhanr/mekha/"),
        Genre(18, "Музыка", "/zhanr/muzyka/"),
        Genre(19, "Пародия", "/zhanr/parodiya/"),
        Genre(20, "Повседневность", "/zhanr/povsednevnost/"),
        Genre(21, "Полиция", "/zhanr/politsiya/"),
        Genre(22, "Приключения", "/zhanr/priklyucheniya/"),
        Genre(23, "Психологическое", "/zhanr/psikhologicheskoe/"),
        Genre(24, "Романтика", "/zhanr/romantika/"),
        Genre(25, "Самураи", "/zhanr/samurai/"),
        Genre(26, "Сверхъестественное", "/zhanr/sverkhyestestvennoe/"),
        Genre(27, "Сёдзё", "/zhanr/sjodzyo/"),
        Genre(28, "Сёдзё-ай", "/zhanr/sjodzyo-aj/"),
        Genre(29, "Сёнэн", "/zhanr/sjonyena/"),
        Genre(30, "Сёнэн-ай", "/zhanr/sjonyena-aj/"),
        Genre(31, "Спорт", "/zhanr/sport/"),
        Genre(32, "Супер сила", "/zhanr/super-sila/"),
        Genre(33, "Сэйнэн", "/zhanr/syejnyena/"),
        Genre(34, "Триллер", "/zhanr/triller/"),
        Genre(35, "Ужасы", "/zhanr/uzhasy/"),
        Genre(36, "Фантастика", "/zhanr/fantastika/"),
        Genre(37, "Фэнтези", "/zhanr/fyentezi/"),
        Genre(38, "Хентай", "/zhanr/khentaj/"),
        Genre(39, "Школа", "/zhanr/shkola/"),
        Genre(40, "Экшен", "/zhanr/yekshen/"),
        Genre(41, "Этти", "/zhanr/yetti/"),
    )

    val years: List<String> = (2025 downTo 2000).map { it.toString() }
}
