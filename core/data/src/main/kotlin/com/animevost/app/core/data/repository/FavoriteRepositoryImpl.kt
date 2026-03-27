package com.animevost.app.core.data.repository

import com.animevost.app.core.data.db.FavoriteDao
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.repository.FavoriteRepository
import com.animevost.app.core.network.AnimeVostApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val api: AnimeVostApi,
) : FavoriteRepository {

    override suspend fun getFavorites(page: Int): List<AnimePreview> {
        val pageSize = 20
        val offset = (page - 1) * pageSize
        return favoriteDao.getPage(pageSize, offset).map { entity ->
            AnimePreview(
                id = entity.newsId,
                title = entity.title,
                titleOriginal = entity.titleOriginal,
                posterUrl = entity.posterUrl,
                episodeInfo = entity.episodeInfo,
                url = entity.url,
            )
        }
    }

    override suspend fun toggleFavorite(newsId: Int): Boolean {
        val isFav = favoriteDao.isFavorite(newsId)
        if (isFav) {
            api.toggleFavorite(newsId, "del")
            favoriteDao.deleteByNewsId(newsId)
            return false
        } else {
            api.toggleFavorite(newsId, "add")
            return true
        }
    }
}
