package com.docuwallet.app.presentacion.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuwallet.app.data.local.entity.DocumentEntity
import com.docuwallet.app.data.repository.DocumentRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FileManagerViewModel(
    private val repository: DocumentRepository
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
         
            println("Compartiendo documentos: $selectedIds")
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
