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

    val categories = listOf(
        "Todos", "Favoritos", "Identificación", "Salud", "Viajes", "Educación", "Trabajo", "Personal"
    )

    Scaffold(
        topBar = {
            if (hasSelection) {
                val allSelected = uiState.documents.isNotEmpty() && uiState.selectedDocuments.size == uiState.documents.size
                SelectionTopAppBar(
                    selectedCount = uiState.selectedDocuments.size,
                    isAllSelected = allSelected,
                    onCloseSelection = { viewModel.clearSelection() },
                    onToggleFavorite = { viewModel.toggleFavoriteForSelected() },
                    onShare = { viewModel.shareSelectedDocuments() },
                    onDelete = {
                        pendingDeleteAction = { viewModel.deleteSelectedDocuments() }
                        showDeleteConfirmationDialog = true
                    },
                    onSelectAll = {
                        if (allSelected) viewModel.clearSelection() else viewModel.selectAllDocuments()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Padding del Scaffold
        ) {
            if (!hasSelection) {
                NormalModeContent(uiState = uiState, viewModel = viewModel, categories = categories)
            }
            MainContent(
                uiState = uiState,
                hasSelection = hasSelection,
                onToggleSelection = { viewModel.toggleDocumentSelection(it) },
                onNavigateToDocumentDetail = onNavigateToDocumentDetail,
                onFavoriteClick = { id, isFav -> viewModel.toggleFavorite(id, isFav) },
                onDeleteClick = { documentId ->
                    pendingDeleteAction = { viewModel.deleteDocument(documentId) }
                    showDeleteConfirmationDialog = true
                }
            )
        }
    }

    if (showDeleteConfirmationDialog) {
        val text = if (hasSelection && uiState.selectedDocuments.isNotEmpty()) {
            val count = uiState.selectedDocuments.size
            if (count > 1) "¿Estás seguro de que quieres eliminar los $count documentos seleccionados?"
            else "¿Estás seguro de que quieres eliminar el documento seleccionado?"
        } else {
            "¿Estás seguro de que quieres eliminar este documento?"
        }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text(text) },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteAction()
                        showDeleteConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormalModeContent(
    uiState: DocumentsUiState,
    viewModel: DocumentsViewModel,
    categories: List<String>
) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
        Text(
            text = "Mis Documentos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("Buscar por nombre o categoría...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(25.dp),
            leadingIcon = { Icon(Icons.Filled.Search, "Buscar") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(category) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MainContent(
    uiState: DocumentsUiState,
    hasSelection: Boolean,
    onToggleSelection: (String) -> Unit,
    onNavigateToDocumentDetail: (String) -> Unit,
    onFavoriteClick: (String, Boolean) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        }
        uiState.documents.isEmpty() && !uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
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
                        hasSelection = hasSelection,
                        onToggleSelection = { onToggleSelection(document.id) },
                        onNavigateToDetail = { onNavigateToDocumentDetail(document.id) },
                        onFavoriteClick = { onFavoriteClick(document.id, document.isFavorite) },
                        onDeleteClick = { onDeleteClick(document.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onCloseSelection: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount Seleccionados") },
        navigationIcon = {
            IconButton(onClick = onCloseSelection) {
                Icon(Icons.Default.Close, contentDescription = "Limpiar selección")
            }
        },
        actions = {
            IconButton(onClick = onToggleFavorite) {
                Icon(Icons.Default.Star, "Favorito")
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, "Compartir")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Eliminar")
            }
            IconButton(onClick = onSelectAll) {
                Icon(
                    if (isAllSelected) Icons.Default.CheckBox else Icons.Default.SelectAll,
                    contentDescription = if (isAllSelected) "Deseleccionar todo" else "Seleccionar todo"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    )
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
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (hasSelection) onToggleSelection() else onNavigateToDetail()
                },
                onLongClick = onToggleSelection
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasSelection) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            Icon(
                Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = document.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = document.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Creado: ${formatDate(document.createdAt)}", style = MaterialTheme.typography.bodySmall)
            }

            if (!hasSelection) {
                Row {
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            if (document.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorito",
                            tint = if (document.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
