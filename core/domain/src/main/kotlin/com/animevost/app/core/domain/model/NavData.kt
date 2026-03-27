package com.animevost.app.core.domain.model

data class NavData(
    val genres: List<Genre> = emptyList(),
    val years: List<String> = emptyList(),
)
