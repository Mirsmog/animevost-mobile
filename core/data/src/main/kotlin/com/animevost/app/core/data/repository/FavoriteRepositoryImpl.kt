package com.animevost.app.core.data.repository

import com.animevost.app.core.data.db.FavoriteDao
import com.animevost.app.core.data.db.FavoriteEntity
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
) : FavoriteRepository {

    override fun getAllFavorites(): Flow<List<AnimePreview>> {
        return favoriteDao.getAll().map { list ->
            list.map { it.toPreview() }
        }
    }

    override suspend fun getFavorites(page: Int): List<AnimePreview> {
        val pageSize = 20
        val offset = (page - 1) * pageSize
        return favoriteDao.getPage(pageSize, offset).map { it.toPreview() }
    }

    override suspend fun isFavorite(newsId: Int): Boolean {
        return favoriteDao.isFavorite(newsId)
    }

    override fun isFavoriteFlow(newsId: Int): Flow<Boolean> {
        return favoriteDao.isFavoriteFlow(newsId)
    }

    override suspend fun toggleFavorite(newsId: Int, preview: AnimePreview?): Boolean {
        val isFav = favoriteDao.isFavorite(newsId)
        if (isFav) {
            favoriteDao.deleteByNewsId(newsId)
            return false
        } else {
            if (preview != null) {
                favoriteDao.insert(
                    FavoriteEntity(
                        newsId = newsId,
                        title = preview.title,
                        titleOriginal = preview.titleOriginal,
                        posterUrl = preview.posterUrl,
                        episodeInfo = preview.episodeInfo,
                        url = preview.url,
                    ),
                )
            }
            return true
        }
    }

    private fun FavoriteEntity.toPreview() = AnimePreview(
        id = newsId,
        title = title,
        titleOriginal = titleOriginal,
        posterUrl = posterUrl,
        episodeInfo = episodeInfo,
        url = url,
    )
}
