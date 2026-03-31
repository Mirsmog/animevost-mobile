package com.animevost.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.animevost.app.core.domain.model.Comment
import com.animevost.app.core.domain.repository.CommentRepository
import com.animevost.app.core.domain.util.Result
import com.animevost.app.core.network.AnimeVostApi
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.parser.CommentParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val api: AnimeVostApi,
    private val htmlFetcher: HtmlFetcher,
    private val commentParser: CommentParser,
    private val dataStore: DataStore<Preferences>,
) : CommentRepository {

    override suspend fun getComments(newsId: Int, page: Int, url: String): Result<List<Comment>> {
        return try {
            val html = htmlFetcher.fetch(url)
            Result.Success(commentParser.parseCommentsHtml(html))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun addComment(newsId: Int, text: String): Result<Comment> {
        return try {
            val username = dataStore.data
                .map { it[AuthRepositoryImpl.KEY_USERNAME] }
                .firstOrNull()
                .orEmpty()
            val response = api.addComment(newsId, text, username)
            val comments = commentParser.parse(response)
            val comment = comments.firstOrNull()
                ?: Comment(id = 0, author = username, date = "", text = text, avatar = "")
            Result.Success(comment)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }
}
