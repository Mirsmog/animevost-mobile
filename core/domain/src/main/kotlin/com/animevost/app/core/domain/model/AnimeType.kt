package com.animevost.app.core.domain.model

enum class AnimeType(val displayName: String, val urlSlug: String) {
    TV("ТВ", "tv"),
    TV_SPECIAL("ТВ-спэшл", "tv-speshl"),
    OVA("OVA", "ova"),
    ONA("ONA", "ona"),
    FULL_MOVIE("Полнометражный фильм", "polnometrazhnyy-film"),
    SHORT_MOVIE("Короткометражный фильм", "korotkometrazhnyy-film"),
    DUNKHUA("Дунхуа", "dunkhua"),
    UNKNOWN("Неизвестно", ""),
}
