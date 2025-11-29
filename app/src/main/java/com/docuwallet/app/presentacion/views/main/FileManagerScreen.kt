package com.docuwallet.app.presentacion.views.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class DocumentItem(
    val id: String,
    val name: String,
    val category: String,
    val icon: ImageVector,
    val iconColor: Color,
    val expiryDate: String,
    val size: String,
    var isSelected: Boolean = false,
    var isFavorite: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    onNavigateBack: () -> Unit
) {
    // Estado de los documentos
    var documents by remember {
        mutableStateOf(
            listOf(
                DocumentItem(
                    id = "1",
                    name = "INE",
                    category = "Identificación",
                    icon = Icons.Default.Badge,
                    iconColor = Color(0xFF2196F3),
                    expiryDate = "Vence: 15/06/2030",
                    size = "2.0 KB"
                ),
                DocumentItem(
                    id = "2",
                    name = "Pasaporte",
                    category = "Viajes",
                    icon = Icons.Default.Flight,
                    iconColor = Color(0xFF2196F3),
                    expiryDate = "Vence: 20/11/2025",
                    size = "3 MB"
                ),
                DocumentItem(
                    id = "3",
                    name = "Licencia de Conducir",
                    category = "Identificación",
                    icon = Icons.Default.Badge,
                    iconColor = Color(0xFF2196F3),
                    expiryDate = "Vence: 15/08/2030",
                    size = "1 MB"
                ),
                DocumentItem(
                    id = "4",
                    name = "Trabajo movil",
                    category = "Trabajo",
                    icon = Icons.Default.Work,
                    iconColor = Color(0xFF2196F3),
                    expiryDate = "sin vencimiento",
                    size = "5 MB"
                )
            )
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") }
    val selectedCount = documents.count { it.isSelected }
    val hasSelection = selectedCount > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (hasSelection) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFF9800)
                            ) {
                                Text(
                                    text = "$selectedCount Seleccionados",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        Text("Gestión de Archivos")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (hasSelection) {
                        // Botón Favorito
                        IconButton(
                            onClick = {
                                documents = documents.map {
                                    if (it.isSelected) it.copy(isFavorite = !it.isFavorite)
                                    else it
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Star,
                                "Favorito",
                                tint = Color(0xFFFF9800)
                            )
                        }

                        // Botón Compartir
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(
                                Icons.Default.Share,
                                "Compartir",
                                tint = Color(0xFF4CAF50)
                            )
                        }

                        // Botón Eliminar
                        IconButton(
                            onClick = {
                                documents = documents.filterNot { it.isSelected }
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                "Eliminar",
                                tint = Color(0xFFF44336)
                            )
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
            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar Documentos...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(25.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, "Buscar")
                },
                singleLine = true
            )

            // Filtros
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("Todos", "Favoritos", "Identificación", "Salud", "Viajes")

                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón Filtrar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FilterList, "Filtrar", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Filtrar", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de documentos
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(documents) { document ->
                    DocumentCard(
                        document = document,
                        onToggleSelection = {
                            documents = documents.map {
                                if (it.id == document.id) it.copy(isSelected = !it.isSelected)
                                else it
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    document: DocumentItem,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (document.isSelected) {
                    Modifier.background(
                        Color(0xFFFFF9C4),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (document.isSelected) Color(0xFFFFF9C4) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = document.isSelected,
                onCheckedChange = { onToggleSelection() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Icono
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

            // Información
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = document.expiryDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Tamaño
            Text(
                text = document.size,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}