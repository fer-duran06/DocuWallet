package com.docuwallet.app.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class DocumentModel(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "",
    val notes: String = "",
    val pdfUrl: String = "",
    val pdfFileName: String = "",
    val fileSize: Long = 0,
    val expiryDate: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val isFavorite: Boolean = false,
    val pageCount: Int = 0
) {
    // Constructor sin parámetros requerido por Firestore
    constructor() : this(
        id = "",
        userId = "",
        name = "",
        category = "",
        notes = "",
        pdfUrl = "",
        pdfFileName = "",
        fileSize = 0,
        expiryDate = null,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
        isFavorite = false,
        pageCount = 0
    )
}