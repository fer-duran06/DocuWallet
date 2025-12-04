package com.docuwallet.app.presentacion.views.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuwallet.app.data.repository.DocumentRepository
import com.docuwallet.app.presentacion.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToNewDocument: () -> Unit,
    viewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DocumentRepository(context) }
    val scope = rememberCoroutineScope()

    var totalDocuments by remember { mutableStateOf(0) }
    var totalSpaceMB by remember { mutableStateOf(0.0) }
    var documentsExpiringSoon by remember { mutableStateOf(0) }

    // Cargar estadísticas
    LaunchedEffect(Unit) {
        scope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val stats = repository.getStatistics(userId)
            totalDocuments = stats.totalDocuments
            totalSpaceMB = stats.totalStorageBytes / (1024.0 * 1024.0)

            // TODO: Calcular documentos próximos a vencer cuando agregues expiryDate
            documentsExpiringSoon = 0
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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
                    value = documentsExpiringSoon.toString()
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Alertas Recientes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (documentsExpiringSoon > 0) {
                        Text(
                            text = "⚠️ Tienes $documentsExpiringSoon documento(s) próximo(s) a vencer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "✅ No hay documentos próximos a vencer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String
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
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}