package com.docuwallet.app.data.repository

import com.docuwallet.app.data.remote.firebase.DocumentData
import com.docuwallet.app.data.remote.firebase.FirebaseDocumentService
import com.docuwallet.app.domain.model.Category
import com.docuwallet.app.domain.model.Document
import com.docuwallet.app.utils.DateUtils
import com.docuwallet.app.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class DocumentRepository(
    private val documentService: FirebaseDocumentService = FirebaseDocumentService()
) {

    fun saveDocument(document: Document): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())

            val documentData = DocumentData(
                userId = document.userId,
                name = document.name,
                category = document.category.name,
                expirationDate = document.expirationDate,
                fileUrl = document.fileUrl,
                createdAt = document.createdAt,
                updatedAt = document.updatedAt,
                accessCount = document.accessCount,
                isFavorite = document.isFavorite,
                fileSizeBytes = document.fileSizeBytes
            )

            val result = documentService.saveDocument(documentData)

            result.onSuccess { documentId ->
                emit(Resource.Success(documentId))
            }.onFailure { e ->
                emit(Resource.Error(e.message ?: "Error al guardar documento"))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }

    fun updateDocument(documentId: String, document: Document): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            val documentData = DocumentData(
                id = documentId,
                userId = document.userId,
                name = document.name,
                category = document.category.name,
                expirationDate = document.expirationDate,
                fileUrl = document.fileUrl,
                createdAt = document.createdAt,
                updatedAt = System.currentTimeMillis(),
                accessCount = document.accessCount,
                isFavorite = document.isFavorite,
                fileSizeBytes = document.fileSizeBytes
            )

            val result = documentService.updateDocument(documentId, documentData)

            result.onSuccess {
                emit(Resource.Success(Unit))
            }.onFailure { e ->
                emit(Resource.Error(e.message ?: "Error al actualizar documento"))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }

    fun deleteDocument(documentId: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            val result = documentService.deleteDocument(documentId)

            result.onSuccess {
                emit(Resource.Success(Unit))
            }.onFailure { e ->
                emit(Resource.Error(e.message ?: "Error al eliminar documento"))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }

    fun getDocumentById(documentId: String): Flow<Resource<Document?>> = flow {
        try {
            emit(Resource.Loading())

            val result = documentService.getDocumentById(documentId)

            result.onSuccess { documentData ->
                val document = documentData?.let { mapToDocument(it) }
                emit(Resource.Success(document))
            }.onFailure { e ->
                emit(Resource.Error(e.message ?: "Error al obtener documento"))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }

    fun getAllDocuments(): Flow<List<Document>> {
        return documentService.getAllDocuments().map { documentsList ->
            documentsList.map { mapToDocument(it) }
        }
    }

    fun searchDocuments(query: String): Flow<Resource<List<Document>>> = flow {
        try {
            emit(Resource.Loading())

            val result = documentService.searchDocuments(query)

            result.onSuccess { documentsList ->
                val documents = documentsList.map { mapToDocument(it) }
                emit(Resource.Success(documents))
            }.onFailure { e ->
                emit(Resource.Error(e.message ?: "Error al buscar documentos"))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }

    fun getDocumentsByCategory(category: Category): Flow<Resource<List<Document>>> = flow {
        try {
            emit(Resource.Loading())

            val result = documentService.getDocumentsByCategory(category.name)

            result.onSuccess { documentsList ->
                val documents = documentsList.map { mapToDocument(it) }
                emit(Resource.Success(documents))
            }.onFailure { e ->
                emit(Resource.Error(e.message ?: "Error al obtener documentos"))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }

    fun toggleFavorite(documentId: String, isFavorite: Boolean): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            val result = documentService.toggleFavorite(documentId, isFavorite)

            result.onSuccess {
                emit(Resource.Success(Unit))
            }.onFailure { e ->
                emit(Resource.Error(e.message ?: "Error al actualizar favorito"))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }

    private fun mapToDocument(documentData: DocumentData): Document {
        val daysRemaining = DateUtils.calculateDaysRemaining(documentData.expirationDate)
        val alertLevel = DateUtils.getAlertLevel(daysRemaining)

        return Document(
            id = documentData.id,
            userId = documentData.userId,
            name = documentData.name,
            category = Category.fromString(documentData.category),
            expirationDate = documentData.expirationDate,
            fileUrl = documentData.fileUrl,
            createdAt = documentData.createdAt,
            updatedAt = documentData.updatedAt,
            accessCount = documentData.accessCount,
            isFavorite = documentData.isFavorite,
            fileSizeBytes = documentData.fileSizeBytes,
            daysRemaining = daysRemaining,
            alertLevel = alertLevel
        )
    }
}