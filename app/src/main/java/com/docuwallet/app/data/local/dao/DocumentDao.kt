package com.docuwallet.app.data.local.dao

import androidx.room.*
import com.docuwallet.app.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUserDocuments(userId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE userId = :userId AND category = :category ORDER BY createdAt DESC")
    fun getDocumentsByCategory(userId: String, category: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE userId = :userId AND isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteDocuments(userId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun getDocumentById(documentId: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE isSynced = 0")
    suspend fun getUnsyncedDocuments(): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("UPDATE documents SET isFavorite = :isFavorite WHERE id = :documentId")
    suspend fun updateFavoriteStatus(documentId: String, isFavorite: Boolean)

    @Query("UPDATE documents SET isSynced = :isSynced, pdfUrl = :pdfUrl WHERE id = :documentId")
    suspend fun updateSyncStatus(documentId: String, isSynced: Boolean, pdfUrl: String?)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteDocumentById(documentId: String)

    @Query("DELETE FROM documents WHERE userId = :userId")
    suspend fun deleteUserDocuments(userId: String)

    @Query("SELECT COUNT(*) FROM documents WHERE userId = :userId")
    suspend fun getDocumentCount(userId: String): Int

    @Query("SELECT SUM(fileSize) FROM documents WHERE userId = :userId")
    suspend fun getTotalStorageUsed(userId: String): Long?

    @Query("UPDATE documents SET accessCount = accessCount + 1 WHERE id = :documentId")
    suspend fun incrementAccessCount(documentId: String)
}