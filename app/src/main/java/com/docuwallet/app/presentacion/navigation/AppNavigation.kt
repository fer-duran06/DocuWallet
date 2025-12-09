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
import com.docuwallet.app.data.local.database.AppDatabase
import com.docuwallet.app.data.local.entity.DocumentEntity
import com.docuwallet.app.data.repository.DocumentRepository
import com.docuwallet.app.presentacion.viewmodel.AuthViewModel
import com.docuwallet.app.presentacion.viewmodels.DocumentUploadViewModel
import com.docuwallet.app.presentacion.viewmodels.ProfileViewModel
import com.docuwallet.app.presentacion.views.SplashScreen
import com.docuwallet.app.presentacion.views.auth.LoginScreen
import com.docuwallet.app.presentacion.views.auth.RegisterScreen
import com.docuwallet.app.presentacion.views.main.CameraScanScreen
import com.docuwallet.app.presentacion.views.main.ChangePasswordScreen
import com.docuwallet.app.presentacion.views.main.DocumentDetailScreen
import com.docuwallet.app.presentacion.views.main.EditProfileScreen
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
    var capturedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val documentUploadViewModel: DocumentUploadViewModel = viewModel()

    val startDestination = if (authViewModel.isUserLoggedIn()) Routes.HOME else Routes.SPLASH

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(navController = navController)
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.REGISTER) { inclusive = true } }
                }
            )
        }
        composable(Routes.HOME) {
            MainScreen(
                authViewModel = authViewModel,
                onNavigateToNewDocument = { navController.navigate(Routes.NEW_DOCUMENT) },
                onNavigateToFileManager = { navController.navigate(Routes.FILE_MANAGER) },
                onNavigateToDocumentDetail = { documentId ->
                    navController.navigate(Routes.documentDetail(documentId))
                },
                onNavigateToEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                onNavigateToChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }
        composable(Routes.EDIT_PROFILE) {
            val profileViewModel: ProfileViewModel = viewModel()
            val uiState by profileViewModel.uiState.collectAsState()
            EditProfileScreen(
                currentName = uiState.userName,
                currentEmail = uiState.userEmail,
                onNavigateBack = { navController.popBackStack() },
                onSave = { newName -> profileViewModel.updateUserName(newName) },
                isLoading = uiState.isLoading,
                error = uiState.error
            )
            LaunchedEffect(uiState.isSaveSuccess) {
                if (uiState.isSaveSuccess) navController.popBackStack()
            }
        }
        composable(Routes.CHANGE_PASSWORD) {
            val profileViewModel: ProfileViewModel = viewModel()
            val uiState by profileViewModel.uiState.collectAsState()
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onSave = { oldPass, newPass -> profileViewModel.changePassword(oldPass, newPass) },
                isLoading = uiState.isLoading,
                error = uiState.error
            )
            LaunchedEffect(uiState.isSaveSuccess) {
                if (uiState.isSaveSuccess) navController.popBackStack()
            }
        }
        composable(Routes.FILE_MANAGER) {
            FileManagerScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.NEW_DOCUMENT) {
            NewDocumentScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCamera = {
                    capturedImages = emptyList()
                    navController.navigate(Routes.CAMERA_SCAN)
                },
                onImagesSelected = { uris ->
                    capturedImages = uris
                    navController.navigate(Routes.PAGE_PREVIEW)
                },
                onNavigateToDocuments = {
                    capturedImages = emptyList()
                    documentUploadViewModel.resetPdfState()
                    documentUploadViewModel.resetSaveState()
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = false } }
                },
                capturedImages = capturedImages,
                viewModel = documentUploadViewModel
            )
        }
        composable(Routes.CAMERA_SCAN) {
            CameraScanScreen(
                onNavigateBack = { navController.popBackStack() },
                onImagesCapture = { images ->
                    capturedImages = images
                    navController.navigate(Routes.PAGE_PREVIEW)
                }
            )
        }
        composable(Routes.PAGE_PREVIEW) {
            PagePreviewScreen(
                capturedImages = capturedImages,
                onNavigateBack = { navController.popBackStack() },
                onGeneratePdf = { navController.popBackStack() },
                onDeletePage = { index ->
                    capturedImages = capturedImages.filterIndexed { i, _ -> i != index }
                },
                viewModel = documentUploadViewModel
            )
        }
        composable(
            route = Routes.DOCUMENT_DETAIL,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId")
            if (documentId == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            val context = LocalContext.current
            val repository = remember {
                val db = AppDatabase.getDatabase(context)
                DocumentRepository(db.documentDao(), context)
            }
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
                        isLoading = false
                    } catch (e: Exception) {
                        isLoading = false
                        error = e.message ?: "Error al cargar documento"
                    }
                }
            }
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error ?: "Error desconocido", color = MaterialTheme.colorScheme.error)
                    }
                }
                document != null -> {
                    DocumentDetailScreen(
                        document = document!!,
                        onNavigateBack = { navController.popBackStack() },
                        onDocumentOpened = {
                            scope.launch {
                                repository.incrementAccessCount(documentId)
                                document = repository.getDocumentById(documentId)
                            }
                        }
                    )
                }
            }
        }
    }
}