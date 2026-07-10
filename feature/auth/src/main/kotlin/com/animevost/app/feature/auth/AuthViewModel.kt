package com.animevost.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animevost.app.core.domain.usecase.LoginUseCase
import com.animevost.app.core.domain.usecase.RegisterUseCase
import com.animevost.app.core.domain.usecase.SyncFavoritesUseCase
import com.animevost.app.core.domain.util.InputValidator
import com.animevost.app.core.domain.util.ValidationResult
import com.animevost.app.core.domain.util.isValid
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val email: String = "",
    val passwordConfirmation: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val registrationStatus: RegistrationUiStatus = RegistrationUiStatus.Idle,
)

enum class RegistrationUiStatus {
    Idle,
    Active,
    AwaitingEmailActivation,
}

sealed interface AuthEvent {
    data class UpdateUsername(val value: String) : AuthEvent
    data class UpdatePassword(val value: String) : AuthEvent
    data class UpdateEmail(val value: String) : AuthEvent
    data class UpdatePasswordConfirmation(val value: String) : AuthEvent
    data object Login : AuthEvent
    data object Register : AuthEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val syncFavoritesUseCase: SyncFavoritesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.UpdateUsername -> updateInput { copy(username = event.value) }
            is AuthEvent.UpdatePassword -> updateInput { copy(password = event.value) }
            is AuthEvent.UpdateEmail -> updateInput { copy(email = event.value) }
            is AuthEvent.UpdatePasswordConfirmation -> updateInput {
                copy(passwordConfirmation = event.value)
            }
            is AuthEvent.Login -> login()
            is AuthEvent.Register -> register()
        }
    }

    private fun updateInput(transform: AuthUiState.() -> AuthUiState) {
        _uiState.update { state -> state.transform().copy(error = null) }
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Ошибка входа")
                }
            }
        }
    }

    private fun register() {
        val state = _uiState.value
        val validationError = firstValidationError(
            InputValidator.validateUsername(state.username),
            InputValidator.validateEmail(state.email),
            InputValidator.validatePassword(state.password),
            InputValidator.validatePasswordConfirmation(state.password, state.passwordConfirmation),
        )
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val user = registerUseCase(
                    username = state.username.trim(),
                    password = state.password,
                    email = state.email.trim(),
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = user.isLoggedIn,
                        registrationStatus = if (user.isLoggedIn) {
                            RegistrationUiStatus.Active
                        } else {
                            RegistrationUiStatus.AwaitingEmailActivation
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Ошибка регистрации")
                }
            }
        }
    }

    private fun firstValidationError(vararg results: ValidationResult): String? =
        results.firstNotNullOfOrNull { result ->
            (result as? ValidationResult.Invalid)?.reason
        }
}
