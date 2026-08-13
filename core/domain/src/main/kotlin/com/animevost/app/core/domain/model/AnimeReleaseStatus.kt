package com.animevost.app.core.domain.model

enum class AnimeReleaseStatus {
    ONGOING,
    ANNOUNCED,
    COMPLETED,
    UNKNOWN,
    ;

    val supportsEpisodeNotifications: Boolean
        get() = this == ONGOING || this == UNKNOWN

    val shouldPollForEpisodes: Boolean
        get() = this != COMPLETED

    companion object {
        fun fromCategories(categories: Iterable<String>): AnimeReleaseStatus {
            val normalized = categories
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .toList()
            return when {
                normalized.any { it == "онгоинги" || it == "онгоинг" } -> ONGOING
                normalized.any { it == "анонсы" || it == "анонс" } -> ANNOUNCED
                normalized.isNotEmpty() -> COMPLETED
                else -> UNKNOWN
            }
        }

        fun fromStorage(value: String): AnimeReleaseStatus =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}
