package com.docuwallet.app.presentacion.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuwallet.app.utils.PdfUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class PdfGenerationState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val pdfFile: File? = null,
    val error: String? = null
)

class DocumentUploadViewModel : ViewModel() {

    private val _pdfState = MutableStateFlow(PdfGenerationState())
    val pdfState: StateFlow<PdfGenerationState> = _pdfState.asStateFlow()

    fun generatePdf(
        context: Context,
        imageUris: List<Uri>,
        documentName: String = "documento"
    ) {
        viewModelScope.launch {
            try {
                _pdfState.value = PdfGenerationState(isLoading = true)

                val pdfFile = PdfUtils.generatePdfFromImages(
                    context = context,
                    imageUris = imageUris,
                    documentName = documentName
                )

                _pdfState.value = PdfGenerationState(
                    isLoading = false,
                    isSuccess = true,
                    pdfFile = pdfFile
                )

            } catch (e: Exception) {
                _pdfState.value = PdfGenerationState(
                    isLoading = false,
                    isSuccess = false,
                    error = e.message ?: "Error al generar PDF"
                )
            }
        }
    }

    fun resetState() {
        _pdfState.value = PdfGenerationState()
    }
}