package com.docuwallet.app.presentacion.views.main

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.docuwallet.app.utils.PermissionUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NewDocumentScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var documentName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("PERSONAL") }
    var notes by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Permisos
    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionUtils.ALL_PERMISSIONS.toList()
    )

    // Launcher para seleccionar imagen de galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
        }
    }

    // Categorías disponibles
    val categories = listOf(
        "IDENTIFICACION" to "🪪",
        "SALUD" to "🏥",
        "VIAJES" to "✈️",
        "EDUCACION" to "🎓",
        "TRABAJO" to "💼",
        "PERSONAL" to "📄"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Documento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección: Método de captura
            Text(
                text = "Seleccionar origen",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón: Escanear con cámara
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                    onClick = {
                        if (permissionsState.allPermissionsGranted) {
                            onNavigateToCamera()
                        } else {
                            permissionsState.launchMultiplePermissionRequest()
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
                            Icons.Default.PhotoCamera,
                            contentDescription = "Cámara",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Escanear",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Botón: Seleccionar de galería
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                    onClick = {
                        if (permissionsState.allPermissionsGranted) {
                            galleryLauncher.launch("image/*")
                        } else {
                            permissionsState.launchMultiplePermissionRequest()
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
                            Icons.Default.Image,
                            contentDescription = "Galería",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Galería",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Vista previa de imagen seleccionada
            selectedImageUri?.let { uri ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = "Vista previa",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Divider()

            // Nombre del documento
            Text(
                text = "Información del documento",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = documentName,
                onValueChange = { documentName = it },
                label = { Text("Nombre del documento") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Description, "Nombre")
                }
            )

            // Categoría
            Text(
                text = "Categoría",
                style = MaterialTheme.typography.bodyMedium
            )

            categories.forEach { (category, emoji) ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text("$emoji $category") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Notas
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                leadingIcon = {
                    Icon(Icons.Default.Notes, "Notas")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón guardar
            Button(
                onClick = {
                    // TODO: Implementar guardado
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = documentName.isNotEmpty() && selectedImageUri != null
            ) {
                Icon(Icons.Default.Save, "Guardar")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar Documento")
            }
        }
    }

    // Diálogo de permisos
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permisos necesarios") },
            text = { Text("Esta función requiere permisos de cámara y almacenamiento.") },
            confirmButton = {
                TextButton(onClick = {
                    permissionsState.launchMultiplePermissionRequest()
                    showPermissionDialog = false
                }) {
                    Text("Otorgar permisos")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}