package com.docuwallet.app.presentacion.views.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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

data class Document(
    val id: String,
    val name: String,
    val category: String,
    val categoryIcon: ImageVector,
    val categoryColor: Color,
    val expiryDate: String,
    val daysUntilExpiry: Int,
    val isFavorite: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onNavigateToNewDocument: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }

    // Documentos de ejemplo (hardcoded temporalmente)
    val allDocuments = remember {
        listOf(
            Document(
                id = "1",
                name = "INE",
                category = "Identificación",
                categoryIcon = Icons.Default.Badge,
                categoryColor = Color(0xFF2196F3),
                expiryDate = "Vence: 15/06/2030",
                daysUntilExpiry = 1500,
                isFavorite = false
            ),
            Document(
                id = "2",
                name = "Pasaporte",
                category = "Viajes",
                categoryIcon = Icons.Default.Flight,
                categoryColor = Color(0xFF2196F3),
                expiryDate = "Vence: 20/11/2025",
                daysUntilExpiry = 45,
                isFavorite = false
            ),
            Document(
                id = "3",
                name = "Licencia de Conducir",
                category = "Identificación",
                categoryIcon = Icons.Default.Badge,
                categoryColor = Color(0xFF2196F3),
                expiryDate = "Vence: 15/08/2030",
                daysUntilExpiry = 1400,
                isFavorite = false
            ),
            Document(
                id = "4",
                name = "Cédula profesional",
                category = "Educación",
                categoryIcon = Icons.Default.School,
                categoryColor = Color(0xFF2196F3),
                expiryDate = "Vence: 31/05/2026",
                daysUntilExpiry = 95,
                isFavorite = true
            ),
            Document(
                id = "5",
                name = "Seguro Médico",
                category = "Salud",
                categoryIcon = Icons.Default.LocalHospital,
                categoryColor = Color(0xFF2196F3),
                expiryDate = "Vence: 02/11/26",
                daysUntilExpiry = 15,
                isFavorite = false
            ),
            Document(
                id = "6",
                name = "Trabajo móvil",
                category = "Trabajo",
                categoryIcon = Icons.Default.Work,
                categoryColor = Color(0xFF2196F3),
                expiryDate = "sin vencimiento",
                daysUntilExpiry = 9999,
                isFavorite = false
            ),
            Document(
                id = "7",
                name = "Cartilla",
                category = "Salud",
                categoryIcon = Icons.Default.LocalHospital,
                categoryColor = Color(0xFF2196F3),
                expiryDate = "sin vencimiento",
                daysUntilExpiry = 9999,
                isFavorite = true
            ),
            Document(
                id = "8",
                name = "Acta de Nacimiento",
                category = "Personal",
                categoryIcon = Icons.Default.Person,
                categoryColor = Color(0xFF2196F3),
                expiryDate = "sin vencimiento",
                daysUntilExpiry = 9999,
                isFavorite = false
            )
        )
    }

    // Filtrar documentos según búsqueda y categoría
    val filteredDocuments = allDocuments.filter { doc ->
        val matchesSearch = doc.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = when (selectedCategory) {
            "Todos" -> true
            "Favoritos" -> doc.isFavorite
            else -> doc.category == selectedCategory
        }
        matchesSearch && matchesCategory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Documentos") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewDocument,
                containerColor = Color(0xFFFF9800)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Nuevo Documento",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
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
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, "Limpiar")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // Filtros por categoría (Carrusel/Slider horizontal)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    "Todos",
                    "Favoritos",
                    "Identificación",
                    "Salud",
                    "Viajes",
                    "Educación",
                    "Trabajo",
                    "Personal"
                )

                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2196F3),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de documentos
            if (filteredDocuments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📂",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty() && selectedCategory == "Todos") {
                                "No tienes documentos"
                            } else {
                                "No se encontraron resultados"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isEmpty() && selectedCategory == "Todos") {
                                "Toca el botón + para agregar uno"
                            } else {
                                "Intenta con otra búsqueda o categoría"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDocuments) { document ->
                        DocumentCard(
                            document = document,
                            onClick = { /* TODO: Navegar a detalle */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    document: Document,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
            // Icono de categoría
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        document.categoryColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    document.categoryIcon,
                    contentDescription = null,
                    tint = document.categoryColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información del documento
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = document.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (document.isFavorite) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Favorito",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = document.expiryDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Indicador de vencimiento
            ExpiryIndicator(daysUntilExpiry = document.daysUntilExpiry)
        }
    }
}

@Composable
fun ExpiryIndicator(daysUntilExpiry: Int) {
    val (color, label) = when {
        daysUntilExpiry > 365 -> Color(0xFF4CAF50) to ""  // Verde
        daysUntilExpiry in 31..365 -> Color(0xFFFFC107) to "${daysUntilExpiry} días"  // Amarillo
        daysUntilExpiry in 1..30 -> Color(0xFFF44336) to "${daysUntilExpiry} días"  // Rojo
        else -> Color(0xFF4CAF50) to ""  // Sin vencimiento
    }

    if (label.isNotEmpty()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.2f)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, shape = CircleShape)
        )
    }
}