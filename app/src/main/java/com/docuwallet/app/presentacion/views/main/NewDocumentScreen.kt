package com.docuwallet.app.presentacion.views.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
    var selectedExpiryDate by remember { mutableStateOf<Long?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagesDialog by remember { mutableStateOf(false) }

    val saveState by viewModel.saveState.collectAsState()
    val pdfState by viewModel.pdfState.collectAsState()

    val hasPdfGenerated = pdfState.pdfFile != null || capturedImages.isNotEmpty()
    val pageCount = capturedImages.size

    // ✅ CORREGIDO: Usar documentId en lugar de isSuccess
    LaunchedEffect(saveState.documentId) {
        if (saveState.documentId != null) {
            documentName = ""
            selectedCategory = ""
            notes = ""
            expiryDateString = ""
            selectedExpiryDate = null
            selectedImageUri = null

            viewModel.resetSaveState()
            onNavigateToDocuments()
        }
    }

    val cameraPermissions = rememberMultiplePermissionsState(
        permissions = listOf(android.Manifest.permission.CAMERA)
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${capturedImages.size} página(s) capturadas",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                    if (pdfState.pdfFile != null) {
                                        Text(
                                            text = "PDF: ${pdfState.pdfFile?.length()?.div(1024)} KB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            TextButton(onClick = { showImagesDialog = true }) {
                                Text("Ver fotos")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(capturedImages) { imageUri ->
                                Card(modifier = Modifier.size(80.dp)) {
                                    Image(
                                        painter = rememberAsyncImagePainter(imageUri),
                                        contentDescription = "Página",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onNavigateToCamera,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.CameraAlt, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Escanear nuevamente")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Text(
                    text = "Seleccionar origen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clickable {
                                if (cameraPermissions.allPermissionsGranted) {
                                    onNavigateToCamera()
                                } else {
                                    cameraPermissions.launchMultiplePermissionRequest()
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Escanear", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clickable {
                                galleryLauncher.launch("image/*")
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Image,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Galería", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = "Información del documento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = documentName,
                onValueChange = { documentName = it },
                label = { Text("Nombre del documento") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Notes, "Nombre")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Categoría",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val categories = listOf(
                "Identificación" to "🪪",
                "Salud" to "🏥",
                "Viajes" to "✈️",
                "Educación" to "🎓",
                "Trabajo" to "💼",
                "Personal" to "📄"
            )

            categories.chunked(2).forEach { rowCategories ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowCategories.forEach { (category, emoji) ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text("$emoji $category") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowCategories.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════════════
            // ✨ CAMPO DE FECHA DE VENCIMIENTO
            // ═══════════════════════════════════════════════════════════
            OutlinedTextField(
                value = expiryDateString,
                onValueChange = { }, // Read-only
                label = { Text("Fecha de vencimiento (opcional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                enabled = false,
                leadingIcon = {
                    Icon(Icons.Filled.CalendarToday, "Fecha")
                },
                trailingIcon = {
                    if (expiryDateString.isNotBlank()) {
                        IconButton(onClick = {
                            expiryDateString = ""
                            selectedExpiryDate = null
                        }) {
                            Icon(Icons.Filled.Clear, "Limpiar")
                        }
                    }
                },
                placeholder = { Text("Toca para seleccionar fecha") },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            if (expiryDateString.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📅 Este documento será incluido en las alertas de vencimiento",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            // ═══════════════════════════════════════════════════════════

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                leadingIcon = {
                    Icon(Icons.Filled.Edit, "Notas")
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if ((hasPdfGenerated || capturedImages.isNotEmpty()) && documentName.isNotBlank() && selectedCategory.isNotBlank()) {
                        // Convertir fecha a timestamp si existe
                        val expiryDateTimestamp = if (expiryDateString.isNotBlank()) {
                            parseDateToTimestamp(expiryDateString)
                        } else {
                            null
                        }

                        viewModel.saveDocumentWithExpiry(
                            context = context,
                            imageUris = capturedImages,
                            name = documentName,
                            category = selectedCategory,
                            notes = notes,
                            pageCount = pageCount,
                            expiryDate = expiryDateTimestamp
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (hasPdfGenerated || capturedImages.isNotEmpty()) && documentName.isNotBlank() && selectedCategory.isNotBlank() && !saveState.isLoading
            ) {
                if (saveState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardando...")
                } else {
                    Icon(Icons.Filled.Save, "Guardar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Documento")
                }
            }

            if (!hasPdfGenerated && capturedImages.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📷 Primero escanea o selecciona un documento",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ✨ DATE PICKER DIALOG
    // ═══════════════════════════════════════════════════════════
    if (showDatePicker) {
        SimpleDatePickerDialog(
            onDateSelected = { dateString ->
                expiryDateString = dateString
                selectedExpiryDate = parseDateToTimestamp(dateString)
                showDatePicker = false
            },
            onDismiss = {
                showDatePicker = false
            }
        )
    }

    if (showImagesDialog) {
        AlertDialog(
            onDismissRequest = { showImagesDialog = false },
            title = { Text("Páginas capturadas (${capturedImages.size})") },
            text = {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(capturedImages) { imageUri ->
                        Card {
                            Image(
                                painter = rememberAsyncImagePainter(imageUri),
                                contentDescription = "Página",
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(300.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showImagesDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (saveState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.resetSaveState() },
            icon = {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Error al guardar") },
            text = { Text(saveState.error ?: "Error desconocido") },
            confirmButton = {
                Button(onClick = { viewModel.resetSaveState() }) {
                    Text("Aceptar")
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════
// ✨ DATE PICKER SIMPLE
// ═══════════════════════════════════════════════════════════
@Composable
fun SimpleDatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDay by remember { mutableStateOf("01") }
    var selectedMonth by remember { mutableStateOf("01") }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fecha de Vencimiento") },
        text = {
            Column {
                Text(
                    "Selecciona día, mes y año:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Día
                    OutlinedTextField(
                        value = selectedDay,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) selectedDay = it },
                        label = { Text("Día") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Mes
                    OutlinedTextField(
                        value = selectedMonth,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) selectedMonth = it },
                        label = { Text("Mes") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    // Año
                    OutlinedTextField(
                        value = selectedYear,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) selectedYear = it },
                        label = { Text("Año") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Formato: DD/MM/AAAA",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dateString = "${selectedDay.padStart(2, '0')}/${selectedMonth.padStart(2, '0')}/$selectedYear"
                    onDateSelected(dateString)
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════
// ✨ FUNCIÓN HELPER PARA CONVERTIR FECHA A TIMESTAMP
// ═══════════════════════════════════════════════════════════
private fun parseDateToTimestamp(dateString: String): Long? {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.isLenient = false // Validación estricta
        val date = sdf.parse(dateString)
        date?.time
    } catch (e: Exception) {
        null
    }
}