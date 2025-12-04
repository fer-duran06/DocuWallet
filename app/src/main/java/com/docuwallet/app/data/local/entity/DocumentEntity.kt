package com.docuwallet.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val category: String,
    val notes: String,
    val pdfLocalPath: String,           // Ruta local del PDF
    val pdfUrl: String? = null,         // URL de Firebase Storage (cuando se suba)
    val pdfFileName: String,
    val fileSize: Long,
    val pageCount: Int,
    val expiryDate: Long? = null,       // Timestamp en millis
    val createdAt: Long,                // Timestamp en millis
    val updatedAt: Long,                // Timestamp en millis
    val isFavorite: Boolean = false,
    val isSynced: Boolean = false,       // ¿Ya está en Firebase?
    val accessCount: Int = 0  // ← AGREGAR ESTE CAMPO

)