package com.docuwallet.app.presentacion.views.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import com.docuwallet.app.utils.Constants

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf(false) }
    var passwordMismatch by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState.isSuccess) {
        if (authState.isSuccess && authState.user != null) {
            onNavigateToHome()
        }
    }

    LaunchedEffect(authState.error) {
        if (authState.error != null) {
            nameError = false
            emailError = false
            passwordError = false
            confirmPasswordError = false
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
            text = "Crear Cuenta",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Únete a DocuWallet y organiza tus documentos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        CustomTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = false
                viewModel.clearError()
            },
            label = "Nombre completo",
            leadingIcon = Icons.Default.Person,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            isError = nameError,
            errorMessage = if (nameError) "El nombre es requerido" else null
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                passwordMismatch = false
                viewModel.clearError()
            },
            label = "Contraseña",
            leadingIcon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            isPassword = true,
            isError = passwordError,
            errorMessage = if (passwordError) "La contraseña debe tener al menos ${Constants.MIN_PASSWORD_LENGTH} caracteres" else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                confirmPasswordError = false
                passwordMismatch = false
                viewModel.clearError()
            },
            label = "Confirmar contraseña",
            leadingIcon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            isError = confirmPasswordError || passwordMismatch,
            errorMessage = when {
                confirmPasswordError -> "Confirma tu contraseña"
                passwordMismatch -> "Las contraseñas no coinciden"
                else -> null
            },
            onImeAction = {  // ✅ CAMBIADO: onDone → onImeAction
                val validation = validateInputs(name, email, password, confirmPassword)
                if (validation.isValid) {
                    viewModel.register(email.trim(), password, name.trim())
                } else {
                    nameError = validation.nameError
                    emailError = validation.emailError
                    passwordError = validation.passwordError
                    confirmPasswordError = validation.confirmPasswordError
                    passwordMismatch = validation.passwordMismatch
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
            text = "Registrarse",
            onClick = {
                val validation = validateInputs(name, email, password, confirmPassword)
                if (validation.isValid) {
                    viewModel.register(email.trim(), password, name.trim())
                } else {
                    nameError = validation.nameError
                    emailError = validation.emailError
                    passwordError = validation.passwordError
                    confirmPasswordError = validation.confirmPasswordError
                    passwordMismatch = validation.passwordMismatch
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
                text = "¿Ya tienes cuenta? ",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Inicia Sesión",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

private data class ValidationResult(
    val isValid: Boolean,
    val nameError: Boolean = false,
    val emailError: Boolean = false,
    val passwordError: Boolean = false,
    val confirmPasswordError: Boolean = false,
    val passwordMismatch: Boolean = false
)

private fun validateInputs(
    name: String,
    email: String,
    password: String,
    confirmPassword: String
): ValidationResult {
    val isNameValid = name.isNotBlank()
    val isEmailValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.length >= Constants.MIN_PASSWORD_LENGTH
    val isConfirmPasswordValid = confirmPassword.isNotBlank()
    val passwordsMatch = password == confirmPassword

    return ValidationResult(
        isValid = isNameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid && passwordsMatch,
        nameError = !isNameValid,
        emailError = !isEmailValid,
        passwordError = !isPasswordValid,
        confirmPasswordError = !isConfirmPasswordValid,
        passwordMismatch = !passwordsMatch && isConfirmPasswordValid
    )
}