package com.animevost.app.core.data.repository

import com.animevost.app.core.domain.model.AnimeDetail
import com.animevost.app.core.domain.model.AnimePreview
import com.animevost.app.core.domain.model.CatalogFilter
import com.animevost.app.core.domain.model.NavData
import com.animevost.app.core.domain.repository.AnimeRepository
import com.animevost.app.core.domain.util.Result
import com.animevost.app.core.data.sdk.toDomain
import com.animevost.app.core.data.sdk.toSdk
import com.animevost.sdk.AnimeVostClient
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeRepositoryImpl @Inject constructor(
    private val client: AnimeVostClient,
) : AnimeRepository {

    override suspend fun getAnimeList(page: Int, filter: CatalogFilter): Result<List<AnimePreview>> {
        return try {
            Result.Success(
                client.getAnimeList(page = page, filter = filter.toSdk())
                    .items
                    .map { it.toDomain() },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun getNavData(): Result<NavData> {
        return try {
            Result.Success(client.getNavigation().toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun getAnimeDetail(url: String): Result<AnimeDetail> {
        return try {
            val details = client.getAnimeDetails(url)
            Result.Success(details.toDomain(totalCommentPages = 1))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun search(query: String, page: Int): Result<List<AnimePreview>> {
        return try {
            Result.Success(
                client.searchAnime(query = query, page = page)
                    .items
                    .map { it.toDomain() },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun submitRating(newsId: Int, rating: Int): Result<Double> {
        return try {
            Result.Success(client.voteAnime(newsId = newsId, rating = rating).rating ?: 0.0)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

    override suspend fun getRandomAnimeUrl(): Result<String> {
        return try {
            val anime = client.getRandomAnime()
                ?: return Result.Error(message = "No link in random post response")
            Result.Success(anime.url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e, e.message)
        }
    }

}
