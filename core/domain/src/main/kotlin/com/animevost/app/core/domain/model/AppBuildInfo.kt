package com.animevost.app.core.domain.model

enum class AppDistribution {
    PROD,
    DEV,
}

data class AppBuildInfo(
    val distribution: AppDistribution,
    val versionName: String,
    val versionCode: Int,
    val inAppUpdatesEnabled: Boolean,
    val backgroundNotificationsEnabled: Boolean,
) {
    val isDevelopment: Boolean
        get() = distribution == AppDistribution.DEV
}
