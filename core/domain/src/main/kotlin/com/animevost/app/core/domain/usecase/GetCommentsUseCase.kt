package com.animevost.app.core.domain.usecase

import com.animevost.app.core.domain.model.CommentPage
import com.animevost.app.core.domain.repository.CommentRepository
import com.animevost.app.core.domain.util.Result
import javax.inject.Inject

/** Fetches a page of comments for the anime identified by [newsId]. */
class GetCommentsUseCase @Inject constructor(
    private val repository: CommentRepository,
) {
    suspend operator fun invoke(newsId: Int, page: Int, url: String): Result<CommentPage> =
        repository.getComments(newsId, page, url)
}
