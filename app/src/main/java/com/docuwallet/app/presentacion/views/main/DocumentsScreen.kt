package com.docuwallet.app.presentacion.views.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuwallet.app.data.local.entity.DocumentEntity
import com.docuwallet.app.presentacion.viewmodels.DocumentsUiState
import com.docuwallet.app.presentacion.viewmodels.DocumentsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocumentsScreen(
    onNavigateToNewDocument: () -> Unit,
    onNavigateToDocumentDetail: (String) -> Unit,
    viewModel: DocumentsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasSelection = uiState.selectedDocuments.isNotEmpty()
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var pendingDeleteAction by remember { mutableStateOf<() -> Unit>({}) }

    Scaffold(
        topBar = {
            if (hasSelection) {
                SelectionTopAppBar(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDeleteRequest = {
                        pendingDeleteAction = { viewModel.deleteSelectedDocuments() }
                        showDeleteConfirmationDialog = true
                    }
                )
            }
        },
        floatingActionButton = {
            if (!hasSelection) {
                FloatingActionButton(
                    onClick = onNavigateToNewDocument,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Agregar documento")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (!hasSelection) {
                NormalModeContent(uiState, viewModel)
            }
            MainContent(
                uiState = uiState,
                onToggleSelection = { viewModel.toggleDocumentSelection(it) },
                onNavigateToDetail = onNavigateToDocumentDetail,
                onFavoriteClick = { id, isFav -> viewModel.toggleFavorite(id, isFav) },
                onDeleteClick = { documentId ->
                    pendingDeleteAction = { viewModel.deleteDocument(documentId) }
                    showDeleteConfirmationDialog = true
                }
            )
        }
    }

    if (showDeleteConfirmationDialog) {
        DeleteConfirmationDialog(
            count = if (hasSelection) uiState.selectedDocuments.size else 1,
            onConfirm = {
                pendingDeleteAction()
                showDeleteConfirmationDialog = false
            },
            onDismiss = { showDeleteConfirmationDialog = false }
        )
    }

    if (uiState.showShareDialog) {
        ShareDialog(
            onDismiss = { viewModel.onShareDialogDismiss() },
            onConfirm = { email -> viewModel.shareSelectedDocumentsWithEmail(email) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    uiState: DocumentsUiState,
    viewModel: DocumentsViewModel,
    onDeleteRequest: () -> Unit
) {
    val selectedCount = uiState.selectedDocuments.size
    val allSelected = selectedCount > 0 && selectedCount == uiState.documents.size
    var showShareMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("$selectedCount Seleccionados") },
        navigationIcon = {
            IconButton(onClick = { viewModel.clearSelection() }) {
                Icon(Icons.Default.Close, contentDescription = "Limpiar selección")
            }
        },
        actions = {
            IconButton(onClick = { viewModel.toggleFavoriteForSelected() }) {
                Icon(Icons.Default.Star, "Favorito")
            }

            // Botón de compartir con menú desplegable
            Box {
                IconButton(onClick = { showShareMenu = true }) {
                    Icon(Icons.Default.Share, "Compartir")
                }
                DropdownMenu(
                    expanded = showShareMenu,
                    onDismissRequest = { showShareMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Compartir con usuario") },
                        onClick = {
                            showShareMenu = false
                            viewModel.onShareRequest()
                        },
                        leadingIcon = { Icon(Icons.Default.PersonAdd, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Compartir enlace (WhatsApp, etc.)") },
                        onClick = {
                            showShareMenu = false
                            viewModel.shareSelectedDocumentsAsPublicLink()
                        },
                        leadingIcon = { Icon(Icons.Default.Link, null) }
                    )
                }
            }

            IconButton(onClick = onDeleteRequest) {
                Icon(Icons.Default.Delete, "Eliminar")
            }
            IconButton(onClick = { 
                if (allSelected) viewModel.clearSelection() else viewModel.selectAllDocuments()
            }) {
                Icon(
                    if (allSelected) Icons.Default.CheckBox else Icons.Default.SelectAll,
                    contentDescription = if (allSelected) "Deseleccionar todo" else "Seleccionar todo"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalModeContent(uiState: DocumentsUiState, viewModel: DocumentsViewModel) {
    val categories = listOf("Todos", "Favoritos", "Identificación", "Salud", "Viajes", "Educación", "Trabajo", "Personal")
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
        Text("Mis Documentos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("Buscar por nombre o categoría...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(25.dp),
            leadingIcon = { Icon(Icons.Filled.Search, "Buscar") },
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(category) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun MainContent(
    uiState: DocumentsUiState,
    onToggleSelection: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        uiState.error != null -> {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        }
        uiState.documents.isEmpty() && !uiState.isLoading -> {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No se encontraron documentos.")
            }
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.documents, key = { it.id }) { document ->
                    val isSelected = uiState.selectedDocuments.contains(document.id)
                    DocumentCard(
                        document = document,
                        isSelected = isSelected,
                        hasSelection = uiState.selectedDocuments.isNotEmpty(),
                        onToggleSelection = { onToggleSelection(document.id) },
                        onNavigateToDetail = { onNavigateToDetail(document.id) },
                        onFavoriteClick = { onFavoriteClick(document.id, document.isFavorite) },
                        onDeleteClick = { onDeleteClick(document.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentCard(
    document: DocumentEntity,
    isSelected: Boolean,
    hasSelection: Boolean,
    onToggleSelection: () -> Unit,
    onNavigateToDetail: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = { if (hasSelection) onToggleSelection() else onNavigateToDetail() },
            onLongClick = onToggleSelection
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (hasSelection) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelection() }, modifier = Modifier.padding(end = 12.dp))
            }
            Icon(Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(document.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(document.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(8.dp))
                Text("Creado: ${formatDate(document.createdAt)}", style = MaterialTheme.typography.bodySmall)
            }
            if (!hasSelection) {
                Row {
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            if (document.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            "Favorito",
                            tint = if (document.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val text = if (count > 1) "¿Estás seguro de que quieres eliminar los $count documentos seleccionados?" else "¿Estás seguro de que quieres eliminar este documento?"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Eliminación") },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Eliminar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compartir Documento") },
        text = {
            Column {
                Text("Introduce el email del usuario con quien quieres compartir.")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email del destinatario") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(email) }, enabled = email.isNotBlank() && "@" in email) {
                Text("Compartir")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
