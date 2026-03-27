package com.animevost.app.feature.catalog

import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.AnimeType
import com.animevost.app.core.domain.model.Genre

data class CatalogUiState(
    val selectedTab: CatalogTab = CatalogTab.GENRES,
    val genres: List<Genre> = CatalogDefaults.genres,
    val types: List<AnimeType> = AnimeType.entries.filter { it.urlSlug.isNotEmpty() },
    val years: List<String> = CatalogDefaults.years,
    val isLoadingNav: Boolean = false,
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
        Genre(1, "Боевые искусства", "/zhanr/boyevyye-iskusstva/"),
        Genre(2, "Война", "/zhanr/voyna/"),
        Genre(3, "Драма", "/zhanr/drama/"),
        Genre(4, "Детектив", "/zhanr/detektiv/"),
        Genre(5, "История", "/zhanr/istoriya/"),
        Genre(6, "Комедия", "/zhanr/komediya/"),
        Genre(7, "Меха", "/zhanr/mekha/"),
        Genre(8, "Мистика", "/zhanr/mistika/"),
        Genre(9, "Махо-сёдзё", "/zhanr/makho-sedze/"),
        Genre(10, "Музыкальный", "/zhanr/muzykalnyy/"),
        Genre(11, "Повседневность", "/zhanr/povsednevnost/"),
        Genre(12, "Приключения", "/zhanr/priklyucheniya/"),
        Genre(13, "Пародия", "/zhanr/parodiya/"),
        Genre(14, "Романтика", "/zhanr/romantika/"),
        Genre(15, "Сёнэн", "/zhanr/senen/"),
        Genre(16, "Сёдзё", "/zhanr/sedze/"),
        Genre(17, "Спорт", "/zhanr/sport/"),
        Genre(18, "Сказка", "/zhanr/skazka/"),
        Genre(19, "Сёдзё-ай", "/zhanr/sedze-ay/"),
        Genre(20, "Самураи", "/zhanr/samurai/"),
        Genre(21, "Триллер", "/zhanr/triller/"),
        Genre(22, "Ужасы", "/zhanr/uzhasy/"),
        Genre(23, "Фантастика", "/zhanr/fantastika/"),
        Genre(24, "Фэнтези", "/zhanr/fentezi/"),
        Genre(25, "Школа", "/zhanr/shkola/"),
        Genre(26, "Этти", "/zhanr/etti/"),
    )

    val years: List<String> = (listOf(2025, 2024, 2023, 2022, 2021, 2020, 2019, 2018, 2017, 2016, 2015, 2014, 2013, 2012, 2011, 2010, 2009, 2008, 2007, 2006, 2005, 2004, 2003, 2002, 2001, 2000, 1999, 1998, 1997, 1996, 1995, 1994, 1993, 1992, 1991, 1984, 1980, 1977, 1971)).map { it.toString() }
}
