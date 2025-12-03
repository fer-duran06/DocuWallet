package com.docuwallet.app.presentacion.views.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.docuwallet.app.data.local.entity.DocumentEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    document: DocumentEntity,
    onNavigateBack: () -> Unit,
    onOpenDocument: (String, String) -> Unit
) {
    val daysRemaining = if (document.expiryDate != null) {
        calculateDaysRemaining(document.expiryDate!!)
    } else {
        null
    }

    val isExpiringSoon = daysRemaining != null && daysRemaining in 0..30
    val isExpired = daysRemaining != null && daysRemaining < 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dentro de un documento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "━━━━",
                                    style = MaterialTheme.typography.bodySmall,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = document.name,  // ✅ CORREGIDO
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                DetailRow("ID", "001")
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Categoría", document.category)
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (document.expiryDate != null) {
                    DetailRow(
                        "Fecha de Vencimiento",
                        formatDateLong(document.expiryDate!!)
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    if (daysRemaining != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Días Restantes",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when {
                                    isExpired -> "${abs(daysRemaining)} (vencido)"
                                    else -> "$daysRemaining"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isExpired -> MaterialTheme.colorScheme.error
                                    isExpiringSoon -> Color(0xFFFFA500)
                                    else -> Color(0xFF4CAF50)
                                }
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                DetailRow("Tamaño", "%.1f KB".format(document.fileSize / 1024.0))
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Páginas", "${document.pageCount}")
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Veces Accedido", "5 veces")
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Creado", formatDateLong(document.createdAt))

                if (document.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Notas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = document.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (!document.isSynced) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Documento guardado solo localmente. Activa Firebase Storage para sincronizar.",