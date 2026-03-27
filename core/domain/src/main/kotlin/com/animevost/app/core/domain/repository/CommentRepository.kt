package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.Comment

interface CommentRepository {
    suspend fun getComments(newsId: Int, page: Int): List<Comment>
    suspend fun addComment(newsId: Int, text: String): Comment
}
