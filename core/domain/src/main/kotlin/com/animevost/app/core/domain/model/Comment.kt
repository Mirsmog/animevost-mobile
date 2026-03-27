package com.animevost.app.core.domain.model

data class Comment(
    val id: Int,
    val author: String,
    val date: String,
    val text: String,
    val avatar: String,
)
