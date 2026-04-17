package com.animevost.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.model.BetaFeature
import com.animevost.app.core.domain.repository.FeatureFlagsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val featureFlagsRepository: FeatureFlagsRepository,
) : ViewModel() {

    private val _betaFeatures = MutableStateFlow<Map<BetaFeature, Boolean>>(emptyMap())
    val betaFeatures: StateFlow<Map<BetaFeature, Boolean>> = _betaFeatures.asStateFlow()

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
}
