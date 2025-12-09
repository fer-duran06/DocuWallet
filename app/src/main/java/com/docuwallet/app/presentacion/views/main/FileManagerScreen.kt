package com.docuwallet.app.presentacion.views.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuwallet.app.data.local.entity.DocumentEntity
import com.docuwallet.app.presentacion.viewmodels.FileManagerViewModel
import com.docuwallet.app.presentacion.viewmodels.FileManagerViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

data class DocumentItem(
    val id: String,
    val name: String,
    val category: String,
    val icon: ImageVector,
    val iconColor: Color,
    val expiryDate: String,
    val size: String
)

fun DocumentEntity.toDocumentItem(): DocumentItem {
    val (icon, iconColor) = when (category) {
        "Facturas" -> Icons.Default.Receipt to Color(0xFF4CAF50)
        "Identificación" -> Icons.Default.Person to Color(0xFF2196F3)
        "Contratos" -> Icons.Default.Description to Color(0xFFFF9800)
        else -> Icons.Default.Article to Color.Gray
    }
    val formattedDate = expiryDate?.let {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
    } ?: "Sin vencimiento"

    val formattedSize = "${(fileSize / 1024)} KB"

    return DocumentItem(
        id = id,
        name = name,
        category = category,
        icon = icon,
        iconColor = iconColor,
        expiryDate = formattedDate,
        size = formattedSize
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: FileManagerViewModel = viewModel(factory = FileManagerViewModelFactory(context))

    val documentsFromDb by viewModel.documents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDocuments by viewModel.selectedDocuments.collectAsState()
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    val documents = documentsFromDb.map { it.toDocumentItem() }
    val selectedCount = selectedDocuments.size
    val hasSelection = selectedCount > 0
    val allSelected = documents.isNotEmpty() && selectedCount == documents.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (hasSelection) {
                        Text("$selectedCount Seleccionados")
                    } else {
                        Text("Gestión de Archivos")
                    }
                },
                navigationIcon = {
                    if (hasSelection) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar selección")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (allSelected) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.selectAllDocuments()
                            }
                        },
                        enabled = documents.isNotEmpty() // Correcto: Se deshabilita si no hay documentos, pero sigue visible.
                    ) {
                        Icon(
                            imageVector = if (allSelected) Icons.Default.CheckBox else Icons.Default.SelectAll,
                            contentDescription = if (allSelected) "Deseleccionar Todo" else "Seleccionar Todo"
                        )
                    }

                    if (hasSelection) {
                        IconButton(onClick = { viewModel.toggleFavoriteForSelected() }) {
                            Icon(Icons.Default.Star, "Favorito", tint = Color(0xFFFF9800))
                        }
                        IconButton(onClick = { viewModel.shareSelectedDocuments() }) {
                            Icon(Icons.Default.Share, "Compartir", tint = Color(0xFF4CAF50))
                        }
                        IconButton(onClick = { showDeleteConfirmationDialog = true }) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = Color(0xFFF44336))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Buscar por nombre, categoría o ID...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(25.dp),
                leadingIcon = {
                    Icon(Icons.Filled.Search, "Buscar")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron documentos.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(documents, key = { it.id }) { document ->
                        DocumentCard(
                            document = document,
                            isSelected = selectedDocuments.contains(document.id),
                            onToggleSelection = { viewModel.toggleDocumentSelection(document.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar $selectedCount documento(s)? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelectedDocuments()
                        showDeleteConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmationDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentCard(
    document: DocumentItem,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onToggleSelection
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        document.iconColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    document.icon,
                    contentDescription = null,
                    tint = document.iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = document.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = document.expiryDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }

            Text(text = document.size, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
