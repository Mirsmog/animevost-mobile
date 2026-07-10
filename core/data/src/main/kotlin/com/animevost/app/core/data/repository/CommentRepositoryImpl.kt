package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.model.CommentPage
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.CommentRepository
import com.animevost.app.core.domain.util.Result
import com.animevost.app.core.data.sdk.toDomain
import com.animevost.sdk.AnimeVostClient
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val client: AnimeVostClient,
    private val authRepository: AuthRepository,
) : CommentRepository {

    override suspend fun getComments(newsId: Int, page: Int, url: String): Result<CommentPage> {
        return try {
            val commentPage = if (page <= 1) {
                client.getComments(url)
            } else {
                client.getComments(newsId = newsId, page = page)
            }
            Result.Success(commentPage.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun addComment(newsId: Int, text: String): Result<Comment> {
        return try {
            val user = authRepository.getCurrentUser()
                ?: return Result.Error(Exception("Не авторизован"), "Войдите в аккаунт")
            val result = client.addComment(
                newsId = newsId,
                text = text,
                authorName = user.name,
            )
            val comment = result.comments.firstOrNull()?.toDomain()
                ?: Comment(id = 0, author = user.name, date = "", text = text, avatar = "")
            Result.Success(comment)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun getReplyTemplate(commentId: Int): Result<String> {
        return try {
            Result.Success(client.getCommentReplyTemplate(commentId).markup)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun reportComment(commentId: Int, text: String): Result<Unit> {
        return try {
            val result = client.reportComment(commentId = commentId, text = text)
            if (result.success) {
                Result.Success(Unit)
            } else {
                Result.Error(message = result.message ?: "Жалоба не отправлена")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun deleteComment(commentId: Int): Result<Unit> {
        return try {
            val result = client.deleteComment(commentId = commentId)
            if (result.success) {
                Result.Success(Unit)
            } else {
                Result.Error(message = result.message ?: "Комментарий не удален")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
}
