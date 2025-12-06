package com.docuwallet.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.docuwallet.app.data.local.dao.DocumentDao
import com.docuwallet.app.data.local.database.AppDatabase
import com.docuwallet.app.data.local.entity.DocumentEntity
import com.docuwallet.app.data.remote.cloudinary.CloudinaryService
import com.docuwallet.app.data.remote.firebase.FirebaseDocumentService
import com.docuwallet.app.domain.model.DocumentModel
import com.docuwallet.app.utils.PdfUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class DocumentRepository(private val context: Context) {

    private val documentDao: DocumentDao = AppDatabase.getDatabase(context).documentDao()
    private val auth = FirebaseAuth.getInstance()
    private val cloudinaryService = CloudinaryService(context)
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

    // ✨ LA FUNCIÓN PRINCIPAL DE GUARDADO (CORREGIDA)
    suspend fun saveDocumentWithExpiry(
        imageUris: List<Uri>,
        name: String,
        category: String,
        notes: String,
        pageCount: Int,
        expiryDate: Long? = null
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
                cloudinaryUrl = null,
                pdfFileName = pdfFile.name,
                fileSize = pdfFile.length(),
                pageCount = pageCount,
                expiryDate = expiryDate,
                createdAt = currentTime,
                updatedAt = currentTime,
                isSynced = false,
                isFavorite = false,
                accessCount = 0
            )

            // 3. Guardar en BD Local
            documentDao.insertDocument(documentEntity)
            Log.d(TAG, "✅ Documento guardado en Room: $documentId - ${documentEntity.name}")

            // 4. ✨ NUEVO: Subir a Cloudinary de forma ASÍNCRONA (no bloqueante)
            // Esto se ejecuta en segundo plano y NO bloquea el guardado
            CoroutineScope(Dispatchers.IO).launch {
                tryUploadToCloudinary(documentEntity, pdfFile)
            }

            // 5. Retornar éxito inmediatamente
            Result.success(documentId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error guardando", e)
            Result.failure(e)
        }
    }

    /**
     * ✨ CORREGIDO: Subir documento a Cloudinary (con timeout y manejo de errores)
     */
    private suspend fun tryUploadToCloudinary(document: DocumentEntity, pdfFile: File) {
        try {
            Log.d(TAG, "📤 Intentando subir a Cloudinary: ${document.id}")

            // Subir PDF a Cloudinary (esto puede fallar si no hay internet)
            val cloudinaryUrl = cloudinaryService.uploadPdf(pdfFile, document.id)

            // Actualizar URL en Room
            documentDao.updateCloudinaryUrl(
                documentId = document.id,
                cloudinaryUrl = cloudinaryUrl,
                isSynced = true
            )

            Log.d(TAG, "✅ Documento sincronizado con Cloudinary: ${document.id}")
            Log.d(TAG, "🔗 URL: $cloudinaryUrl")

        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error al subir a Cloudinary (quedará pendiente): ${e.message}")
            // El documento queda guardado localmente, se puede reintentar después
            // NO marcamos como error, simplemente queda pendiente de sincronización
        }
    }

    /**
     * Sincronizar documentos pendientes con Cloudinary
     */
    suspend fun syncPendingDocuments(): Result<Int> {
        return try {
            val unsyncedDocs = documentDao.getUnsyncedDocuments()
            var syncedCount = 0

            Log.d(TAG, "🔄 Sincronizando ${unsyncedDocs.size} documentos pendientes...")

            unsyncedDocs.forEach { doc ->
                val pdfFile = File(doc.pdfLocalPath)
                if (pdfFile.exists()) {
                    try {
                        tryUploadToCloudinary(doc, pdfFile)
                        syncedCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sincronizando ${doc.id}: ${e.message}")
                    }
                }
            }

            Log.d(TAG, "✅ Sincronizados $syncedCount de ${unsyncedDocs.size} documentos")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

            // Eliminar archivo local
            File(doc.pdfLocalPath).delete()

            // Eliminar de Room
            documentDao.deleteDocumentById(id)

            // ✨ Eliminar de Cloudinary si existe (asíncrono)
            if (doc.cloudinaryUrl != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val publicId = cloudinaryService.extractPublicId(doc.cloudinaryUrl)
                        if (publicId != null) {
                            cloudinaryService.deletePdf(publicId)
                            Log.d(TAG, "🗑️ Documento eliminado de Cloudinary: $publicId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al eliminar de Cloudinary", e)
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getStatistics(userId: String): DocumentStatistics {
        val count = documentDao.getDocumentCount(userId)
        val size = documentDao.getTotalStorageUsed(userId) ?: 0L
        return DocumentStatistics(count, size)
    }

    /**
     * Obtener estadísticas de sincronización con Cloudinary
     */
    suspend fun getSyncStatistics(userId: String): SyncStatistics {
        return try {
            val allDocs = documentDao.getUserDocuments(userId)
            var totalDocs = 0
            var syncedDocs = 0
            var localOnlyDocs = 0

            allDocs.collect { documents ->
                totalDocs = documents.size
                syncedDocs = documents.count { it.cloudinaryUrl != null && it.isSynced }
                localOnlyDocs = documents.count { it.cloudinaryUrl == null }
            }

            SyncStatistics(
                totalDocuments = totalDocs,
                syncedToCloud = syncedDocs,
                localOnly = localOnlyDocs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo estadísticas de sync", e)
            SyncStatistics(0, 0, 0)
        }
    }
}

data class DocumentStatistics(
    val totalDocuments: Int,
    val totalStorageBytes: Long
)

data class SyncStatistics(
    val totalDocuments: Int,
    val syncedToCloud: Int,
    val localOnly: Int
)