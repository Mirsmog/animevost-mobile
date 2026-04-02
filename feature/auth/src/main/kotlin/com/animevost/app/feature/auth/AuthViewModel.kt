package com.animevost.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.usecase.LoginUseCase
import com.animevost.app.core.domain.usecase.SyncFavoritesUseCase
import com.animevost.app.core.domain.util.InputValidator
import com.animevost.app.core.domain.util.ValidationResult
import com.animevost.app.core.domain.util.isValid
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
)

sealed interface AuthEvent {
    data class UpdateUsername(val value: String) : AuthEvent
    data class UpdatePassword(val value: String) : AuthEvent
    data object Login : AuthEvent
    data object ClearError : AuthEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val syncFavoritesUseCase: SyncFavoritesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.UpdateUsername -> _uiState.update { it.copy(username = event.value) }
            is AuthEvent.UpdatePassword -> _uiState.update { it.copy(password = event.value) }
            is AuthEvent.Login -> login()
            is AuthEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun login() {
        val state = _uiState.value

        val usernameResult = InputValidator.validateUsername(state.username)
        if (!usernameResult.isValid) {
            _uiState.update { it.copy(error = (usernameResult as ValidationResult.Invalid).reason) }
            return
        }
        val passwordResult = InputValidator.validatePassword(state.password)
        if (!passwordResult.isValid) {
            _uiState.update { it.copy(error = (passwordResult as ValidationResult.Invalid).reason) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                loginUseCase(state.username, state.password)
                // Fire-and-forget: sync favourites in background, don't block login UI
                launch { syncFavoritesUseCase() }
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Ошибка входа")
                }
            }
        }
    }
}
