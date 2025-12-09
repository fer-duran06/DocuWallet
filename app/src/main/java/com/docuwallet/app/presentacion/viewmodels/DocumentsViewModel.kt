package com.docuwallet.app.presentacion.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docuwallet.app.DocuWalletApplication
import com.docuwallet.app.data.local.entity.DocumentEntity
import com.docuwallet.app.data.repository.DocumentRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

data class DocumentsUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCategory: String = "Todos",
    val searchQuery: String = "",
    val selectedDocuments: Set<String> = emptySet()
)

class DocumentsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository = (application as DocuWalletApplication).repository
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    private val loadTrigger = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            combine(
                _uiState.map { it.selectedCategory }.distinctUntilChanged(),
                _uiState.map { it.searchQuery }.distinctUntilChanged(),
                loadTrigger
            ) { category, query, _ ->
                Triple(category, query, auth.currentUser?.uid)
            }
            .flatMapLatest { (category, query, userId) ->
                getDocumentsFlow(userId, category, query)
            }
            .collect { (error, docs) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        documents = docs,
                        error = error
                    )
                }
            }
        }
    }

    private fun getDocumentsFlow(userId: String?, category: String, query: String): Flow<Pair<String?, List<DocumentEntity>>> = flow {
        if (userId == null) {
            emit(Pair("Usuario no autenticado", emptyList()))
            return@flow
        }

        try {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val documentsFlowSource = when (category) {
                "Todos" -> repository.getUserDocuments(userId)
                "Favoritos" -> repository.getFavoriteDocuments(userId)
                else -> repository.getDocumentsByCategory(userId, category)
            }

            documentsFlowSource.collect { documents ->
                val filteredList = if (query.isBlank()) {
                    documents
                } else {
                    documents.filter {
                        it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
                    }
                }
                emit(Pair(null, filteredList))
            }
        } catch (e: Exception) {
            emit(Pair(e.message ?: "Error inesperado al cargar documentos", emptyList()))
        }
    }

    fun selectCategory(category: String) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                searchQuery = "",
                selectedDocuments = emptySet()
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleDocumentSelection(documentId: String) {
        _uiState.update { currentState ->
            val selected = currentState.selectedDocuments
            val newSelection = if (selected.contains(documentId)) selected - documentId else selected + documentId
            currentState.copy(selectedDocuments = newSelection)
        }
    }

    fun selectAllDocuments() {
        _uiState.update { currentState ->
            val allIds = currentState.documents.map { it.id }.toSet()
            currentState.copy(selectedDocuments = allIds)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedDocuments = emptySet()) }
    }

    fun deleteSelectedDocuments() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedDocuments
            if (selectedIds.isEmpty()) return@launch
            try {
                selectedIds.forEach { documentId ->
                    repository.deleteDocument(documentId)
                }
                clearSelection()
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Error al eliminar: ${e.message}") }
            }
        }
    }

    fun toggleFavoriteForSelected() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedDocuments
            if (selectedIds.isEmpty()) return@launch
            try {
                val documentsToToggle = _uiState.value.documents.filter { it.id in selectedIds }
                documentsToToggle.forEach { doc ->
                    repository.toggleFavorite(doc.id, !doc.isFavorite)
                }
                clearSelection()
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Error al marcar como favorito: ${e.message}") }
            }
        }
    }

    fun shareSelectedDocuments() {
        viewModelScope.launch {
            try {
                val selectedIds = _uiState.value.selectedDocuments
                if (selectedIds.isEmpty()) return@launch

                val shareableUrls = selectedIds.mapNotNull { getShareUrl(it) }

                if (shareableUrls.isNotEmpty()) {
                    val shareText = if (shareableUrls.size == 1) {
                        "Link para el documento: ${shareableUrls.first()}"
                    } else {
                        "Links para los documentos:\n${shareableUrls.joinToString("\n")}"
                    }

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    val chooser = Intent.createChooser(intent, "Compartir Documentos").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    getApplication<Application>().startActivity(chooser)
                }
                clearSelection()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo compartir: ${e.message}") }
            }
        }
    }

    private suspend fun getShareUrl(documentId: String): String? {
        return repository.getDocumentById(documentId)?.cloudinaryUrl
    }

    fun toggleFavorite(documentId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(documentId, !isFavorite)
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Error al marcar como favorito: ${e.message}") }
            }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            try {
                repository.deleteDocument(documentId)
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Error al eliminar: ${e.message}") }
            }
        }
    }
}