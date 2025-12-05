package com.docuwallet.app.presentacion.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docuwallet.app.data.repository.DocumentRepository
import com.docuwallet.app.utils.PdfUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class DocumentUploadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(application.applicationContext)

    private val _pdfState = MutableStateFlow(PdfGenerationState())
    val pdfState: StateFlow<PdfGenerationState> = _pdfState.asStateFlow()

    private val _saveState = MutableStateFlow(DocumentSaveState())
    val saveState: StateFlow<DocumentSaveState> = _saveState.asStateFlow()

    private var generatedPdfFile: File? = null

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

                generatedPdfFile = pdfFile

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

    // Función wrapper para compatibilidad
    fun saveDocument(
        context: Context,
        imageUris: List<Uri>,
        name: String,
        category: String,
        notes: String,
        pageCount: Int
    ) {
        saveDocumentWithExpiry(context, imageUris, name, category, notes, pageCount, null)
    }

    // ✨ FUNCIÓN CORREGIDA: Pasa el Long directamente
    fun saveDocumentWithExpiry(
        context: Context,
        imageUris: List<Uri>,
        name: String,
        category: String,
        notes: String,
        pageCount: Int,
        expiryDate: Long?
    ) {
        viewModelScope.launch {
            try {
                _saveState.value = DocumentSaveState(isLoading = true)

                // NO convertimos a String aquí. Enviamos el Long (timestamp) al repositorio.
                val result = repository.saveDocumentWithExpiry(
                    imageUris = imageUris,
                    name = name,
                    category = category,
                    notes = notes,
                    pageCount = pageCount,
                    expiryDate = expiryDate // <--- Pasamos el Long directo
                )

                if (result.isSuccess) {
                    _saveState.value = DocumentSaveState(
                        isLoading = false,
                        isSuccess = true,
                        documentId = result.getOrNull()
                    )
                } else {
                    _saveState.value = DocumentSaveState(
                        isLoading = false,
                        isSuccess = false,
                        error = result.exceptionOrNull()?.message ?: "Error al guardar documento"
                    )
                }

            } catch (e: Exception) {
                _saveState.value = DocumentSaveState(
                    isLoading = false,
                    isSuccess = false,
                    error = e.message ?: "Error al guardar documento"
                )
            }
        }
    }

    fun resetPdfState() {
        _pdfState.value = PdfGenerationState()
        generatedPdfFile = null
    }

    fun resetSaveState() {
        _saveState.value = DocumentSaveState()
    }
}