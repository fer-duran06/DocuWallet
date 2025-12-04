package com.docuwallet.app.presentacion.views.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.docuwallet.app.presentacion.viewmodels.DocumentsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onNavigateToNewDocument: () -> Unit,
    onNavigateToDocumentDetail: (String) -> Unit,
    viewModel: DocumentsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val categories = listOf(
        "Todos" to "📁",
        "Favoritos" to "⭐",
        "Identificación" to "🪪",
        "Salud" to "🏥",
        "Viajes" to "✈️",
        "Educación" to "🎓",
        "Trabajo" to "💼",
        "Personal" to "📄"
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewDocument,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Agregar documento")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Mis Documentos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filtros de categorías
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { (category, emoji) ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.loadDocuments(category) },
                        label = { Text("$emoji $category") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contenido
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.error ?: "Error desconocido",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                uiState.documents.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No hay documentos",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Escanea tu primer documento para comenzar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.documents) { document ->
                            DocumentCard(
                                document = document,
                                onFavoriteClick = {
                                    viewModel.toggleFavorite(document.id, document.isFavorite)
                                },
                                onDeleteClick = {
                                    viewModel.deleteDocument(document.id)
                                },
                                onClick = {
                                    onNavigateToDocumentDetail(document.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    document: DocumentEntity,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = document.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = document.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

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

            Spacer(modifier = Modifier.height(8.dp))

            if (document.notes.isNotBlank()) {
                Text(
                    text = document.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tamaño",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${document.fileSize / 1024} KB",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = "Páginas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${document.pageCount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = "Creado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatDate(document.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (!document.isSynced) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = "No sincronizado",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Local",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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