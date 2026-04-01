package com.animevost.app.core.data.repository

import com.animevost.app.core.data.db.FavoriteDao
import com.animevost.app.core.data.mapper.toAnimePreview
import com.animevost.app.core.data.mapper.toFavoriteEntity
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.util.Result
import com.animevost.app.core.network.DleEndpoints
import com.animevost.app.core.network.HtmlFetcher
import com.animevost.app.core.network.SessionCookieJar
import com.animevost.app.core.network.parser.FavoritesParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val htmlFetcher: HtmlFetcher,
    private val favoritesParser: FavoritesParser,
    private val cookieJar: SessionCookieJar,
) : FavoriteRepository {

    /** Prevents concurrent sync and toggle from conflicting on the favorites state. */
    private val favMutex = Mutex()

    override fun getAllFavorites(): Flow<List<AnimePreview>> =
        favoriteDao.getAll().map { list -> list.map { it.toAnimePreview() } }

    override suspend fun getFavorites(page: Int): List<AnimePreview> {
        val pageSize = 20
        val offset = (page - 1) * pageSize
        return favoriteDao.getPage(pageSize, offset).map { it.toAnimePreview() }
    }

    override suspend fun isFavorite(newsId: Int): Boolean = favoriteDao.isFavorite(newsId)

    override fun isFavoriteFlow(newsId: Int): Flow<Boolean> = favoriteDao.isFavoriteFlow(newsId)

    override suspend fun toggleFavorite(newsId: Int, preview: AnimePreview?): Boolean = favMutex.withLock {
        val isFav = favoriteDao.isFavorite(newsId)
        if (isFav) {
            favoriteDao.deleteByNewsId(newsId)
            mirrorToRemote(newsId, add = false)
            false
        } else {
            if (preview != null) favoriteDao.insert(preview.toFavoriteEntity())
            mirrorToRemote(newsId, add = true)
            true
        }
    }

    override suspend fun syncWithRemote(): Result<Unit> {
        if (!isLoggedIn()) return Result.Error(IllegalStateException("Not logged in"))
        return favMutex.withLock {
            try {
                val html = htmlFetcher.fetch(DleEndpoints.BASE_URL + DleEndpoints.FAVORITES)
                val remoteItems = favoritesParser.parse(html)
                val remoteIds = remoteItems.map { it.id }.toSet()
                val localIds = favoriteDao.getAllIds().toSet()

                // Remote is the source of truth when logged in:
                // 1. Pull remote-only items → insert into local DB
                val toPull = remoteItems.filter { it.id !in localIds }
                toPull.forEach { favoriteDao.insert(it.toFavoriteEntity()) }

                // 2. Delete local items not present on remote (stale / incorrectly synced)
                //    New favorites added in-app are immediately mirrored via mirrorToRemote(),
                //    so any local-only item here is considered stale.
                val toDelete = localIds - remoteIds
                toDelete.forEach { favoriteDao.deleteByNewsId(it) }

                Timber.d("Favorites sync: pulled=${toPull.size}, deleted=${toDelete.size}")
                Result.Success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Favorites sync failed")
                Result.Error(e)
            }
        }
    }

    /** Best-effort mirror of a local toggle to the remote server. Errors are logged, not thrown. */
    private suspend fun mirrorToRemote(newsId: Int, add: Boolean) {
        if (!isLoggedIn()) return
        try {
            val endpoint = if (add) DleEndpoints.FAVORITES_ADD else DleEndpoints.FAVORITES_REMOVE
            htmlFetcher.fetch(DleEndpoints.BASE_URL + endpoint + newsId)
        } catch (e: Exception) {
            Timber.w(e, "Failed to mirror favorite newsId=$newsId add=$add to remote")
        }
    }

    private fun isLoggedIn(): Boolean =
        cookieJar.getCookieValue("animevost.org", "dle_user_id") != null
}

