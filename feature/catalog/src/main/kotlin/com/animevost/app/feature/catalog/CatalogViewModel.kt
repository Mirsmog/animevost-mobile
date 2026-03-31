package com.animevost.app.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.usecase.GetNavDataUseCase
import com.animevost.app.core.domain.util.onError
import com.animevost.app.core.domain.util.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val getNavDataUseCase: GetNavDataUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadNavData()
    }

    fun selectTab(tab: CatalogTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun loadNavData() {
        _uiState.update { it.copy(isLoadingNav = true) }
        viewModelScope.launch {
            getNavDataUseCase()
                .onSuccess { navData ->
                    _uiState.update {
                        it.copy(
                            genres = if (navData.genres.isNotEmpty()) navData.genres else CatalogDefaults.genres,
                            years = if (navData.years.isNotEmpty()) navData.years else CatalogDefaults.years,
                            isLoadingNav = false,
                        )
                    }
                }
                .onError { _, _ ->
                    _uiState.update { it.copy(isLoadingNav = false) }
                }
        }
    }
}
