package com.docuwallet.app.data.remote.cloudinary

/**
 * Configuración de Cloudinary
 * IMPORTANTE: En producción, estas credenciales deben estar en:
 * - local.properties (no se sube a Git)
 * - Variables de entorno
 * - BuildConfig
 */
object CloudinaryConfig {
    const val CLOUD_NAME = "dq6xe7dcj"
    const val API_KEY = "579759489819765"
    const val API_SECRET = "eRbP556TnHBqPzihdWhbvSjAYmY"

    // Configuración de upload
    const val UPLOAD_PRESET = "docuwallet_pdfs" // Lo crearemos en el dashboard
    const val FOLDER = "docuwallet/documents"

    // Límites
    const val MAX_FILE_SIZE_MB = 10
    const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024L
}