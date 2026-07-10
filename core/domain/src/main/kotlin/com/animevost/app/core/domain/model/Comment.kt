package com.animevost.app.core.domain.model

data class Comment(
    val id: Int,
    val author: String,
    val date: String,
    val text: String,
    val avatar: String,
    val quotedAuthor: String = "",
    val quotedText: String = "",
    val depth: Int = 0,
    val ordinal: Int? = null,
    val authorCommentCount: Int? = null,
    val canReply: Boolean = false,
    val canReport: Boolean = false,
    val canDelete: Boolean = false,
)
