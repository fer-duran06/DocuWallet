package com.docuwallet.app.presentacion.views.main

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.docuwallet.app.presentacion.viewmodels.DocumentUploadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagePreviewScreen(
    capturedImages: List<Uri>,
    onNavigateBack: () -> Unit,
    onGeneratePdf: () -> Unit,
    onDeletePage: (Int) -> Unit,
    viewModel: DocumentUploadViewModel = viewModel()
) {
    val context = LocalContext.current
    val pdfState by viewModel.pdfState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var pageToDelete by remember { mutableStateOf(-1) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Mostrar diálogo de éxito cuando el PDF se genera
    LaunchedEffect(pdfState.isSuccess) {
        if (pdfState.isSuccess && pdfState.pdfFile != null) {
            showSuccessDialog = true
        }
    }

    // Mostrar error si hay
    LaunchedEffect(pdfState.error) {
        if (pdfState.error != null) {
            // TODO: Mostrar snackbar con error
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Páginas: ${capturedImages.size}")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = {
                        if (capturedImages.isNotEmpty()) {
                            viewModel.generatePdf(
                                context = context,
                                imageUris = capturedImages,
                                documentName = "documento_escaneado"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = capturedImages.isNotEmpty() && !pdfState.isLoading
                ) {
                    if (pdfState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generando PDF...")
                    } else {
                        Icon(Icons.Default.PictureAsPdf, "PDF")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generar PDF")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (capturedImages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay páginas",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Toca el botón de la cámara para capturar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(capturedImages) { index, imageUri ->
                        PagePreviewItem(
                            imageUri = imageUri,
                            pageNumber = index + 1,
                            onDeleteClick = {
                                pageToDelete = index
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar página") },
            text = { Text("¿Estás seguro de que deseas eliminar esta página?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePage(pageToDelete)
                        showDeleteDialog = false
                        pageToDelete = -1
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                viewModel.resetState()
                onGeneratePdf()
            },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("PDF Generado") },
            text = {
                Column {
                    Text("El PDF se ha generado exitosamente")
                    Spacer(modifier = Modifier.height(8.dp))
                    pdfState.pdfFile?.let { file ->
                        Text(
                            text = "Archivo: ${file.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Tamaño: ${file.length() / 1024} KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.resetState()
                        onGeneratePdf()
                    }
                ) {
                    Text("Continuar")
                }
            }
        )
    }

    // Diálogo de error
    if (pdfState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            icon = {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Error al generar PDF") },
            text = { Text(pdfState.error ?: "Error desconocido") },
            confirmButton = {
                Button(onClick = { viewModel.resetState() }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

@Composable
fun PagePreviewItem(
    imageUri: Uri,
    pageNumber: Int,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            Column {
                // Badge de número de página
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Página $pageNumber",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Imagen
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Página $pageNumber",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}