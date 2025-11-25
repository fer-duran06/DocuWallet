package com.docuwallet.app.presentacion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuwallet.app.data.repository.AuthRepository
import com.docuwallet.app.domain.model.User
import com.docuwallet.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkUserLoggedIn()
    }

    private fun checkUserLoggedIn() {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            _authState.value = AuthState(user = user, isSuccess = true)
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            authRepository.login(email, password).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _authState.value = AuthState(isLoading = true)
                    }
                    is Resource.Success -> {
                        _authState.value = AuthState(
                            user = result.data,
                            isSuccess = true,
                            isLoading = false
                        )
                    }
                    is Resource.Error -> {
                        _authState.value = AuthState(
                            error = result.message ?: "Error al iniciar sesión",
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            authRepository.register(email, password, name).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _authState.value = AuthState(isLoading = true)
                    }
                    is Resource.Success -> {
                        _authState.value = AuthState(
                            user = result.data,
                            isSuccess = true,
                            isLoading = false
                        )
                    }
                    is Resource.Error -> {
                        _authState.value = AuthState(
                            error = result.message ?: "Error al registrar usuario",
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _authState.value = AuthState()
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            authRepository.resetPassword(email).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _authState.value = _authState.value.copy(isLoading = true)
                    }
                    is Resource.Success -> {
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _authState.value = _authState.value.copy(
                            error = result.message ?: "Error al enviar correo",
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }
}