package com.docuwallet.app.presentacion.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileUiState(
    val userName: String? = null,
    val userEmail: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaveSuccess: Boolean = false
)

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        _uiState.update { it.copy(isLoading = true) }
        val user = auth.currentUser
        if (user != null) {
            _uiState.update {
                it.copy(
                    userName = user.displayName,
                    userEmail = user.email,
                    isLoading = false,
                    isSaveSuccess = false, // Resetear al cargar
                    error = null
                )
            }
        } else {
            _uiState.update { it.copy(isLoading = false, error = "Usuario no autenticado.") }
        }
    }

    fun updateUserName(newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSaveSuccess = false) }
            val user = auth.currentUser ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Error: Usuario no autenticado.") }
                return@launch
            }

            try {
                val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(newName).build()
                user.updateProfile(profileUpdates).await()

                firestore.collection("users").document(user.uid).update("name", newName).await()

                _uiState.update { it.copy(isLoading = false, isSaveSuccess = true, userName = newName) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Ocurrió un error al actualizar el perfil.") }
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSaveSuccess = false) }
            val user = auth.currentUser ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Error: Usuario no autenticado.") }
                return@launch
            }

            try {
                // 1. Re-autenticar al usuario
                val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)
                user.reauthenticate(credential).await()

                // 2. Cambiar la contraseña
                user.updatePassword(newPass).await()

                _uiState.update { it.copy(isLoading = false, isSaveSuccess = true) }

            } catch (e: Exception) {
                // Manejar errores comunes
                val errorMessage = when (e) {
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "La contraseña actual es incorrecta."
                    else -> e.message ?: "Ocurrió un error al cambiar la contraseña."
                }
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }
}