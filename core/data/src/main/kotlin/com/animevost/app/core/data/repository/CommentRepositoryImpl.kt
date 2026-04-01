package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.repository.CommentRepository
import com.animevost.app.core.domain.util.Result
import com.animevost.app.core.network.AnimeVostApi
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.parser.CommentParser
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val api: AnimeVostApi,
    private val htmlFetcher: HtmlFetcher,
    private val commentParser: CommentParser,
    private val authRepository: AuthRepository,
) : CommentRepository {

    override suspend fun getComments(newsId: Int, page: Int, url: String): Result<List<Comment>> {
        return try {
            if (page <= 1) {
                // Page 1: comments are already embedded in the detail page HTML
                val html = htmlFetcher.fetch(url)
                Result.Success(commentParser.parseCommentsHtml(html))
            } else {
                // Page 2+: use AJAX endpoint
                val response = api.getComments(page, newsId)
                Result.Success(commentParser.parse(response))
            }
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
            val responseBody = api.addComment(
                postId = newsId,
                text = text,
                name = user.name,
            )
            val responseText = responseBody.string()
            if (responseText.contains("Hacking attempt", ignoreCase = true) ||
                responseText.contains("error", ignoreCase = true) && responseText.length < 50
            ) {
                return Result.Error(Exception(responseText), responseText)
            }
            // Parse newly added comment from response HTML; fall back to a synthetic comment
            val parsed = commentParser.parseCommentsHtml(responseText)
            val comment = parsed.firstOrNull()
                ?: Comment(id = 0, author = user.name, date = "", text = text, avatar = "")
            Result.Success(comment)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
}
