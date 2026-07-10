package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.repository.CommentRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

class DeleteCommentUseCase @Inject constructor(
    private val repository: CommentRepository,
) {
    suspend operator fun invoke(commentId: Int): Result<Unit> =
        repository.deleteComment(commentId)
}
