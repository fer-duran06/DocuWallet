package com.docuwallet.app.presentacion.views.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuwallet.app.presentacion.viewmodel.AuthViewModel
import com.docuwallet.app.presentacion.views.components.CustomButton
import com.docuwallet.app.presentacion.views.components.CustomTextField

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState.isSuccess) {
        if (authState.isSuccess && authState.user != null) {
            onNavigateToHome()
        }
    }

    LaunchedEffect(authState.error) {
        if (authState.error != null) {
            emailError = false
            passwordError = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "DocuWallet",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Gestiona tus documentos de forma segura",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        CustomTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = false
                viewModel.clearError()
            },
            label = "Correo electrónico",
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            isError = emailError,
            errorMessage = if (emailError) "Ingresa un correo válido" else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = false
                viewModel.clearError()
            },
            label = "Contraseña",
            leadingIcon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            isError = passwordError,
            errorMessage = if (passwordError) "La contraseña es requerida" else null,
            onImeAction = {  // ✅ CAMBIADO: onDone → onImeAction
                if (validateInputs(email, password)) {
                    viewModel.login(email.trim(), password)
                } else {
                    emailError = email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    passwordError = password.isBlank()
                }
            }
        )

        if (authState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = authState.error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        CustomButton(
            text = "Iniciar Sesión",
            onClick = {
                if (validateInputs(email, password)) {
                    viewModel.login(email.trim(), password)
                } else {
                    emailError = email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    passwordError = password.isBlank()
                }
            },
            isLoading = authState.isLoading,
            enabled = !authState.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "¿No tienes cuenta? ",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onNavigateToRegister) {
                Text(
                    text = "Regístrate",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

private fun validateInputs(email: String, password: String): Boolean {
    val isEmailValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.isNotBlank()
    return isEmailValid && isPasswordValid
}