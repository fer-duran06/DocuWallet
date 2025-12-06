package com.docuwallet.app.data.remote.cloudinary

import android.content.Context
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CloudinaryService(private val context: Context) {

    private val TAG = "CloudinaryService"

    init {
        initializeCloudinary()
    }

    /**
     * Inicializar Cloudinary con las credenciales (solo una vez)
     */
    private fun initializeCloudinary() {
        try {
            // ✅ CORREGIDO: Verificar si ya está inicializado
            if (!isCloudinaryInitialized) {
                val config = HashMap<String, String>()
                config["cloud_name"] = CloudinaryConfig.CLOUD_NAME
                config["api_key"] = CloudinaryConfig.API_KEY
                config["api_secret"] = CloudinaryConfig.API_SECRET

                MediaManager.init(context, config)
                isCloudinaryInitialized = true
                Log.d(TAG, "✅ Cloudinary inicializado correctamente")
            } else {
                Log.d(TAG, "ℹ️ Cloudinary ya estaba inicializado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al inicializar Cloudinary", e)
        }
    }

    /**
     * Subir PDF a Cloudinary
     */
    suspend fun uploadPdf(pdfFile: File, documentId: String): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                // Validar tamaño del archivo
                if (pdfFile.length() > CloudinaryConfig.MAX_FILE_SIZE_BYTES) {
                    continuation.resumeWithException(
                        Exception("El archivo excede el tamaño máximo de ${CloudinaryConfig.MAX_FILE_SIZE_MB} MB")
                    )
                    return@suspendCancellableCoroutine
                }

                // Validar que el archivo existe
                if (!pdfFile.exists()) {
                    continuation.resumeWithException(Exception("El archivo no existe"))
                    return@suspendCancellableCoroutine
                }

                Log.d(TAG, "📤 Subiendo PDF a Cloudinary: ${pdfFile.name}")

                // Configurar opciones de upload
                val requestId = MediaManager.get()
                    .upload(pdfFile.absolutePath)
                    .option("resource_type", "raw") // Para PDFs usar "raw"
                    .option("folder", CloudinaryConfig.FOLDER)
                    .option("public_id", documentId) // Usar ID del documento como nombre
                    .option("overwrite", true)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            Log.d(TAG, "Upload iniciado: $requestId")
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            val progress = (bytes.toDouble() / totalBytes.toDouble() * 100).toInt()
                            Log.d(TAG, "Upload progreso: $progress%")
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val secureUrl = resultData["secure_url"] as? String
                            if (secureUrl != null) {
                                Log.d(TAG, "✅ Upload exitoso: $secureUrl")
                                continuation.resume(secureUrl)
                            } else {
                                continuation.resumeWithException(Exception("No se obtuvo URL del archivo"))
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            Log.e(TAG, "❌ Error en upload: ${error.description}")
                            continuation.resumeWithException(Exception("Error al subir PDF: ${error.description}"))
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            Log.w(TAG, "⏰ Upload reprogramado: ${error.description}")
                        }
                    })
                    .dispatch()

                // Cancelar upload si se cancela la coroutine
                continuation.invokeOnCancellation {
                    MediaManager.get().cancelRequest(requestId)
                    Log.d(TAG, "Upload cancelado: $requestId")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al subir PDF", e)
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * Eliminar PDF de Cloudinary
     */
    suspend fun deletePdf(publicId: String): Boolean {
        return try {
            // Cloudinary Android SDK no tiene método directo para eliminar
            // Necesitarías usar la API REST directamente o hacerlo desde el dashboard
            // Por ahora, solo retornamos true y lo manejamos manualmente
            Log.d(TAG, "Eliminación programada para: $publicId")
            Log.d(TAG, "Nota: La eliminación debe hacerse desde el dashboard o API REST")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar PDF", e)
            false
        }
    }

    /**
     * Extraer public_id de una URL de Cloudinary
     */
    fun extractPublicId(cloudinaryUrl: String): String? {
        return try {
            val parts = cloudinaryUrl.split("/")
            val uploadIndex = parts.indexOf("upload")
            if (uploadIndex != -1 && uploadIndex + 2 < parts.size) {
                // Saltar versión (v123) y obtener folder/filename sin extensión
                val pathAfterVersion = parts.subList(uploadIndex + 2, parts.size).joinToString("/")
                pathAfterVersion.substringBeforeLast(".") // Remover extensión
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo public_id", e)
            null
        }
    }

    companion object {
        @Volatile
        private var isCloudinaryInitialized = false
    }
}