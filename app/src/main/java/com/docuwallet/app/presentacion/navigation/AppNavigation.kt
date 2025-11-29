package com.docuwallet.app.presentacion.navigation

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.docuwallet.app.presentacion.viewmodel.AuthViewModel
import com.docuwallet.app.presentacion.views.SplashScreen
import com.docuwallet.app.presentacion.views.auth.LoginScreen
import com.docuwallet.app.presentacion.views.auth.RegisterScreen
import com.docuwallet.app.presentacion.views.main.CameraScanScreen
import com.docuwallet.app.presentacion.views.main.FileManagerScreen
import com.docuwallet.app.presentacion.views.main.MainScreen
import com.docuwallet.app.presentacion.views.main.NewDocumentScreen
import com.docuwallet.app.presentacion.views.main.PagePreviewScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    // Estado para las imágenes capturadas
    var capturedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

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
                onNavigateToFileManager = {  // ← NUEVA NAVEGACIÓN
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

        // File Manager Screen
        composable(Routes.FILE_MANAGER) {
            FileManagerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // New Document Screen
        composable(Routes.NEW_DOCUMENT) {
            NewDocumentScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCamera = {
                    capturedImages = emptyList() // Limpiar imágenes previas
                    navController.navigate(Routes.CAMERA_SCAN)
                }
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

        // Page Preview Screen
        composable(Routes.PAGE_PREVIEW) {
            PagePreviewScreen(
                capturedImages = capturedImages,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onGeneratePdf = {
                    // TODO: Implementar generación de PDF
                    navController.navigate(Routes.NEW_DOCUMENT) {
                        popUpTo(Routes.NEW_DOCUMENT) { inclusive = true }
                    }
                },
                onDeletePage = { index ->
                    capturedImages = capturedImages.filterIndexed { i, _ -> i != index }
                }
            )
        }
    }
}