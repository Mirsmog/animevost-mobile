package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.repository.CommentRepository
import javax.inject.Inject

class GetCommentsUseCase @Inject constructor(
    private val repository: CommentRepository,
) {
    suspend operator fun invoke(newsId: Int, page: Int, url: String): List<Comment> {
        return repository.getComments(newsId, page, url)
    }
}
