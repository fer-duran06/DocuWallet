package com.docuwallet.app.presentacion.viewmodels

import java.io.File

data class PdfGenerationState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val pdfFile: File? = null,
    val error: String? = null
)

data class DocumentSaveState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val documentId: String? = null,
    val error: String? = null
)