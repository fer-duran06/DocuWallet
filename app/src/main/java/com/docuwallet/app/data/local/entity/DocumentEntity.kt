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
    val notes: String = "",

    // Rutas de almacenamiento
    val pdfLocalPath: String,           // Ruta local (Room/SQLite)
    val pdfUrl: String? = null,         // URL de Firebase Storage (legacy)
    val cloudinaryUrl: String? = null,  // ✨ NUEVO: URL de Cloudinary

    val pdfFileName: String,
    val fileSize: Long,
    val pageCount: Int,

    // Campos adicionales
    val expiryDate: Long? = null,       // Fecha de vencimiento (timestamp)
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean = false,
    val isFavorite: Boolean = false,
    val accessCount: Int = 0
)