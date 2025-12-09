package com.docuwallet.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.docuwallet.app.data.local.dao.DocumentDao
import com.docuwallet.app.data.local.entity.DocumentEntity
import com.docuwallet.app.data.remote.cloudinary.CloudinaryService
import com.docuwallet.app.data.remote.firebase.FirebaseDocumentService
import com.docuwallet.app.utils.PdfUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

data class SharedDocument(
    val documentId: String = "",
    val ownerId: String = "",
    val sharedWithId: String = "",
    val sharedAt: Long = System.currentTimeMillis()
)

class DocumentRepository(
    private val documentDao: DocumentDao,
    private val context: Context
) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore
    private val cloudinaryService = CloudinaryService(context)
    private val documentService = FirebaseDocumentService()
    private val TAG = "DocumentRepository"

    // --- MÉTODOS DE LECTURA ---
    fun getUserDocuments(userId: String): Flow<List<DocumentEntity>> {
        val userDocsFlow = documentDao.getUserDocuments(userId)
        val sharedDocsFlow = getSharedDocumentsForUser(userId)

        return userDocsFlow.combine(sharedDocsFlow) { own, shared ->
            (own + shared).distinctBy { it.id }.sortedByDescending { it.createdAt }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getSharedDocumentsForUser(userId: String): Flow<List<DocumentEntity>> = callbackFlow {
        val listener = firestore.collection("shared_documents")
            .whereEqualTo("sharedWithId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error escuchando documentos compartidos", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val docIds = snapshot.documents.mapNotNull { it.getString("documentId") }
                if (docIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Lanzamos una corrutina para buscar los detalles de los documentos
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val allSharedDocuments = mutableListOf<DocumentEntity>()
                        // Firestore tiene un límite de 30 IDs por consulta 'in'
                        docIds.chunked(30).forEach { chunk ->
                            val docsSnapshot = firestore.collection("documents")
                                .whereIn(FieldPath.documentId(), chunk)
                                .get()
                                .await()
                            allSharedDocuments.addAll(docsSnapshot.toObjects(DocumentEntity::class.java))
                        }
                        trySend(allSharedDocuments)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error buscando detalles de docs compartidos", e)
                        trySend(emptyList())
                    }
                }
            }
        awaitClose { listener.remove() }
    }


    fun getDocumentsByCategory(userId: String, category: String): Flow<List<DocumentEntity>> = documentDao.getDocumentsByCategory(userId, category)
    fun getFavoriteDocuments(userId: String): Flow<List<DocumentEntity>> = documentDao.getFavoriteDocuments(userId)

    suspend fun getDocumentById(documentId: String): DocumentEntity? {
        return try { documentDao.getDocumentById(documentId) } catch (e: Exception) { null }
    }

    // --- MÉTODOS DE ESCRITURA ---

    suspend fun shareDocumentWithUser(documentId: String, email: String): Result<Unit> {
        return try {
            val ownerId = auth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))
            val userQuery = firestore.collection("users").whereEqualTo("email", email).limit(1).get().await()
            if (userQuery.isEmpty) {
                return Result.failure(Exception("No se encontró ningún usuario con el email '$email'."))
            }
            val sharedWithId = userQuery.documents.first().id
            if(ownerId == sharedWithId) {
                return Result.failure(Exception("No puedes compartir un documento contigo mismo."))
            }

            val sharedDocument = SharedDocument(
                documentId = documentId,
                ownerId = ownerId,
                sharedWithId = sharedWithId
            )
            firestore.collection("shared_documents").add(sharedDocument).await()
            Log.d(TAG, "✅ Documento $documentId compartido con $email (ID: $sharedWithId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al compartir el documento: ${e.message}", e)
            Result.failure(Exception("Ocurrió un error al compartir. Inténtalo de nuevo.", e))
        }
    }

    suspend fun incrementAccessCount(documentId: String) {
        try { documentDao.incrementAccessCount(documentId) } catch (e: Exception) { Log.e(TAG, "Error contador", e) }
    }

    suspend fun saveDocument(imageUris: List<Uri>, name: String, category: String, notes: String, pageCount: Int): Result<String> {
        return saveDocumentWithExpiry(imageUris, name, category, notes, pageCount, null)
    }

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
            val pdfFile = PdfUtils.generatePdfFromImages(context, imageUris, name)
            if (!pdfFile.exists() || pdfFile.length() == 0L) return Result.failure(Exception("Error al generar PDF"))

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

            documentDao.insertDocument(documentEntity)
            Log.d(TAG, "✅ Documento guardado en Room: $documentId - ${documentEntity.name}")

            CoroutineScope(Dispatchers.IO).launch {
                tryUploadToCloudinary(documentEntity, pdfFile)
            }

            Result.success(documentId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error guardando", e)
            Result.failure(e)
        }
    }

    private suspend fun tryUploadToCloudinary(document: DocumentEntity, pdfFile: File) {
        try {
            Log.d(TAG, "📤 Intentando subir a Cloudinary: ${document.id}")
            val cloudinaryUrl = cloudinaryService.uploadPdf(pdfFile, document.id)
            documentDao.updateCloudinaryUrl(
                documentId = document.id,
                cloudinaryUrl = cloudinaryUrl,
                isSynced = true
            )
            Log.d(TAG, "✅ Documento sincronizado con Cloudinary: ${document.id}")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error al subir a Cloudinary (quedará pendiente): ${e.message}")
        }
    }

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
            File(doc.pdfLocalPath).delete()
            documentDao.deleteDocumentById(id)
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
}