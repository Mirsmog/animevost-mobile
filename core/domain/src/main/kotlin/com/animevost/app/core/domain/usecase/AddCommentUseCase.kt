package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.model.CommentScope
import com.animevost.app.core.domain.repository.CommentRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Posts a new comment and returns the created [Comment] on success. */
class AddCommentUseCase @Inject constructor(
    private val repository: CommentRepository,
) {
    suspend operator fun invoke(
        newsId: Int,
        text: String,
        scope: CommentScope = CommentScope.Anime,
    ): Result<Comment> = repository.addComment(newsId, text, scope)
}
