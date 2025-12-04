package com.docuwallet.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object PdfUtils {

    /**
     * Generar PDF desde múltiples imágenes usando iText
     */
    fun generatePdfFromImages(
        context: Context,
        imageUris: List<Uri>,
        documentName: String
    ): File {
        // Crear directorio de documentos si no existe
        val documentsDir = File(context.cacheDir, "documents")
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }

        // Crear archivo PDF
        val pdfFileName = "${documentName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(documentsDir, pdfFileName)

        // Crear PDF con iText
        val writer = PdfWriter(pdfFile)
        val pdfDoc = com.itextpdf.kernel.pdf.PdfDocument(writer)
        val document = Document(pdfDoc)

        // Configurar márgenes
        document.setMargins(0f, 0f, 0f, 0f)

        // Agregar cada imagen como una página
        imageUris.forEachIndexed { index, uri ->
            try {
                // Leer imagen
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    // Convertir bitmap a ByteArray
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    val byteArray = stream.toByteArray()
                    stream.close()

                    // Crear ImageData de iText
                    val imageData = ImageDataFactory.create(byteArray)
                    val image = Image(imageData)

                    // Calcular tamaño de página basado en la imagen
                    val pageWidth = bitmap.width.toFloat()
                    val pageHeight = bitmap.height.toFloat()

                    // Agregar nueva página con el tamaño de la imagen
                    if (index > 0) {
                        document.add(com.itextpdf.layout.element.AreaBreak(PageSize(pageWidth, pageHeight)))
                    } else {
                        pdfDoc.defaultPageSize = PageSize(pageWidth, pageHeight)
                    }

                    // Escalar imagen para que ocupe toda la página
                    image.scaleToFit(pageWidth, pageHeight)
                    image.setFixedPosition(0f, 0f)

                    // Agregar imagen al documento
                    document.add(image)

                    // Limpiar bitmap
                    bitmap.recycle()

                    android.util.Log.d("PdfUtils", "Página ${index + 1}/${imageUris.size} agregada al PDF")
                } else {
                    android.util.Log.e("PdfUtils", "Error al decodificar imagen ${index + 1}")
                }
            } catch (e: Exception) {
                android.util.Log.e("PdfUtils", "Error al procesar imagen ${index + 1}", e)
            }
        }

        // Cerrar documento
        document.close()

        android.util.Log.d("PdfUtils", "PDF generado exitosamente: ${pdfFile.absolutePath}")
        android.util.Log.d("PdfUtils", "Tamaño del PDF: ${pdfFile.length() / 1024} KB")
        android.util.Log.d("PdfUtils", "Total de páginas: ${imageUris.size}")

        return pdfFile
    }

    /**
     * Generar PDF desde una sola imagen
     */
    fun generatePdfFromSingleImage(
        context: Context,
        imageUri: Uri,
        documentName: String
    ): File {
        return generatePdfFromImages(context, listOf(imageUri), documentName)
    }
}