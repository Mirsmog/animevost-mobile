package com.animevost.app.core.domain.model

data class AnimeListResult(
    val items: List<AnimePreview>,
    val currentPage: Int,
    val totalPages: Int,
)
