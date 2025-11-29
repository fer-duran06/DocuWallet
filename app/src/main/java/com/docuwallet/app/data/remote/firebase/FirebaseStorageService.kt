package com.docuwallet.app.data.remote.firebase

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageService @Inject constructor() {

    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    /**
     * Sube un archivo PDF a Firebase Storage
     * @param userId ID del usuario
     * @param file Archivo PDF a subir
     * @param fileName Nombre del archivo
     * @return URL de descarga del archivo
     */
    suspend fun uploadPdf(
        userId: String,
        file: File,
        fileName: String
    ): String {
        val fileUri = Uri.fromFile(file)
        val documentRef = storageRef
            .child("users/$userId/documents/$fileName")

        // Subir archivo
        documentRef.putFile(fileUri).await()

        // Obtener URL de descarga
        return documentRef.downloadUrl.await().toString()
    }

    /**
     * Elimina un archivo PDF de Firebase Storage
     * @param pdfUrl URL del archivo a eliminar
     */
    suspend fun deletePdf(pdfUrl: String) {
        val fileRef = storage.getReferenceFromUrl(pdfUrl)
        fileRef.delete().await()
    }

    /**
     * Obtiene el tamaño de un archivo
     * @param pdfUrl URL del archivo
     * @return Tamaño en bytes
     */
    suspend fun getFileSize(pdfUrl: String): Long {
        val fileRef = storage.getReferenceFromUrl(pdfUrl)
        val metadata = fileRef.metadata.await()
        return metadata.sizeBytes
    }
}