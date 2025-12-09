package com.docuwallet.app.presentacion.views.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.docuwallet.app.presentacion.components.BottomNavBar
import com.docuwallet.app.presentacion.navigation.Routes
import com.docuwallet.app.presentacion.viewmodel.AuthViewModel

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    onNavigateToNewDocument: () -> Unit,
    onNavigateToFileManager: () -> Unit,
    onNavigateToDocumentDetail: (String) -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToChangePassword: () -> Unit, // <-- AGREGADO
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            BottomNavBar(
                navController = navController,
                currentRoute = currentRoute
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToNewDocument = onNavigateToNewDocument,
                    viewModel = authViewModel,
                    onLogout = onLogout
                )
            }

            composable(Routes.DOCUMENTS) {
                DocumentsScreen(
                    onNavigateToNewDocument = onNavigateToNewDocument,
                    onNavigateToDocumentDetail = onNavigateToDocumentDetail
                )
            }

            composable(Routes.STATS) {
                StatsScreen()
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    viewModel = authViewModel,
                    onLogout = onLogout,
                    onNavigateToFileManager = onNavigateToFileManager,
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onNavigateToChangePassword = onNavigateToChangePassword // <-- AGREGADO
                )
            }
        }
    }
}