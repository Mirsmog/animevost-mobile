package com.animevost.app.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.animevost.app.core.data.db.FavoriteDao
import com.animevost.app.core.data.db.FavoriteEntity
import com.animevost.app.core.data.mapper.toAnimePreview
import com.animevost.app.core.data.mapper.toFavoriteEntity
import com.animevost.app.core.data.sdk.toDomain
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.FavoriteEntry
import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.domain.util.Result
import com.animevost.sdk.AnimeVostClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val client: AnimeVostClient,
    private val dataStore: DataStore<Preferences>,
) : FavoriteRepository {

    private val favMutex = Mutex()

    override fun getAllFavorites(): Flow<List<AnimePreview>> =
        favoriteDao.getAll().map { list -> list.map { it.toAnimePreview() } }

    override fun getAllFavoriteEntries(): Flow<List<FavoriteEntry>> =
        favoriteDao.getAll().map { list -> list.map { it.toEntry() } }

    override suspend fun getFavorites(page: Int): List<AnimePreview> {
        val pageSize = 20
        val offset = (page - 1) * pageSize
        return favoriteDao.getPage(pageSize, offset).map { it.toAnimePreview() }
    }

    override suspend fun isFavorite(newsId: Int): Boolean =
        favoriteDao.isFavorite(newsId)

    override fun isFavoriteFlow(newsId: Int): Flow<Boolean> =
        favoriteDao.isFavoriteFlow(newsId)

    override suspend fun toggleFavorite(newsId: Int, preview: AnimePreview?): Boolean = favMutex.withLock {
        if (isLoggedIn()) {
            val accountId = requireNotNull(client.currentSession()?.userId) {
                "Authenticated session has no user id"
            }
            ensureCacheBelongsTo(accountId)
            val isFav = favoriteDao.isFavorite(newsId)
            if (isFav) {
                client.removeFavorite(newsId)
            } else {
                client.addFavorite(newsId)
            }
            if (isFav) {
                favoriteDao.deleteByNewsId(newsId)
            } else if (preview != null) {
                favoriteDao.insert(preview.toFavoriteEntity())
            }
            setOwnerAccountId(accountId)
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
                val accountId = requireNotNull(client.currentSession()?.userId) {
                    "Authenticated session has no user id"
                }
                // 1. Fetch current remote favorites (all pages)
                val remoteItems = fetchAllRemotePages()
                val remoteIds = remoteItems.map { it.id }.toSet()

                // 2. Only anonymous or same-account local items may be merged.
                val ownerAccountId = ownerAccountId()
                val localIds = if (canMergeLocalFavorites(ownerAccountId, accountId)) {
                    favoriteDao.getAllIds()
                } else {
                    emptyList()
                }
                val toPush = localIds.filter { it !in remoteIds }
                toPush.forEach { id ->
                    client.addFavorite(id)
                }

                // 3. Re-fetch remote if we pushed anything (now includes merged items)
                val finalRemote = if (toPush.isNotEmpty()) fetchAllRemotePages() else remoteItems

                // 4. Cache merged server state locally
                favoriteDao.replaceAll(finalRemote.toCachedEntities(previous = favoriteDao.getAllList()))
                setOwnerAccountId(accountId)

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
        if (ownerAccountId() == null && favoriteDao.getAllIds().isNotEmpty()) {
            return syncOnLogin()
        }
        return try {
            favMutex.withLock {
                val accountId = requireNotNull(client.currentSession()?.userId) {
                    "Authenticated session has no user id"
                }
                val items = fetchAllRemotePages()
                favoriteDao.replaceAll(items.toCachedEntities(previous = favoriteDao.getAllList()))
                setOwnerAccountId(accountId)
            }
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to load remote favorites")
            Result.Error(e)
        }
    }

    override suspend fun clearLocal() {
        favMutex.withLock {
            favoriteDao.deleteAll()
            dataStore.edit { it.remove(KEY_OWNER_ACCOUNT_ID) }
        }
    }

    private suspend fun ensureCacheBelongsTo(accountId: Int) {
        val ownerAccountId = ownerAccountId() ?: return
        if (ownerAccountId == accountId) return
        val items = fetchAllRemotePages()
        favoriteDao.replaceAll(items.toCachedEntities(previous = emptyList()))
        setOwnerAccountId(accountId)
    }

    private suspend fun ownerAccountId(): Int? =
        dataStore.data.first()[KEY_OWNER_ACCOUNT_ID]

    private suspend fun setOwnerAccountId(accountId: Int) {
        dataStore.edit { it[KEY_OWNER_ACCOUNT_ID] = accountId }
    }

    private suspend fun fetchAllRemotePages(): List<AnimePreview> {
        val allItems = mutableListOf<AnimePreview>()
        var page = 1
        while (true) {
            val remotePage = client.getFavorites(page)
            val items = remotePage.items.map { it.toDomain() }
            if (items.isEmpty()) break
            allItems.addAll(items)
            if (page >= remotePage.totalPages) break
            page++
        }
        return allItems.distinctBy { it.id }
    }

    private fun isLoggedIn(): Boolean =
        client.isLoggedIn()

    private fun FavoriteEntity.toEntry() = FavoriteEntry(
        anime = toAnimePreview(),
        addedAt = addedAt,
    )

    private fun List<AnimePreview>.toCachedEntities(previous: List<FavoriteEntity>): List<FavoriteEntity> {
        val previousMap = previous.associateBy { it.newsId }
        return map { anime ->
            anime.toFavoriteEntity().copy(
                addedAt = previousMap[anime.id]?.addedAt ?: System.currentTimeMillis(),
            )
        }
    }

    private companion object {
        val KEY_OWNER_ACCOUNT_ID = intPreferencesKey("favorite_owner_account_id")
    }
}

internal fun canMergeLocalFavorites(ownerAccountId: Int?, currentAccountId: Int): Boolean =
    ownerAccountId == null || ownerAccountId == currentAccountId
