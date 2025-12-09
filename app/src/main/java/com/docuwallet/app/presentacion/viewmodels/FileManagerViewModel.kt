package com.docuwallet.app.presentacion.viewmodels

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuwallet.app.data.local.entity.DocumentEntity
import com.docuwallet.app.data.repository.DocumentRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File


@OptIn(ExperimentalCoroutinesApi::class)
class FileManagerViewModel(
    private val repository: DocumentRepository,
    private val context: Context
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid ?: ""


    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDocuments = MutableStateFlow<Set<String>>(emptySet())
    val selectedDocuments: StateFlow<Set<String>> = _selectedDocuments.asStateFlow()

    val documents: StateFlow<List<DocumentEntity>> = _searchQuery
        .flatMapLatest { query ->
            // Se obtiene el flujo de documentos del repositorio.
            repository.getUserDocuments(userId).map { documents ->
                if (query.isBlank()) {
                    // Si la búsqueda está vacía, devuelve la lista completa.
                    documents
                } else {
                    // Si hay texto, filtra la lista basándose en el nombre, categoría o ID.
                    // La comparación `ignoreCase = true` cumple el requisito de ser insensible a mayúsculas/minúsculas.
                    documents.filter { doc ->
                        doc.name.contains(query, ignoreCase = true) ||
                                doc.category.contains(query, ignoreCase = true) ||
                                doc.id.contains(query, ignoreCase = true)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }


    fun toggleDocumentSelection(documentId: String) {
        _selectedDocuments.update { currentSelection ->
            if (currentSelection.contains(documentId)) {
                currentSelection - documentId
            } else {
                currentSelection + documentId
            }
        }
    }


    fun selectAllDocuments() {
        _selectedDocuments.value = documents.value.map { it.id }.toSet()
    }


    fun clearSelection() {
        _selectedDocuments.value = emptySet()
    }


    fun deleteSelectedDocuments() {
        viewModelScope.launch {
            _selectedDocuments.value.forEach { documentId ->
                repository.deleteDocument(documentId)
            }
            clearSelection()
        }
    }


    fun shareSelectedDocuments() {
        viewModelScope.launch {
            val selectedIds = _selectedDocuments.value
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
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                // Placeholder for analytics
                // trackDocumentShared(selectedIds)

                context.startActivity(Intent.createChooser(intent, "Compartir Documentos").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }

            clearSelection()
        }
    }


    fun toggleFavoriteForSelected() {
        viewModelScope.launch {
            val selectedIds = _selectedDocuments.value
            println("Marcando como favoritos: $selectedIds")
            clearSelection()
        }
    }


    suspend fun getShareUrl(documentId: String): String? {

        return repository.getDocumentById(documentId)?.cloudinaryUrl
    }
}
