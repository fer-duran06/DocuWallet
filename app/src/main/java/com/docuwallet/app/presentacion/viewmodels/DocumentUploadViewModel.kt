package com.docuwallet.app.presentacion.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuwallet.app.data.remote.firebase.FirebaseDocumentService
import com.docuwallet.app.data.remote.firebase.FirebaseStorageService
import com.docuwallet.app.domain.model.DocumentModel
import com.docuwallet.app.utils.PdfUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

// ❌ ELIMINAR ESTAS LÍNEAS (18-29) - Ya están en DocumentUploadStates.kt

class DocumentUploadViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val storageService = FirebaseStorageService()
    private val documentService = FirebaseDocumentService()

    private val _pdfState = MutableStateFlow(PdfGenerationState())
    val pdfState: StateFlow<PdfGenerationState> = _pdfState.asStateFlow()

    private val _saveState = MutableStateFlow(DocumentSaveState())
    val saveState: StateFlow<DocumentSaveState> = _saveState.asStateFlow()

    // Almacenar el PDF generado temporalmente
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

    fun saveDocument(
        name: String,
        category: String,
        notes: String,
        pageCount: Int
    ) {
        viewModelScope.launch {
            try {
                _saveState.value = DocumentSaveState(isLoading = true)

                val userId = auth.currentUser?.uid
                    ?: throw Exception("Usuario no autenticado")

                val pdfFile = generatedPdfFile
                    ?: throw Exception("No hay PDF generado")

                // 1. Subir PDF a Firebase Storage
                val pdfUrl = storageService.uploadPdf(
                    userId = userId,
                    file = pdfFile,
                    fileName = pdfFile.name
                )

                // 2. Crear documento en Firestore
                val document = DocumentModel(
                    userId = userId,
                    name = name,
                    category = category,
                    notes = notes,
                    pdfUrl = pdfUrl,
                    pdfFileName = pdfFile.name,
                    fileSize = pdfFile.length(),
                    pageCount = pageCount,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                val documentId = documentService.saveDocument(document)

                _saveState.value = DocumentSaveState(
                    isLoading = false,
                    isSuccess = true,
                    documentId = documentId
                )

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
    }

    fun resetSaveState() {
        _saveState.value = DocumentSaveState()
        generatedPdfFile = null
    }
}