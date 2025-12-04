package com.docuwallet.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.docuwallet.app.data.local.dao.DocumentDao
import com.docuwallet.app.data.local.database.AppDatabase
import com.docuwallet.app.data.local.entity.DocumentEntity
import com.docuwallet.app.data.remote.firebase.FirebaseDocumentService
import com.docuwallet.app.data.remote.firebase.FirebaseStorageService
import com.docuwallet.app.domain.model.DocumentModel
import com.docuwallet.app.utils.PdfUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

class DocumentRepository(private val context: Context) {

    private val documentDao: DocumentDao = AppDatabase.getDatabase(context).documentDao()
    private val auth = FirebaseAuth.getInstance()
    private val storageService = FirebaseStorageService()
    private val documentService = FirebaseDocumentService()

    private val TAG = "DocumentRepository"

    /**
     * Obtener todos los documentos del usuario (desde Room)
     */
    fun getUserDocuments(userId: String): Flow<List<DocumentEntity>> {
        return documentDao.getUserDocuments(userId)
    }

    /**
     * Obtener documentos por categoría
     */
    fun getDocumentsByCategory(userId: String, category: String): Flow<List<DocumentEntity>> {
        return documentDao.getDocumentsByCategory(userId, category)
    }

    /**
     * Obtener documentos favoritos
     */
    fun getFavoriteDocuments(userId: String): Flow<List<DocumentEntity>> {
        return documentDao.getFavoriteDocuments(userId)
    }

    /**
     * Obtener documento por ID
     */
    suspend fun getDocumentById(documentId: String): DocumentEntity? {
        return try {
            Log.d(TAG, "Buscando documento con ID: $documentId")
            val doc = documentDao.getDocumentById(documentId)

            if (doc == null) {
                Log.w(TAG, "Documento no encontrado en Room: $documentId")
            } else {
                Log.d(TAG, "Documento encontrado - Nombre: ${doc.name}, Categoría: ${doc.category}")
            }

            doc
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener documento: $documentId", e)
            null
        }
    }

    /**
     * Incrementar contador de accesos
     */
    suspend fun incrementAccessCount(documentId: String) {
        try {
            documentDao.incrementAccessCount(documentId)
            Log.d(TAG, "Contador de accesos incrementado para: $documentId")
        } catch (e: Exception) {
            Log.e(TAG, "Error al incrementar contador", e)
        }
    }

    /**
     * Guardar documento completo (PDF + metadata)
     * Guarda primero en Room (local), luego intenta subir a Firebase
     */
    suspend fun saveDocument(
        imageUris: List<Uri>,
        name: String,
        category: String,
        notes: String,
        pageCount: Int
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            // 1. Generar PDF localmente
            val pdfFile = PdfUtils.generatePdfFromImages(
                context = context,
                imageUris = imageUris,
                documentName = name
            )

            if (!pdfFile.exists() || pdfFile.length() == 0L) {
                return Result.failure(Exception("Error al generar PDF"))
            }

            // 2. Crear entidad para Room
            val documentId = UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()

            val documentEntity = DocumentEntity(
                id = documentId,
                userId = userId,
                name = name,
                category = category,
                notes = notes,
                pdfLocalPath = pdfFile.absolutePath,
                pdfUrl = null,
                pdfFileName = pdfFile.name,
                fileSize = pdfFile.length(),
                pageCount = pageCount,
                createdAt = currentTime,
                updatedAt = currentTime,
                isSynced = false
            )

            // 3. Guardar en Room (local)
            documentDao.insertDocument(documentEntity)
            Log.d(TAG, "Documento guardado en Room: $documentId - ${documentEntity.name}")

            // 4. Intentar subir a Firebase (en background)
            tryUploadToFirebase(documentEntity, pdfFile)

            Result.success(documentId)

        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar documento", e)
            Result.failure(e)
        }
    }

    /**
     * Intentar subir documento a Firebase
     */
    private suspend fun tryUploadToFirebase(document: DocumentEntity, pdfFile: File) {
        try {
            Log.d(TAG, "Intentando subir a Firebase: ${document.id}")

            val pdfUrl = storageService.uploadPdf(
                userId = document.userId,
                file = pdfFile,
                fileName = pdfFile.name
            )

            val firebaseDoc = DocumentModel(
                id = document.id,
                userId = document.userId,
                name = document.name,
                category = document.category,
                notes = document.notes,
                pdfUrl = pdfUrl,
                pdfFileName = document.pdfFileName,
                fileSize = document.fileSize,
                pageCount = document.pageCount,
                createdAt = Timestamp(document.createdAt / 1000, 0),
                updatedAt = Timestamp(document.updatedAt / 1000, 0),
                isFavorite = document.isFavorite
            )

            documentService.saveDocument(firebaseDoc)
            documentDao.updateSyncStatus(document.id, isSynced = true, pdfUrl = pdfUrl)

            Log.d(TAG, "Documento sincronizado con Firebase: ${document.id}")

        } catch (e: Exception) {
            Log.e(TAG, "Error al subir a Firebase (quedará pendiente): ${e.message}")
        }
    }

    /**
     * Sincronizar documentos pendientes con Firebase
     */
    suspend fun syncPendingDocuments(): Result<Int> {
        return try {
            val unsyncedDocs = documentDao.getUnsyncedDocuments()
            var syncedCount = 0

            unsyncedDocs.forEach { doc ->
                val pdfFile = File(doc.pdfLocalPath)
                if (pdfFile.exists()) {
                    tryUploadToFirebase(doc, pdfFile)
                    syncedCount++
                }
            }

            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Alternar favorito
     */
    suspend fun toggleFavorite(documentId: String, isFavorite: Boolean): Result<Unit> {
        return try {
            documentDao.updateFavoriteStatus(documentId, isFavorite)

            try {
                documentService.toggleFavorite(documentId, isFavorite)
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar favorito en Firebase", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar documento
     */
    suspend fun deleteDocument(documentId: String): Result<Unit> {
        return try {
            val document = documentDao.getDocumentById(documentId)
                ?: return Result.failure(Exception("Documento no encontrado"))

            val pdfFile = File(document.pdfLocalPath)
            if (pdfFile.exists()) {
                pdfFile.delete()
            }

            documentDao.deleteDocumentById(documentId)

            try {
                if (document.pdfUrl != null) {
                    storageService.deletePdf(document.pdfUrl!!)
                }
                documentService.deleteDocument(documentId)
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar de Firebase", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtener estadísticas
     */
    suspend fun getStatistics(userId: String): DocumentStatistics {
        val count = documentDao.getDocumentCount(userId)
        val totalSize = documentDao.getTotalStorageUsed(userId) ?: 0L

        return DocumentStatistics(
            totalDocuments = count,
            totalStorageBytes = totalSize
        )
    }
}

data class DocumentStatistics(
    val totalDocuments: Int,
    val totalStorageBytes: Long
)