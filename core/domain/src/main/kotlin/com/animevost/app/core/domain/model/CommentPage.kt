package com.animevost.app.core.domain.model

data class CommentPage(
    val comments: List<Comment>,
    val currentPage: Int,
    val totalPages: Int,
)
