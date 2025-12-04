package com.docuwallet.app.presentacion.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.docuwallet.app.data.local.entity.DocumentEntity
import com.docuwallet.app.data.repository.DocumentRepository
import com.docuwallet.app.presentacion.viewmodel.AuthViewModel
import com.docuwallet.app.presentacion.viewmodels.DocumentUploadViewModel
import com.docuwallet.app.presentacion.views.SplashScreen
import com.docuwallet.app.presentacion.views.auth.LoginScreen
import com.docuwallet.app.presentacion.views.auth.RegisterScreen
import com.docuwallet.app.presentacion.views.main.CameraScanScreen
import com.docuwallet.app.presentacion.views.main.DocumentDetailScreen
import com.docuwallet.app.presentacion.views.main.FileManagerScreen
import com.docuwallet.app.presentacion.views.main.MainScreen
import com.docuwallet.app.presentacion.views.main.NewDocumentScreen
import com.docuwallet.app.presentacion.views.main.PagePreviewScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    // Estado para las imágenes capturadas
    var capturedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // ViewModel compartido para todo el flujo de documentos
    val documentUploadViewModel: DocumentUploadViewModel = viewModel()

    val startDestination = if (authViewModel.isUserLoggedIn()) {
        Routes.HOME
    } else {
        Routes.SPLASH
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash Screen
        composable(Routes.SPLASH) {
            SplashScreen(navController = navController)
        }

        // Login Screen
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // Register Screen
        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        // Main Screen (con Bottom Navigation)
        composable(Routes.HOME) {
            MainScreen(
                authViewModel = authViewModel,
                onNavigateToNewDocument = {
                    navController.navigate(Routes.NEW_DOCUMENT)
                },
                onNavigateToFileManager = {
                    navController.navigate(Routes.FILE_MANAGER)
                },
                onNavigateToDocumentDetail = { documentId ->  // ← AGREGAR ESTE CALLBACK
                    navController.navigate(Routes.documentDetail(documentId))
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        // File Manager Screen
        composable(Routes.FILE_MANAGER) {
            FileManagerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // New Document Screen - COMPARTIR VIEWMODEL
        composable(Routes.NEW_DOCUMENT) {
            NewDocumentScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCamera = {
                    capturedImages = emptyList()
                    navController.navigate(Routes.CAMERA_SCAN)
                },
                onNavigateToDocuments = {
                    // Limpiar ViewModel después de guardar
                    capturedImages = emptyList()
                    documentUploadViewModel.resetPdfState()
                    documentUploadViewModel.resetSaveState()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                capturedImages = capturedImages,
                viewModel = documentUploadViewModel
            )
        }

        // Camera Scan Screen
        composable(Routes.CAMERA_SCAN) {
            CameraScanScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onImagesCapture = { images ->
                    capturedImages = images
                    navController.navigate(Routes.PAGE_PREVIEW)
                }
            )
        }

        // Page Preview Screen - COMPARTIR VIEWMODEL
        composable(Routes.PAGE_PREVIEW) {
            PagePreviewScreen(
                capturedImages = capturedImages,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onGeneratePdf = {
                    // Volver a NewDocumentScreen después de generar PDF
                    navController.popBackStack()
                },
                onDeletePage = { index ->
                    capturedImages = capturedImages.filterIndexed { i, _ -> i != index }
                },
                viewModel = documentUploadViewModel
            )
        }

        // Document Detail Screen - CON MANEJO DE ERRORES COMPLETO
        composable(
            route = Routes.DOCUMENT_DETAIL,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId")

            if (documentId == null) {
                // Si no hay ID, volver atrás
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
                return@composable
            }

            val context = LocalContext.current
            val repository = remember { DocumentRepository(context) }
            val scope = rememberCoroutineScope()

            var document by remember { mutableStateOf<DocumentEntity?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            var error by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(documentId) {
                scope.launch {
                    try {
                        isLoading = true
                        error = null
                        android.util.Log.d("AppNavigation", "Cargando documento: $documentId")

                        document = repository.getDocumentById(documentId)

                        if (document == null) {
                            error = "Documento no encontrado"
                            android.util.Log.w("AppNavigation", "Documento no encontrado: $documentId")
                        } else {
                            android.util.Log.d("AppNavigation", "Documento cargado: ${document!!.name}")
                        }

                        isLoading = false
                    } catch (e: Exception) {
                        isLoading = false
                        error = e.message ?: "Error al cargar documento"
                        android.util.Log.e("AppNavigation", "Error al cargar documento: $documentId", e)
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Cargando documento...")
                        }
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = error ?: "Error desconocido",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ID: $documentId",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Volver")
                            }
                        }
                    }
                }

                document != null -> {
                    DocumentDetailScreen(
                        document = document!!,
                        onNavigateBack = { navController.popBackStack() },
                        onDocumentOpened = {
                            // Incrementar contador de accesos
                            scope.launch {
                                repository.incrementAccessCount(documentId)
                                android.util.Log.d("AppNavigation", "Contador incrementado para: $documentId")

                                // Recargar documento para mostrar nuevo contador
                                document = repository.getDocumentById(documentId)
                            }
                        }
                    )
                }
            }
        }
    }
}