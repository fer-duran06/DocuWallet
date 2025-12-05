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

    // --- MÉTODOS DE LECTURA ---
    fun getUserDocuments(userId: String): Flow<List<DocumentEntity>> = documentDao.getUserDocuments(userId)
    fun getDocumentsByCategory(userId: String, category: String): Flow<List<DocumentEntity>> = documentDao.getDocumentsByCategory(userId, category)
    fun getFavoriteDocuments(userId: String): Flow<List<DocumentEntity>> = documentDao.getFavoriteDocuments(userId)

    suspend fun getDocumentById(documentId: String): DocumentEntity? {
        return try { documentDao.getDocumentById(documentId) } catch (e: Exception) { null }
    }

    // --- MÉTODOS DE ESCRITURA ---
    suspend fun incrementAccessCount(documentId: String) {
        try { documentDao.incrementAccessCount(documentId) } catch (e: Exception) { Log.e(TAG, "Error contador", e) }
    }

    // Compatibilidad
    suspend fun saveDocument(imageUris: List<Uri>, name: String, category: String, notes: String, pageCount: Int): Result<String> {
        return saveDocumentWithExpiry(imageUris, name, category, notes, pageCount, null)
    }

    // ✨ LA FUNCIÓN PRINCIPAL
    suspend fun saveDocumentWithExpiry(
        imageUris: List<Uri>,
        name: String,
        category: String,
        notes: String,
        pageCount: Int,
        expiryDate: Long? = null // Recibe Long
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))

            // 1. Generar PDF
            val pdfFile = PdfUtils.generatePdfFromImages(context, imageUris, name)
            if (!pdfFile.exists() || pdfFile.length() == 0L) return Result.failure(Exception("Error al generar PDF"))

            // 2. Crear Entidad LOCAL (Room usa Long)
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
                expiryDate = expiryDate, // Se guarda directo como Long
                createdAt = currentTime,
                updatedAt = currentTime,
                isSynced = false,
                isFavorite = false,
                accessCount = 0
            )

            // 3. Guardar en BD Local
            documentDao.insertDocument(documentEntity)

            // 4. Subir a Firebase
            tryUploadToFirebase(documentEntity, pdfFile)

            Result.success(documentId)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando", e)
            Result.failure(e)
        }
    }

    private suspend fun tryUploadToFirebase(document: DocumentEntity, pdfFile: File) {
        try {
            val pdfUrl = storageService.uploadPdf(document.userId, pdfFile, pdfFile.name)

            // Convertir Long a Timestamp para Firebase
            val expiryTimestamp = document.expiryDate?.let { Timestamp(it / 1000, 0) }

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
                expiryDate = expiryTimestamp, // Firebase usa Timestamp
                createdAt = Timestamp(document.createdAt / 1000, 0),
                updatedAt = Timestamp(document.updatedAt / 1000, 0),
                isFavorite = document.isFavorite
            )

            documentService.saveDocument(firebaseDoc)
            documentDao.updateSyncStatus(document.id, isSynced = true, pdfUrl = pdfUrl)

        } catch (e: Exception) {
            Log.e(TAG, "Error subida Firebase: ${e.message}")
        }
    }

    suspend fun syncPendingDocuments(): Result<Int> {
        return try {
            val unsynced = documentDao.getUnsyncedDocuments()
            var count = 0
            unsynced.forEach { doc ->
                val file = File(doc.pdfLocalPath)
                if (file.exists()) {
                    tryUploadToFirebase(doc, file)
                    count++
                }
            }
            Result.success(count)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun toggleFavorite(id: String, isFav: Boolean): Result<Unit> {
        return try {
            documentDao.updateFavoriteStatus(id, isFav)
            try { documentService.toggleFavorite(id, isFav) } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteDocument(id: String): Result<Unit> {
        return try {
            val doc = documentDao.getDocumentById(id) ?: return Result.failure(Exception("No existe"))
            File(doc.pdfLocalPath).delete()
            documentDao.deleteDocumentById(id)
            try {
                doc.pdfUrl?.let { storageService.deletePdf(it) }
                documentService.deleteDocument(id)
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getStatistics(userId: String): DocumentStatistics {
        val count = documentDao.getDocumentCount(userId)
        val size = documentDao.getTotalStorageUsed(userId) ?: 0L
        return DocumentStatistics(count, size)
    }
}

// 👇 ESTO ERA LO QUE FALTABA Y CAUSABA TODOS LOS ERRORES EN LAS OTRAS PANTALLAS
data class DocumentStatistics(
    val totalDocuments: Int,
    val totalStorageBytes: Long
)