package com.animevost.app.core.domain.model

data class CatalogFilter(
    val genre: Genre? = null,
    val year: String? = null,
    val type: AnimeType? = null,
    val sortBy: SortOption = SortOption.DATE,
)

enum class SortOption(val displayName: String) {
    DATE("По дате"),
    RATING("По рейтингу"),
    VIEWS("По просмотрам"),
    TITLE("По названию"),
}
