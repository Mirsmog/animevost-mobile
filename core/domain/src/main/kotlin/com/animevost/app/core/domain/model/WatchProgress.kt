package com.animevost.app.core.domain.model

/** Stores playback progress for a single episode. */
data class WatchProgress(
    val animeId: Int,
    val episodeVideoId: String,
    val episodeName: String,
    val episodeIndex: Int,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /** True when the user has watched at least 85% of the episode. */
    val isCompleted: Boolean
        get() = durationMs > 0 && positionMs.toFloat() / durationMs >= 0.85f
}
