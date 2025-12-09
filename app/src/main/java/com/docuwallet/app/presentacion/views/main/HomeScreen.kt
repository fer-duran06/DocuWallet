package com.docuwallet.app.presentacion.views.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.docuwallet.app.data.local.database.AppDatabase
import com.docuwallet.app.data.local.entity.DocumentEntity
import com.docuwallet.app.data.repository.DocumentRepository
import com.docuwallet.app.presentacion.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToNewDocument: () -> Unit,
    viewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    // ✨ CORREGIDO: Se obtiene el DAO y se inyecta en el Repositorio.
    val repository = remember {
        val db = AppDatabase.getDatabase(context)
        DocumentRepository(db.documentDao(), context)
    }
    val scope = rememberCoroutineScope()

    var totalDocuments by remember { mutableStateOf(0) }
    var totalSpaceMB by remember { mutableStateOf(0.0) }
    var documentsExpiringSoon by remember { mutableStateOf(0) }
    var expiringDocuments by remember { mutableStateOf<List<DocumentEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

                // Estadísticas generales
                // Nota: Esta parte puede ser optimizada en el futuro
                repository.getUserDocuments(userId).collect { documents ->
                    totalDocuments = documents.size
                    totalSpaceMB = documents.sumOf { it.fileSize } / (1024.0 * 1024.0)

                    // Calcular documentos próximos a vencer (30 días)
                    val currentTime = System.currentTimeMillis()
                    val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000

                    expiringDocuments = documents.filter { doc ->
                        doc.expiryDate != null &&
                                doc.expiryDate!! > currentTime && // No vencidos
                                doc.expiryDate!! - currentTime <= thirtyDaysInMillis // Vencen en 30 días
                    }.sortedBy { it.expiryDate }

                    documentsExpiringSoon = expiringDocuments.size
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
                title = { Text("DocuWallet") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "Cerrar Sesión")
                    }
                }
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📄",
                    style = MaterialTheme.typography.displayLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Bienvenido a DocuWallet",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Gestiona tus documentos de forma segura",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Estadísticas REALES
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard(
                        title = "Documentos",
                        value = totalDocuments.toString()
                    )

                    StatCard(
                        title = "Alertas",
                        value = documentsExpiringSoon.toString(),
                        valueColor = if (documentsExpiringSoon > 0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )

                    StatCard(
                        title = "Espacio",
                        value = "%.1f MB".format(totalSpaceMB)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Alertas Recientes
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (documentsExpiringSoon > 0)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (documentsExpiringSoon > 0) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "Alertas Recientes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        when {
                            totalDocuments == 0 -> {
                                Text(
                                    text = "📄 Aún no tienes documentos. ¡Comienza escaneando tu primer documento!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            documentsExpiringSoon > 0 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "⚠️ Tienes $documentsExpiringSoon documento(s) próximo(s) a vencer:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Mostrar los primeros 3 documentos que vencen
                                    expiringDocuments.take(3).forEach { doc ->
                                        val daysRemaining = calculateDaysRemaining(doc.expiryDate!!)

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color.White.copy(alpha = 0.8f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = doc.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = doc.category,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }

                                                Text(
                                                    text = when {
                                                        daysRemaining == 0 -> "Vence hoy"
                                                        daysRemaining == 1 -> "1 día"
                                                        else -> "$daysRemaining días"
                                                    },
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        daysRemaining <= 7 -> MaterialTheme.colorScheme.error
                                                        daysRemaining <= 15 -> Color(0xFFFF9800)
                                                        else -> Color(0xFF4CAF50)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (expiringDocuments.size > 3) {
                                        Text(
                                            text = "...y ${expiringDocuments.size - 3} más",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = "✅ No hay documentos próximos a vencer en los próximos 30 días",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.size(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun calculateDaysRemaining(expiryDate: Long): Int {
    val currentTime = System.currentTimeMillis()
    val diff = expiryDate - currentTime
    return TimeUnit.MILLISECONDS.toDays(diff).toInt()
}
