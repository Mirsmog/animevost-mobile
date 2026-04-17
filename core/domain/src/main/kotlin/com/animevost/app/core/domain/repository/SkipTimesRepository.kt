package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.SkipInterval

interface SkipTimesRepository {
    suspend fun getSkipTimes(
        animeId: Int,
        episodeNumber: Int,
        titleOriginal: String,
        titleAlternative: String,
    ): List<SkipInterval>
}
