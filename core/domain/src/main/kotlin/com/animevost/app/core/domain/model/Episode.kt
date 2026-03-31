package com.animevost.app.core.domain.model

/** A single playable episode with its video identifier and optional thumbnail. */
data class Episode(
    val name: String,
    /** Opaque identifier passed to [VideoRepository] to resolve playback URLs. */
    val videoId: String,
    val thumbnailUrl: String,
)
