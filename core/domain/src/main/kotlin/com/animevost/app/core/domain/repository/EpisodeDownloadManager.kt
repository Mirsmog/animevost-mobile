package com.animevost.app.core.domain.repository

interface EpisodeDownloadManager {
    fun enqueue(url: String, fileName: String, title: String): Long
}
