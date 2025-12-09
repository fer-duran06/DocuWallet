package com.docuwallet.app.presentacion.views.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.docuwallet.app.data.local.database.AppDatabase
import com.docuwallet.app.data.repository.DocumentRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val repository = remember {
        val db = AppDatabase.getDatabase(context)
        DocumentRepository(db.documentDao(), context)
    }
    val scope = rememberCoroutineScope()

    var totalDocuments by remember { mutableStateOf(0) }
    var totalSpaceMB by remember { mutableStateOf(0.0) }
    var documentsExpiringSoon by remember { mutableStateOf(0) }
    var documentsByCategory by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var syncedDocuments by remember { mutableStateOf(0) }
    var localDocuments by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar estadísticas
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    // Obtener todos los documentos para análisis detallado
                    repository.getUserDocuments(userId).collect { documents ->
                        totalDocuments = documents.size
                        totalSpaceMB = documents.sumOf { it.fileSize } / (1024.0 * 1024.0)

                        // Documentos próximos a vencer (30 días)
                        val currentTime = System.currentTimeMillis()
                        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
                        documentsExpiringSoon = documents.count { doc ->
                            doc.expiryDate != null && doc.expiryDate!! - currentTime in 0..thirtyDaysInMillis
                        }

                        // Documentos por categoría
                        documentsByCategory = documents.groupBy { it.category }
                            .mapValues { it.value.size }

                        // Documentos sincronizados vs locales
                        syncedDocuments = documents.count { it.isSynced }
                        localDocuments = documents.count { !it.isSynced }

                        isLoading = false
                    }
                } else {
                    isLoading = false
                }
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "📊",
                    style = MaterialTheme.typography.displayLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Estadísticas",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Análisis de uso de tus documentos",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "General",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        StatsItem("Total de documentos", totalDocuments.toString())
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        StatsItem("Por vencer (30 días)", documentsExpiringSoon.toString())
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        StatsItem("Espacio usado", "%.2f MB".format(totalSpaceMB))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Sincronización",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        StatsItem("☁️ En la nube", syncedDocuments.toString())
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        StatsItem("📱 Solo local", localDocuments.toString())

                        if (localDocuments > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Activa Storage para sincronizar con la nube",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (documentsByCategory.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Por Categoría",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            documentsByCategory.entries.forEachIndexed { index, (category, count) ->
                                if (index > 0) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                }

                                val emoji = when(category) {
                                    "Identificación" -> "🪪"
                                    "Salud" -> "🏥"
                                    "Viajes" -> "✈️"
                                    "Educación" -> "🎓"
                                    "Trabajo" -> "💼"
                                    "Personal" -> "📄"
                                    else -> "📁"
                                }

                                StatsItem("$emoji $category", count.toString())
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "💡 Consejo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (totalDocuments == 0) {
                                "Comienza escaneando tu primer documento para mantener todo organizado y seguro."
                            } else if (documentsExpiringSoon > 0) {
                                "Revisa los documentos próximos a vencer para renovarlos a tiempo."
                            } else if (localDocuments > 0) {
                                "Activa Firebase Storage para hacer backup de tus documentos en la nube."
                            } else {
                                "¡Excelente! Tus documentos están organizados y sincronizados."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
