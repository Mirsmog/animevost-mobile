package com.animevost.app.core.domain.repository

import com.animevost.app.core.domain.model.BetaFeature
import kotlinx.coroutines.flow.Flow

interface FeatureFlagsRepository {
    fun isEnabled(feature: BetaFeature): Flow<Boolean>
    suspend fun setEnabled(feature: BetaFeature, enabled: Boolean)
}
