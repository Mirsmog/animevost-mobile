package com.animevost.app.core.domain.model

enum class AnimeStatus(val label: String, val code: String) {
    WATCHING("Смотрю", "w"),
    WATCHED("Просмотрено", "d"),
    DROPPED("Брошено", "x"),
    PLANNED("Запланировано", "p"),
    ON_HOLD("Отложено", "h");

    companion object {
        fun fromCode(code: String): AnimeStatus? = entries.find { it.code == code }
    }
}
