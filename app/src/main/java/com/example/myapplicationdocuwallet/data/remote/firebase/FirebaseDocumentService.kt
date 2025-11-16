package com.docuwallet.app.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class DocumentData(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "",
    val expirationDate: String = "",
    val fileUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 0,
    val isFavorite: Boolean = false,
    val fileSizeBytes: Long = 0
)

class FirebaseDocumentService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val documentsCollection = db.collection("documents")

    suspend fun saveDocument(document: DocumentData): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val docData = document.copy(userId = userId)
            val docRef = documentsCollection.add(docData).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDocument(documentId: String, document: DocumentData): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val docData = document.copy(
                userId = userId,
                updatedAt = System.currentTimeMillis()
            )
            documentsCollection.document(documentId).set(docData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(documentId: String): Result<Unit> {
        return try {
            documentsCollection.document(documentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDocumentById(documentId: String): Result<DocumentData?> {
        return try {
            val snapshot = documentsCollection.document(documentId).get().await()
            val document = snapshot.toObject(DocumentData::class.java)?.copy(id = snapshot.id)
            Result.success(document)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllDocuments(): Flow<List<DocumentData>> = callbackFlow {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = documentsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val documents = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(DocumentData::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(documents)
            }

        awaitClose { listener.remove() }
    }

    suspend fun searchDocuments(query: String): Result<List<DocumentData>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val snapshot = documentsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val documents = snapshot.documents.mapNotNull { doc ->
                doc.toObject(DocumentData::class.java)?.copy(id = doc.id)
            }.filter { it.name.contains(query, ignoreCase = true) }

            Result.success(documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDocumentsByCategory(category: String): Result<List<DocumentData>> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Usuario no autenticado"))

            val snapshot = documentsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("category", category)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val documents = snapshot.documents.mapNotNull { doc ->
                doc.toObject(DocumentData::class.java)?.copy(id = doc.id)
            }

            Result.success(documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(documentId: String, isFavorite: Boolean): Result<Unit> {
        return try {
            documentsCollection.document(documentId)
                .update("isFavorite", isFavorite)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun incrementAccessCount(documentId: String): Result<Unit> {
        return try {
            val docRef = documentsCollection.document(documentId)
            val snapshot = docRef.get().await()
            val currentCount = snapshot.getLong("accessCount")?.toInt() ?: 0

            docRef.update("accessCount", currentCount + 1).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}