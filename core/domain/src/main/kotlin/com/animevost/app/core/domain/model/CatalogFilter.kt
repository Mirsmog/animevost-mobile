package com.animevost.app.core.domain.model

data class CatalogFilter(
    val genre: Genre? = null,
    val year: String? = null,
    val type: AnimeType? = null,
    val sortBy: SortOption = SortOption.DATE,
    val sortAscending: Boolean = false,
)

enum class SortOption(val displayName: String, val dleField: String, val dleDirection: String) {
    DATE("По дате", "date", "desc"),
    RATING("По рейтингу", "rating", "desc"),
    VIEWS("По просмотрам", "news_read", "desc"),
    COMMENTS("По комментариям", "comm_num", "desc"),
    TITLE("По алфавиту", "title", "desc"),
}
