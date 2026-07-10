package com.animevost.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.BetaFeature
import com.animevost.app.core.domain.repository.FeatureFlagsRepository
import com.animevost.app.core.domain.repository.NotificationPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val featureFlagsRepository: FeatureFlagsRepository,
    private val notificationPreferencesRepository: NotificationPreferencesRepository,
) : ViewModel() {

    private val _betaFeatures = MutableStateFlow<Map<BetaFeature, Boolean>>(emptyMap())
    val betaFeatures: StateFlow<Map<BetaFeature, Boolean>> = _betaFeatures.asStateFlow()
    val favoriteNotificationsEnabled: StateFlow<Boolean> =
        notificationPreferencesRepository.favoriteNotificationsEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    init {
        viewModelScope.launch {
            val flows = BetaFeature.entries.map { feature ->
                featureFlagsRepository.isEnabled(feature)
            }
            combine(flows) { values ->
                BetaFeature.entries.zip(values.toList())
                    .associate { (feature, enabled) -> feature to enabled }
            }.collect { _betaFeatures.value = it }
        }
    }

    fun toggle(feature: BetaFeature, enabled: Boolean) {
        viewModelScope.launch {
            featureFlagsRepository.setEnabled(feature, enabled)
        }
    }

    fun setFavoriteNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferencesRepository.setFavoriteNotificationsEnabled(enabled)
        }
    }
}
