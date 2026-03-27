package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.repository.CommentRepository
import javax.inject.Inject

class AddCommentUseCase @Inject constructor(
    private val repository: CommentRepository,
) {
    suspend operator fun invoke(newsId: Int, text: String): Comment {
        return repository.addComment(newsId, text)
    }
}
