package com.docuwallet.app.utils

object Constants {
    const val COLLECTION_USERS = "users"
    const val COLLECTION_DOCUMENTS = "documents"
    const val COLLECTION_CATEGORIES = "categories"

    const val MAX_FILE_SIZE_MB = 10
    const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024L

    const val DAYS_WARNING_THRESHOLD = 15
    const val DAYS_URGENT_THRESHOLD = 7

    val CATEGORIES = listOf(
        "Identificación",
        "Salud",
        "Viajes",
        "Educación",
        "Trabajo",
        "Personal"
    )

    const val MIN_PASSWORD_LENGTH = 6
    const val DATE_FORMAT = "dd/MM/yyyy"
}