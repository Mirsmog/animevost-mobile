package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.SkipSegment
import com.animevost.app.core.domain.util.Result

/** Manages intro/outro skip segment data from AniSkip and local user edits. */
interface SkipRepository {
    /** Returns skip segments for [episodeName] of [animeId], preferring local user data. */
    suspend fun getSkipSegments(animeId: Int, episodeName: String): Result<List<SkipSegment>>

    /** Persists a user-defined or fetched [segment] for the given episode. */
    suspend fun saveSkipSegment(animeId: Int, episodeName: String, segment: SkipSegment): Result<Unit>
}
