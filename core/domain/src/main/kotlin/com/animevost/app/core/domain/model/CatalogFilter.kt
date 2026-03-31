package com.animevost.app.core.domain.model

/**
 * Parameters controlling which anime are fetched from the catalogue.
 * All filter fields are optional; `null` means "no restriction".
 *
 * @property sortBy Column to order results by.
 * @property sortAscending `true` for ascending order, `false` (default) for descending.
 */
data class CatalogFilter(
    val genre: Genre? = null,
    val year: String? = null,
    val type: AnimeType? = null,
    val sortBy: SortOption = SortOption.DATE,
    val sortAscending: Boolean = false,
)

/** Sortable columns supported by the DLE backend. */
enum class SortOption(val displayName: String, val dleField: String, val dleDirection: String) {
    DATE("По дате", "date", "desc"),
    RATING("По рейтингу", "rating", "desc"),
    VIEWS("По просмотрам", "news_read", "desc"),
    COMMENTS("По комментариям", "comm_num", "desc"),
    TITLE("По алфавиту", "title", "desc"),
}
