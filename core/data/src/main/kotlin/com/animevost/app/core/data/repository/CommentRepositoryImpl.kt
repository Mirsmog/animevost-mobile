package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.repository.CommentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepositoryImpl @Inject constructor() : CommentRepository {

    override suspend fun getComments(newsId: Int, page: Int): List<Comment> {
        TODO("Implement: call AnimeVostApi.getComments, parse response")
    }

    override suspend fun addComment(newsId: Int, text: String): Comment {
        TODO("Implement: call AnimeVostApi.addComment, parse response")
    }
}
