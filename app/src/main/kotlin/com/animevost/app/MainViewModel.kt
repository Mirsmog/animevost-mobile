package com.animevost.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.repository.AuthRepository
import com.animevost.app.core.domain.usecase.SyncFavoritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncFavoritesUseCase: SyncFavoritesUseCase,
) : ViewModel() {

    init {
        viewModelScope.launch {
            try {
                if (authRepository.isLoggedIn()) {
                    syncFavoritesUseCase()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync favorites on app open")
            }
        }
    }
}
