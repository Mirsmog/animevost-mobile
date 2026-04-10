package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.UpdateInfo
import java.io.File

interface UpdateRepository {
    /** Returns [UpdateInfo] if a newer release exists on GitHub, null otherwise. */
    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo?

    /** Downloads the APK at [url] to the app cache dir, reporting [0-100] progress. */
    suspend fun downloadUpdate(url: String, onProgress: (Int) -> Unit): File
}
