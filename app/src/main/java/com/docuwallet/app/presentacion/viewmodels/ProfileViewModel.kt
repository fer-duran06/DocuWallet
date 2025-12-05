package com.docuwallet.app.presentacion.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docuwallet.app.data.repository.DocumentRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    // Información del usuario
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val memberSince: String = "",
    val photoUrl: String? = null,

    // Estadísticas
    val totalDocuments: Int = 0,
    val totalStorageMB: Double = 0.0,
    val totalAccesses: Int = 0
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(application.applicationContext)
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuario no autenticado"
                    )
                    return@launch
                }

                // Información del usuario desde Firebase Auth
                val userName = currentUser.displayName ?: "Usuario"
                val userEmail = currentUser.email ?: ""
                val memberSince = formatCreationDate(currentUser.metadata?.creationTimestamp ?: 0)
                val photoUrl = currentUser.photoUrl?.toString()

                // Estadísticas desde Room Database
                val stats = repository.getStatistics(currentUser.uid)
                val totalDocuments = stats.totalDocuments
                val totalStorageMB = stats.totalStorageBytes / (1024.0 * 1024.0)

                // Calcular total de accesos
                var totalAccesses = 0
                repository.getUserDocuments(currentUser.uid).collect { documents ->
                    totalAccesses = documents.sumOf { it.accessCount }

                    _uiState.value = ProfileUiState(
                        isLoading = false,
                        error = null,
                        userName = userName,
                        userEmail = userEmail,
                        userPhone = "", // Firebase Auth no guarda teléfono por defecto
                        memberSince = memberSince,
                        photoUrl = photoUrl,
                        totalDocuments = totalDocuments,
                        totalStorageMB = totalStorageMB,
                        totalAccesses = totalAccesses
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar perfil"
                )
            }
        }
    }

    private fun formatCreationDate(timestamp: Long): String {
        if (timestamp == 0L) return "Fecha desconocida"

        val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        return sdf.format(Date(timestamp))
    }

    fun refreshProfile() {
        loadProfileData()
    }
}