package com.animevost.app.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RegisterScreen(
    onRegistrationSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val autofillManager = LocalAutofillManager.current
    var showActivationNotice by remember { mutableStateOf(false) }

    BackHandler {
        if (!showActivationNotice) autofillManager?.cancel()
        onNavigateToLogin()
    }

    LaunchedEffect(state.registrationStatus) {
        when (state.registrationStatus) {
            RegistrationUiStatus.Active -> {
                autofillManager?.commit()
                onRegistrationSuccess()
            }
            RegistrationUiStatus.AwaitingEmailActivation -> {
                autofillManager?.commit()
                showActivationNotice = true
            }
            RegistrationUiStatus.Idle -> Unit
        }
    }

    if (showActivationNotice) {
        ActivationNotice(
            email = state.email,
            onNavigateToLogin = onNavigateToLogin,
        )
        return
    }

    AuthScreenLayout(
        title = "Создание аккаунта",
        subtitle = "Зарегистрируйтесь в AnimeVost",
    ) {
        AuthTextField(
            value = state.username,
            onValueChange = { viewModel.onEvent(AuthEvent.UpdateUsername(it)) },
            label = "Имя пользователя",
            contentType = ContentType.NewUsername,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        AuthTextField(
            value = state.email,
            onValueChange = { viewModel.onEvent(AuthEvent.UpdateEmail(it)) },
            label = "Электронная почта",
            contentType = ContentType.EmailAddress,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        AuthPasswordField(
            value = state.password,
            onValueChange = { viewModel.onEvent(AuthEvent.UpdatePassword(it)) },
            label = "Пароль",
            contentType = ContentType.NewPassword,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        AuthPasswordField(
            value = state.passwordConfirmation,
            onValueChange = { viewModel.onEvent(AuthEvent.UpdatePasswordConfirmation(it)) },
            label = "Повторите пароль",
            contentType = ContentType.NewPassword,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.onEvent(AuthEvent.Register)
                },
            ),
        )

        AuthErrorMessage(state.error)

        Spacer(modifier = Modifier.height(28.dp))

        AuthSubmitButton(
            text = "Зарегистрироваться",
            isLoading = state.isLoading,
            onClick = {
                focusManager.clearFocus()
                viewModel.onEvent(AuthEvent.Register)
            },
        )

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(
            onClick = {
                autofillManager?.cancel()
                onNavigateToLogin()
            },
        ) {
            Text(
                text = "Уже есть аккаунт? Войти",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ActivationNotice(
    email: String,
    onNavigateToLogin: () -> Unit,
) {
    AuthScreenLayout(
        title = "Проверьте почту",
        subtitle = "Регистрация почти завершена",
    ) {
        Icon(
            imageVector = Icons.Filled.MarkEmailRead,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(56.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Мы отправили письмо с подтверждением на $email. " +
                "Откройте ссылку из письма, после чего сможете войти в аккаунт.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                text = "Перейти ко входу",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
