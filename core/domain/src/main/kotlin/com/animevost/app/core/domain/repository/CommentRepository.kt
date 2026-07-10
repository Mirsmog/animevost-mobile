package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.model.CommentPage
import com.animevost.app.core.domain.util.Result

/** Provides access to anime comments and comment submission. */
interface CommentRepository {
    /** Fetches [page] of comments for the anime at [url] identified by [newsId]. */
    suspend fun getComments(newsId: Int, page: Int, url: String): Result<CommentPage>

    /** Posts a new comment with [text] for the anime with [newsId] and returns the created [Comment]. */
    suspend fun addComment(newsId: Int, text: String): Result<Comment>

    /** Returns server-generated quote markup for replying to [commentId]. */
    suspend fun getReplyTemplate(commentId: Int): Result<String>

    /** Sends a complaint for [commentId]. */
    suspend fun reportComment(commentId: Int, text: String): Result<Unit>

    /** Deletes own comment when the current account is allowed to do it. */
    suspend fun deleteComment(commentId: Int): Result<Unit>
}
