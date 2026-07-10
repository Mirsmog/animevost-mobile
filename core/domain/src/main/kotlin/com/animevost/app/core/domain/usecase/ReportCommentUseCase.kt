package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.repository.CommentRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

class ReportCommentUseCase @Inject constructor(
    private val repository: CommentRepository,
) {
    suspend operator fun invoke(commentId: Int, text: String): Result<Unit> =
        repository.reportComment(commentId, text)
}
