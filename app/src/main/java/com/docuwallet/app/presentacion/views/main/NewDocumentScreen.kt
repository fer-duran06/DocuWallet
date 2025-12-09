package com.docuwallet.app.presentacion.views.main

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import coil.compose.rememberAsyncImagePainter
import com.docuwallet.app.presentacion.viewmodels.DocumentUploadViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NewDocumentScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onImagesSelected: (List<Uri>) -> Unit,
    onNavigateToDocuments: () -> Unit,
    capturedImages: List<Uri>,
    viewModel: DocumentUploadViewModel
) {
    val context = LocalContext.current
    var documentName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expiryDateString by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showImagesDialog by remember { mutableStateOf(false) }

    val saveState by viewModel.saveState.collectAsState()
    val pdfState by viewModel.pdfState.collectAsState()

    val pageCount = capturedImages.size

    // --- PERMISOS ---
    val cameraPermissionState = rememberMultiplePermissionsState(permissions = listOf(Manifest.permission.CAMERA))
    val galleryPermissionState = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        rememberMultiplePermissionsState(permissions = listOf(Manifest.permission.READ_EXTERNAL_STORAGE))
    } else {
        null // No se necesita permiso en Android 13+
    }

    // --- LANZADORES ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                onImagesSelected(uris)
            }
        }
    )

    LaunchedEffect(saveState.documentId) {
        if (saveState.documentId != null) {
            viewModel.resetSaveState()
            onNavigateToDocuments()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Documento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (capturedImages.isNotEmpty()) {
                CapturedPagesSection(
                    capturedImages = capturedImages,
                    pdfState = pdfState,
                    onNavigateToCamera = onNavigateToCamera,
                    onShowImagesDialog = { showImagesDialog = true }
                )
            } else {
                SourceSelectionSection(
                    onCameraClick = {
                        if (cameraPermissionState.allPermissionsGranted) {
                            onNavigateToCamera()
                        } else {
                            cameraPermissionState.launchMultiplePermissionRequest()
                        }
                    },
                    onGalleryClick = {
                        if (galleryPermissionState == null || galleryPermissionState.allPermissionsGranted) {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else {
                            galleryPermissionState.launchMultiplePermissionRequest()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- FORMULARIO DEL DOCUMENTO ---
            DocumentInfoForm(
                documentName = documentName,
                onDocumentNameChange = { documentName = it },
                selectedCategory = selectedCategory,
                onCategoryChange = { selectedCategory = it },
                expiryDateString = expiryDateString,
                onShowDatePicker = { showDatePicker = true },
                onClearDate = { expiryDateString = "" },
                notes = notes,
                onNotesChange = { notes = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTÓN DE GUARDAR ---
            Button(
                onClick = {
                    viewModel.saveDocumentWithExpiry(
                        context = context,
                        imageUris = capturedImages,
                        name = documentName,
                        category = selectedCategory,
                        notes = notes,
                        pageCount = pageCount,
                        expiryDate = parseDateToTimestamp(expiryDateString)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = capturedImages.isNotEmpty() && documentName.isNotBlank() && selectedCategory.isNotBlank() && !saveState.isLoading
            ) {
                if (saveState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardando...")
                } else {
                    Icon(Icons.Filled.Save, "Guardar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Documento")
                }
            }
        }
    }

    // --- DIÁLOGOS ---
    if (showDatePicker) {
        SimpleDatePickerDialog(
            onDateSelected = { dateString ->
                expiryDateString = dateString
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showImagesDialog) {
        ImagePreviewDialog(images = capturedImages, onDismiss = { showImagesDialog = false })
    }

    if (saveState.error != null) {
        ErrorDialog(error = saveState.error!!, onDismiss = { viewModel.resetSaveState() })
    }
}

@Composable
fun SourceSelectionSection(onCameraClick: () -> Unit, onGalleryClick: () -> Unit) {
    Text("Seleccionar origen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedCard(modifier = Modifier.weight(1f).height(120.dp).clickable(onClick = onCameraClick)) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Escanear", fontWeight = FontWeight.Bold)
            }
        }
        OutlinedCard(modifier = Modifier.weight(1f).height(120.dp).clickable(onClick = onGalleryClick)) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.Image, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Galería", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CapturedPagesSection(
    capturedImages: List<Uri>,
    pdfState: DocumentUploadViewModel.PdfGenerationState,
    onNavigateToCamera: () -> Unit,
    onShowImagesDialog: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("${capturedImages.size} página(s) capturada(s)", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        if (pdfState.pdfFile != null) {
                            Text("PDF: ${pdfState.pdfFile.length() / 1024} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                TextButton(onClick = onShowImagesDialog) { Text("Ver fotos") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(capturedImages) { imageUri ->
                    Card(modifier = Modifier.size(80.dp)) {
                        Image(painter = rememberAsyncImagePainter(imageUri), "Página", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onNavigateToCamera, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AddAPhoto, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Añadir más páginas")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentInfoForm(
    documentName: String, onDocumentNameChange: (String) -> Unit,
    selectedCategory: String, onCategoryChange: (String) -> Unit,
    expiryDateString: String, onShowDatePicker: () -> Unit, onClearDate: () -> Unit,
    notes: String, onNotesChange: (String) -> Unit
) {
    Text("Información del documento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(value = documentName, onValueChange = onDocumentNameChange, label = { Text("Nombre del documento") }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Filled.Notes, "Nombre") })
    Spacer(modifier = Modifier.height(16.dp))
    Text("Categoría", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    Spacer(modifier = Modifier.height(8.dp))
    val categories = listOf("Identificación" to "🪪", "Salud" to "🏥", "Viajes" to "✈️", "Educación" to "🎓", "Trabajo" to "💼", "Personal" to "📄")
    categories.chunked(2).forEach { rowCategories ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rowCategories.forEach { (category, emoji) ->
                FilterChip(selected = selectedCategory == category, onClick = { onCategoryChange(category) }, label = { Text("$emoji $category") }, modifier = Modifier.weight(1f))
            }
            if (rowCategories.size == 1) Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(value = expiryDateString, onValueChange = {}, label = { Text("Fecha de vencimiento (opcional)") }, modifier = Modifier.fillMaxWidth().clickable(onClick = onShowDatePicker), enabled = false, leadingIcon = { Icon(Icons.Filled.CalendarToday, "Fecha") }, trailingIcon = { if (expiryDateString.isNotBlank()) IconButton(onClick = onClearDate) { Icon(Icons.Filled.Clear, "Limpiar") } }, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline, disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant, disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant))
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(value = notes, onValueChange = onNotesChange, label = { Text("Notas (opcional)") }, modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 5, leadingIcon = { Icon(Icons.Filled.Edit, "Notas") })
}

@Composable
fun SimpleDatePickerDialog(onDateSelected: (String) -> Unit, onDismiss: () -> Unit) {
    var selectedDay by remember { mutableStateOf("01") }
    var selectedMonth by remember { mutableStateOf("01") }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Fecha de Vencimiento") }, text = { Column { Text("Selecciona día, mes y año:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = selectedDay, onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) selectedDay = it }, label = { Text("Día") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(value = selectedMonth, onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) selectedMonth = it }, label = { Text("Mes") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(value = selectedYear, onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) selectedYear = it }, label = { Text("Año") }, modifier = Modifier.weight(1.5f), singleLine = true) }; Spacer(modifier = Modifier.height(8.dp)); Text("Formato: DD/MM/AAAA", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) } }, confirmButton = { Button(onClick = { onDateSelected("${selectedDay.padStart(2, '0')}/${selectedMonth.padStart(2, '0')}/$selectedYear") }) { Text("Aceptar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
fun ImagePreviewDialog(images: List<Uri>, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Páginas capturadas (${images.size})") }, text = { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(images) { imageUri -> Card { Image(painter = rememberAsyncImagePainter(imageUri), contentDescription = "Página", modifier = Modifier.width(200.dp).height(300.dp), contentScale = ContentScale.Fit) } } } }, confirmButton = { Button(onClick = onDismiss) { Text("Cerrar") } })
}

@Composable
fun ErrorDialog(error: String, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp)) }, title = { Text("Error") }, text = { Text(error) }, confirmButton = { Button(onClick = onDismiss) { Text("Aceptar") } })
}

private fun parseDateToTimestamp(dateString: String): Long? {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.isLenient = false
        sdf.parse(dateString)?.time
    } catch (e: Exception) {
        null
    }
}
