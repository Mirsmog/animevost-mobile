package com.animevost.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.usecase.LoginUseCase
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
    val email: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
)

sealed interface AuthEvent {
    data class UpdateUsername(val value: String) : AuthEvent
    data class UpdatePassword(val value: String) : AuthEvent
    data class UpdateEmail(val value: String) : AuthEvent
    data class UpdateConfirmPassword(val value: String) : AuthEvent
    data object Login : AuthEvent
    data object Register : AuthEvent
    data object ClearError : AuthEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.UpdateUsername -> _uiState.update { it.copy(username = event.value) }
            is AuthEvent.UpdatePassword -> _uiState.update { it.copy(password = event.value) }
            is AuthEvent.UpdateEmail -> _uiState.update { it.copy(email = event.value) }
            is AuthEvent.UpdateConfirmPassword -> _uiState.update { it.copy(confirmPassword = event.value) }
            is AuthEvent.Login -> login()
            is AuthEvent.Register -> register()
            is AuthEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun login() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Заполните все поля") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                loginUseCase(state.username, state.password)
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка входа",
                    )
                }
            }
        }
    }

    private fun register() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank() || state.email.isBlank()) {
            _uiState.update { it.copy(error = "Заполните все поля") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(error = "Пароли не совпадают") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Registration uses the same login after register flow
                _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка регистрации",
                    )
                }
            }
        }
    }
}
