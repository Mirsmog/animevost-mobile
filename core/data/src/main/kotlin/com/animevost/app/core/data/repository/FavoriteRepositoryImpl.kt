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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

    private val favMutex = Mutex()
    // In-memory state for remote favorites (only used when logged in)
    private val _remoteFavorites = MutableStateFlow<List<AnimePreview>>(emptyList())

    override fun getAllFavorites(): Flow<List<AnimePreview>> =
        if (isLoggedIn()) _remoteFavorites
        else favoriteDao.getAll().map { list -> list.map { it.toAnimePreview() } }

    override suspend fun getFavorites(page: Int): List<AnimePreview> {
        val pageSize = 20
        val offset = (page - 1) * pageSize
        return if (isLoggedIn()) {
            _remoteFavorites.value.drop(offset).take(pageSize)
        } else {
            favoriteDao.getPage(pageSize, offset).map { it.toAnimePreview() }
        }
    }

    override suspend fun isFavorite(newsId: Int): Boolean =
        if (isLoggedIn()) _remoteFavorites.value.any { it.id == newsId }
        else favoriteDao.isFavorite(newsId)

    override fun isFavoriteFlow(newsId: Int): Flow<Boolean> =
        if (isLoggedIn()) _remoteFavorites.map { list -> list.any { it.id == newsId } }
        else favoriteDao.isFavoriteFlow(newsId)

    override suspend fun toggleFavorite(newsId: Int, preview: AnimePreview?): Boolean = favMutex.withLock {
        if (isLoggedIn()) {
            val isFav = _remoteFavorites.value.any { it.id == newsId }
            val endpoint = if (isFav) DleEndpoints.FAVORITES_REMOVE else DleEndpoints.FAVORITES_ADD
            try {
                htmlFetcher.fetch(DleEndpoints.BASE_URL + endpoint + newsId)
            } catch (e: Exception) {
                Timber.w(e, "Failed to toggle remote favorite newsId=$newsId")
            }
            if (isFav) {
                _remoteFavorites.update { current -> current.filter { it.id != newsId } }
            } else if (preview != null) {
                _remoteFavorites.update { current -> current + preview }
            }
            !isFav
        } else {
            val isFav = favoriteDao.isFavorite(newsId)
            if (isFav) {
                favoriteDao.deleteByNewsId(newsId)
                false
            } else {
                if (preview != null) favoriteDao.insert(preview.toFavoriteEntity())
                true
            }
        }
    }

    override suspend fun syncOnLogin(): Result<Unit> {
        return try {
            favMutex.withLock {
                // 1. Fetch current remote favorites (all pages)
                val remoteItems = fetchAllRemotePages()
                val remoteIds = remoteItems.map { it.id }.toSet()

                // 2. Push local-only items to remote (merge on login)
                val localIds = favoriteDao.getAllIds()
                val toPush = localIds.filter { it !in remoteIds }
                toPush.forEach { id ->
                    try {
                        htmlFetcher.fetch(DleEndpoints.BASE_URL + DleEndpoints.FAVORITES_ADD + id)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to push local favorite id=$id to remote on login sync")
                    }
                }

                // 3. Re-fetch remote if we pushed anything (now includes merged items)
                val finalRemote = if (toPush.isNotEmpty()) fetchAllRemotePages() else remoteItems

                // 4. Clear local DB — remote is now the source of truth
                favoriteDao.deleteAll()

                // 5. Update in-memory state
                _remoteFavorites.value = finalRemote

                Timber.d("Login sync: pushed=${toPush.size}, remote total=${finalRemote.size}")
                Result.Success(Unit)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Login sync failed")
            Result.Error(e)
        }
    }

    override suspend fun loadRemoteFavorites(): Result<Unit> {
        if (!isLoggedIn()) return Result.Success(Unit)
        return try {
            val items = fetchAllRemotePages()
            _remoteFavorites.value = items
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to load remote favorites")
            Result.Error(e)
        }
    }

    private suspend fun fetchAllRemotePages(): List<AnimePreview> {
        val allItems = mutableListOf<AnimePreview>()
        var page = 1
        while (true) {
            val url = if (page == 1)
                DleEndpoints.BASE_URL + DleEndpoints.FAVORITES
            else
                DleEndpoints.BASE_URL + DleEndpoints.FAVORITES + "page/$page/"
            val html = htmlFetcher.fetch(url)
            val items = favoritesParser.parse(html)
            if (items.isEmpty()) break
            allItems.addAll(items)
            page++
        }
        return allItems
    }

    private fun isLoggedIn(): Boolean =
        cookieJar.getCookieValue("animevost.org", "dle_user_id") != null
}
