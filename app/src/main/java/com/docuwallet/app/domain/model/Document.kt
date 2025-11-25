package com.docuwallet.app.domain.model

data class Document(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: Category = Category.PERSONAL,
    val expirationDate: String = "",
    val fileUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 0,
    val isFavorite: Boolean = false,
    val fileSizeBytes: Long = 0,
    val daysRemaining: Int = 0,
    val alertLevel: AlertLevel = AlertLevel.SAFE
)