package com.docuwallet.app.presentacion.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docuwallet.app.DocuWalletApplication
import com.docuwallet.app.data.local.entity.DocumentEntity
import com.docuwallet.app.data.repository.DocumentRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DocumentsUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: String = "Todos"
)

class DocumentsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository = (application as DocuWalletApplication).repository
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    fun loadDocuments(category: String = "Todos") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedCategory = category)

            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Usuario no autenticado"
                )
                return@launch
            }

            try {
                val documentsFlow = when (category) {
                    "Todos" -> repository.getUserDocuments(userId)
                    "Favoritos" -> repository.getFavoriteDocuments(userId)
                    else -> repository.getDocumentsByCategory(userId, category)
                }

                documentsFlow.collect { documents ->
                    _uiState.value = _uiState.value.copy(
                        documents = documents,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun toggleFavorite(documentId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(documentId, !isFavorite)
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            repository.deleteDocument(documentId)
        }
    }

    fun syncDocuments() {
        viewModelScope.launch {
            repository.syncPendingDocuments()
        }
    }
}