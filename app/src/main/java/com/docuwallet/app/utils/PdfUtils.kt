package com.docuwallet.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object PdfUtils {

    private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss"

    /**
     * Genera un PDF a partir de una lista de imágenes
     * @param context Contexto de la aplicación
     * @param imageUris Lista de URIs de las imágenes
     * @param documentName Nombre del documento (opcional)
     * @return File del PDF generado
     */
    suspend fun generatePdfFromImages(
        context: Context,
        imageUris: List<Uri>,
        documentName: String? = null
    ): File = withContext(Dispatchers.IO) {

        if (imageUris.isEmpty()) {
            throw IllegalArgumentException("La lista de imágenes está vacía")
        }

        // Crear archivo PDF
        val pdfFile = createPdfFile(context, documentName)

        // Crear documento PDF
        val writer = PdfWriter(pdfFile)
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)

        // Configurar márgenes
        document.setMargins(0f, 0f, 0f, 0f)

        try {
            // Agregar cada imagen como una página
            imageUris.forEach { uri ->
                addImageToDocument(context, document, pdfDoc, uri)
            }
        } finally {
            document.close()
        }

        return@withContext pdfFile
    }

    /**
     * Agrega una imagen al documento PDF
     */
    private fun addImageToDocument(
        context: Context,
        document: Document,
        pdfDoc: PdfDocument,
        imageUri: Uri
    ) {
        context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            // Leer imagen
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Comprimir imagen si es necesario (para optimizar tamaño del PDF)
            val compressedBitmap = compressBitmapIfNeeded(bitmap)

            // Convertir a byte array
            val byteArray = bitmapToByteArray(compressedBitmap)

            // Crear imagen para iText
            val imageData = ImageDataFactory.create(byteArray)
            val image = Image(imageData)

            // Ajustar imagen al tamaño de la página
            val pageSize = pdfDoc.defaultPageSize
            image.scaleToFit(pageSize.width, pageSize.height)

            // Centrar imagen
            image.setFixedPosition(
                (pageSize.width - image.imageScaledWidth) / 2,
                (pageSize.height - image.imageScaledHeight) / 2
            )

            // Agregar imagen al documento
            document.add(image)

            // Limpiar bitmap
            if (compressedBitmap != bitmap) {
                compressedBitmap.recycle()
            }
            bitmap.recycle()
        } ?: throw IllegalArgumentException("No se pudo leer la imagen: $imageUri")
    }

    /**
     * Comprime el bitmap si es muy grande
     */
    private fun compressBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxWidth = 1200
        val maxHeight = 1600

        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) {
            return bitmap
        }

        val ratio = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )

        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Convierte Bitmap a ByteArray
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Crea el archivo PDF en el directorio de documentos
     */
    private fun createPdfFile(context: Context, documentName: String?): File {
        val timestamp = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val fileName = documentName?.let {
            "${it.replace(" ", "_")}_$timestamp.pdf"
        } ?: "document_$timestamp.pdf"

        val storageDir = File(context.cacheDir, "documents")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        return File(storageDir, fileName)
    }

    /**
     * Obtiene el tamaño del archivo en MB
     */
    fun getFileSizeInMB(file: File): Double {
        return file.length() / (1024.0 * 1024.0)
    }

    /**
     * Cuenta el número de páginas en un PDF
     */
    suspend fun countPdfPages(pdfFile: File): Int = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument(com.itextpdf.kernel.pdf.PdfReader(pdfFile))
            val pageCount = pdfDoc.numberOfPages
            pdfDoc.close()
            pageCount
        } catch (e: Exception) {
            0
        }
    }
}