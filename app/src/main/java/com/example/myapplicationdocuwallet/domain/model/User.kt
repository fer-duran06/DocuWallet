package com.docuwallet.app.domain.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val totalDocuments: Int = 0,
    val storageUsedMB: Double = 0.0
)