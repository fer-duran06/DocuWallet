package com.docuwallet.app.presentacion.views.main

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.docuwallet.app.data.local.entity.DocumentEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    document: DocumentEntity,
    onNavigateBack: () -> Unit,
    onDocumentOpened: () -> Unit = {}
) {
    val context = LocalContext.current

    // Calcular días restantes solo si existe expiryDate
    val daysRemaining = if (document.expiryDate != null) {
        calculateDaysRemaining(document.expiryDate!!)
    } else {
        null
    }

    val isExpiringSoon = daysRemaining != null && daysRemaining in 0..30
    val isExpired = daysRemaining != null && daysRemaining < 0

    // Generar ID único basado en timestamp del documento
    val documentIdShort = document.id.takeLast(8).uppercase()

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
            // Header con icono y nombre
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
                    // Icono del documento
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
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = document.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Detalles del documento
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // ID (basado en timestamp)
                DetailRow("ID", documentIdShort)

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Categoría
                DetailRow("Categoría", document.category)

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Fecha de vencimiento (solo si existe)
                if (document.expiryDate != null) {
                    DetailRow(
                        "Fecha de Vencimiento",
                        formatDateLong(document.expiryDate!!)
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Días restantes (solo si existe expiryDate)
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

                // Tamaño
                DetailRow("Tamaño", "%.1f KB".format(document.fileSize / 1024.0))

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Páginas
                DetailRow("Páginas", "${document.pageCount}")

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Veces accedido (contador real)
                DetailRow("Veces Accedido", "${document.accessCount} veces")

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Fecha de creación
                DetailRow("Creado", formatDateLong(document.createdAt))

                // Notas (si existen)
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

                // Estado de sincronización
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
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Botón Abrir Documento
                Button(
                    onClick = {
                        try {
                            val pdfFile = File(document.pdfLocalPath)

                            if (pdfFile.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    pdfFile
                                )

                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }

                                context.startActivity(intent)
                                onDocumentOpened()  // Incrementar contador
                            } else {
                                android.util.Log.e("DocumentDetail", "PDF no existe: ${document.pdfLocalPath}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("DocumentDetail", "Error al abrir PDF", e)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir Documento")
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun calculateDaysRemaining(expiryDate: Long): Int {
    val currentTime = System.currentTimeMillis()
    val diff = expiryDate - currentTime
    return (diff / (1000 * 60 * 60 * 24)).toInt()
}

private fun formatDateLong(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}