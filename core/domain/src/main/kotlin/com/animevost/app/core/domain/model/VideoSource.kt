package com.animevost.app.core.domain.model

/**
 * A playback URL paired with its quality label and optional direct-download link.
 *
 * @property quality Human-readable quality label, e.g. `"HD (720p)"` or `"SD (480p)"`.
 * @property url Streaming URL (HLS or direct).
 * @property downloadUrl Direct download URL; may be blank when not available.
 */
data class VideoSource(
    val quality: String,
    val url: String,
    val downloadUrl: String,
)
