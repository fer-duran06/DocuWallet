package com.docuwallet.app.presentacion.navigation

import android.content.Context
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.docuwallet.app.presentacion.viewmodel.AuthViewModel
import com.docuwallet.app.presentacion.viewmodels.DocumentUploadViewModel
import com.docuwallet.app.presentacion.views.SplashScreen
import com.docuwallet.app.presentacion.views.auth.LoginScreen
import com.docuwallet.app.presentacion.views.auth.RegisterScreen
import com.docuwallet.app.presentacion.views.main.CameraScanScreen
import com.docuwallet.app.presentacion.views.main.DocumentDetailScreen
import com.docuwallet.app.presentacion.views.main.DocumentEntity // Placeholder import
import com.docuwallet.app.presentacion.views.main.FileManagerScreen
import com.docuwallet.app.presentacion.views.main.MainScreen
import com.docuwallet.app.presentacion.views.main.NewDocumentScreen
import com.docuwallet.app.presentacion.views.main.PagePreviewScreen
import com.docuwallet.app.presentacion.views.main.PdfViewerScreen
import kotlinx.coroutines.launch
import java.util.Date

// Placeholder class, replace with your actual implementation
class DocumentRepository(context: Context) {
    fun getDocumentById(documentId: String): DocumentEntity? {
        // Replace with your actual data retrieval logic. 
        // This placeholder now returns null for a specific ID to avoid warnings.
        if (documentId == "not-found") return null
        return DocumentEntity(
            id = documentId,
            name = "Sample Document",
            category = "Personal",
            expiryDate = Date(),
            fileSize = 1024L,
            pageCount = 1,
            createdAt = Date(),
            notes = "This is a sample document.",
            isSynced = true,
            documentUrl = "",
            mimeType = "application/pdf"
        )
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    var capturedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
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
        composable(Routes.SPLASH) {
            SplashScreen(navController = navController)
        }

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

        composable(Routes.HOME) {
            MainScreen(
                authViewModel = authViewModel,
                onNavigateToNewDocument = {
                    navController.navigate(Routes.NEW_DOCUMENT)
                },
                onNavigateToFileManager = {
                    navController.navigate(Routes.FILE_MANAGER)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.FILE_MANAGER) {
            FileManagerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.NEW_DOCUMENT) {
            NewDocumentScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCamera = {
                    capturedImages = emptyList()
                    navController.navigate(Routes.CAMERA_SCAN)
                }
            )
        }

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

        composable(Routes.PAGE_PREVIEW) {
            PagePreviewScreen(
                capturedImages = capturedImages,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onGeneratePdf = {
                    navController.popBackStack()
                },
                onDeletePage = { index ->
                    capturedImages = capturedImages.filterIndexed { i, _ -> i != index }
                },
                viewModel = documentUploadViewModel
            )
        }

        // Document Detail Screen
        composable(
            route = Routes.DOCUMENT_DETAIL,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId")

            if (documentId == null) {
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
                        document = repository.getDocumentById(documentId)
                        if (document == null) {
                            error = "Documento no encontrado"
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Error desconocido"
                    } finally {
                        isLoading = false
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
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
                                text = error ?: "Error",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
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
                        onOpenDocument = { pdfPath, documentName ->
                            navController.navigate(Routes.pdfViewer(pdfPath, documentName))
                        }
                    )
                }
            }
        }

        // PDF Viewer Screen
        composable(
            route = Routes.PDF_VIEWER,
            arguments = listOf(
                navArgument("pdfPath") { type = NavType.StringType },
                navArgument("documentName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pdfPath = backStackEntry.arguments?.getString("pdfPath")
            val documentName = backStackEntry.arguments?.getString("documentName")

            if (pdfPath == null || documentName == null) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
                return@composable
            }

            val decodedPath = java.net.URLDecoder.decode(pdfPath, "UTF-8")
            val decodedName = java.net.URLDecoder.decode(documentName, "UTF-8")

            PdfViewerScreen(
                pdfPath = decodedPath,
                documentName = decodedName,
                onNavigateBack = { navController.popBackStack() },
                onShare = {
                    // TODO: Implementar compartir
                }
            )
        }
    }
}