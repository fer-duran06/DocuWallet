package com.docuwallet.app.presentacion.views.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuwallet.app.presentacion.viewmodel.AuthViewModel
import com.docuwallet.app.presentacion.viewmodels.DocumentsViewModel
import com.docuwallet.app.presentacion.viewmodels.ProfileUiState
import com.docuwallet.app.presentacion.viewmodels.ProfileViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onLogout: () -> Unit,
    onNavigateToFileManager: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToChangePassword: () -> Unit // <-- AGREGADO
) {
    val profileViewModel: ProfileViewModel = viewModel()
    val profileState by profileViewModel.uiState.collectAsState()

    // ViewModel para los documentos
    val documentsViewModel: DocumentsViewModel = viewModel()
    val documentsState by documentsViewModel.uiState.collectAsState()

    // Lanzar efecto para cargar el perfil una sola vez
    LaunchedEffect(Unit) {
        profileViewModel.loadUserProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Perfil") })
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            if (profileState.isLoading && profileState.userName == null) { // Mostrar solo en la carga inicial
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (profileState.error != null) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(text = profileState.error!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                // Calcular estadísticas de documentos
                val documentCount = documentsState.documents.size
                val totalSize = documentsState.documents.sumOf { it.fileSize }
                val totalSizeMb = String.format(Locale.US, "%.2f", totalSize / (1024.0 * 1024.0))
                val totalAccessCount = documentsState.documents.sumOf { it.accessCount }

                // Contenido del perfil
                ProfileHeader(profileState)
                ProfileStats(documentCount, totalSizeMb, totalAccessCount)
                Divider()
                ProfileInfoSection(profileState)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSection(onNavigateToFileManager, onNavigateToChangePassword)
                Spacer(modifier = Modifier.weight(1f))
                ActionButtons(onLogout, onNavigateToEditProfile)
            }
        }
    }
}

@Composable
fun ProfileHeader(profileState: ProfileUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder para la imagen de perfil
                Icon(Icons.Default.AccountCircle, "Avatar", modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = profileState.userName ?: "Usuario",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = profileState.userEmail ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun ProfileStats(documentCount: Int, sizeUsedMb: String, accessCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ProfileStatCard(documentCount.toString(), "Documentos")
        ProfileStatCard(sizeUsedMb, "MB usados")
        ProfileStatCard(accessCount.toString(), "Accesos")
    }
}

@Composable
fun ProfileInfoSection(profileState: ProfileUiState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("👤 Información Personal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        ProfileInfoItem("Nombre completo", profileState.userName ?: "N/A")
        ProfileInfoItem("Correo", profileState.userEmail ?: "N/A")
    }
}

@Composable
fun SettingsSection(onNavigateToFileManager: () -> Unit, onNavigateToChangePassword: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("⚙️ Configuración", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        ProfileMenuItemClickable("Gestionar archivos", "Ver", Icons.Default.Folder, onClick = onNavigateToFileManager)
        ProfileMenuItemClickable("Cambiar contraseña", "", Icons.Default.Lock, onClick = onNavigateToChangePassword) // <-- CAMBIO AQUÍ
    }
}

@Composable
fun ActionButtons(onLogout: () -> Unit, onNavigateToEditProfile: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Button(
            onClick = onNavigateToEditProfile, 
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Editar Perfil")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Cerrar Sesión")
        }
    }
}

// --- COMPONENTES REUTILIZABLES ---

@Composable
fun ProfileStatCard(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ProfileInfoItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ProfileMenuItemClickable(label: String, action: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null)
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        Text(action, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}
