package com.animevost.app.core.domain.model

data class UserListEntry(
    val anime: AnimePreview,
    val status: AnimeStatus,
    val updatedAt: Long,
)
