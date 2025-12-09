package com.docuwallet.app.data.remote.firebase

import com.docuwallet.app.domain.model.DocumentModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDocumentService @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val documentsCollection = firestore.collection("documents")

    /**
     * Guarda un documento en Firestore
     */
    suspend fun saveDocument(document: DocumentModel): String {
        val docRef = documentsCollection.add(document).await()
        return docRef.id
    }

    /**
     * Obtiene todos los documentos de un usuario
     */
    suspend fun getUserDocuments(userId: String): List<DocumentModel> {
        val snapshot = documentsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.toObjects(DocumentModel::class.java)
    }

    /**
     * Obtiene un documento por ID
     */
    suspend fun getDocument(documentId: String): DocumentModel? {
        val snapshot = documentsCollection.document(documentId).get().await()
        return snapshot.toObject(DocumentModel::class.java)
    }

    /**
     * Actualiza un documento
     */
    suspend fun updateDocument(documentId: String, updates: Map<String, Any>) {
        documentsCollection.document(documentId).update(updates).await()
    }

    /**
     * Elimina un documento
     */
    suspend fun deleteDocument(documentId: String) {
        documentsCollection.document(documentId).delete().await()
    }

    /**
     * Marca/desmarca documento como favorito
     */
    suspend fun toggleFavorite(documentId: String, isFavorite: Boolean) {
        documentsCollection.document(documentId)
            .update("isFavorite", isFavorite)
            .await()
    }
}