package com.animevost.app.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val autofillManager = LocalAutofillManager.current

    BackHandler {
        autofillManager?.cancel()
        onBack()
    }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            autofillManager?.commit()
            onLoginSuccess()
        }
    }

    AuthScreenLayout(
        title = "Войдите в свой аккаунт",
    ) {
        AuthTextField(
            value = state.username,
            onValueChange = { viewModel.onEvent(AuthEvent.UpdateUsername(it)) },
            label = "Имя пользователя",
            contentType = ContentType.Username,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        AuthPasswordField(
            value = state.password,
            onValueChange = { viewModel.onEvent(AuthEvent.UpdatePassword(it)) },
            label = "Пароль",
            contentType = ContentType.Password,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.onEvent(AuthEvent.Login)
                },
            ),
        )

        AuthErrorMessage(state.error)

        Spacer(modifier = Modifier.height(28.dp))

        AuthSubmitButton(
            text = "Войти",
            isLoading = state.isLoading,
            onClick = {
                focusManager.clearFocus()
                viewModel.onEvent(AuthEvent.Login)
            },
        )

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(
            onClick = {
                autofillManager?.cancel()
                onNavigateToRegister()
            },
        ) {
            Text(
                text = "Нет аккаунта? Зарегистрироваться",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
