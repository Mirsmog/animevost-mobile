package com.animevost.app.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.animevost.app.core.ui.R as UiR

private val FieldShape = RoundedCornerShape(14.dp)
private val ButtonShape = RoundedCornerShape(14.dp)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showRegisterDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoginSuccess()
    }

    if (showRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text("Регистрация") },
            text = {
                Text(
                    text = "Регистрация через приложение пока в разработке.\n\n" +
                        "Зарегистрируйтесь на сайте animevost.info, затем войдите в приложении.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { showRegisterDialog = false }) {
                    Text("Понятно")
                }
            },
        )
    }

    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Logo
            Image(
                painter = painterResource(id = UiR.drawable.animevost_logo),
                contentDescription = "AnimeVost",
                modifier = Modifier.size(72.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "AnimeVost",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Войдите в свой аккаунт",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(48.dp))

            TextField(
                value = state.username,
                onValueChange = { viewModel.onEvent(AuthEvent.UpdateUsername(it)) },
                label = { Text("Имя пользователя") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                shape = FieldShape,
                colors = fieldColors,
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(AuthEvent.UpdatePassword(it)) },
                label = { Text("Пароль") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
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
                shape = FieldShape,
                colors = fieldColors,
            )

            if (state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { viewModel.onEvent(AuthEvent.Login) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isLoading,
                shape = ButtonShape,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Войти",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(onClick = { showRegisterDialog = true }) {
                Text(
                    text = "Нет аккаунта? Зарегистрироваться",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
