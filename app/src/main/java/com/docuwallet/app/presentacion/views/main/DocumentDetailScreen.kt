package com.docuwallet.app.presentacion.views.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

// As DocumentEntity couldn't be found, a placeholder is created based on its usage.
// Please replace with the correct import and definition.
data class DocumentEntity(
    val id: String = "001",
    val name: String,
    val category: String,
    val expiryDate: Date?,
    val fileSize: Long,
    val pageCount: Int,
    val createdAt: Date,
    val notes: String,
    val isSynced: Boolean,
    val documentUrl: String,
    val mimeType: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    document: DocumentEntity,
    onNavigateBack: () -> Unit,
    // The onOpenDocument parameter is not used. It can be removed if it is not needed.
    onOpenDocument: (String, String) -> Unit
) {
    val daysRemaining = document.expiryDate?.let { calculateDaysRemaining(it) }

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
                                    color = MaterialTheme.colorScheme.onPrimaryContainer // Corrected from tint to color
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                DetailRow("ID", document.id)
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Categoría", document.category)
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                document.expiryDate?.let {
                    DetailRow(
                        "Fecha de Vencimiento",
                        formatDateLong(it)
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    daysRemaining?.let {
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
                                    isExpired -> "${abs(it)} (vencido)"
                                    else -> "$it"
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

                DetailRow("Veces Accedido", "5 veces") // Placeholder value
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
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
            fontWeight = FontWeight.Bold
        )
    }
}

fun calculateDaysRemaining(expiryDate: Date): Long {
    val diff = expiryDate.time - System.currentTimeMillis()
    return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
}

fun formatDateLong(date: Date): String {
    val format = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault())
    return format.format(date)
}