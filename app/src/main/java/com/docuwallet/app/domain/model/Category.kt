package com.docuwallet.app.domain.model

enum class Category(val displayName: String, val icon: String) {
    IDENTIFICACION("Identificación", "badge"),
    SALUD("Salud", "favorite"),
    VIAJES("Viajes", "flight"),
    EDUCACION("Educación", "school"),
    TRABAJO("Trabajo", "work"),
    PERSONAL("Personal", "person");

    companion object {
        fun fromString(value: String): Category {
            return values().find { it.name == value } ?: PERSONAL
        }

        fun getAllCategories(): List<Category> {
            return values().toList()
        }
    }
}