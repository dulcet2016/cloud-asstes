package com.manha.eventassettracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.manha.eventassettracker.ui.screens.AllDataReportScreen
import com.manha.eventassettracker.ui.screens.AssetsScreen
import com.manha.eventassettracker.ui.screens.CompareScreen
import com.manha.eventassettracker.ui.screens.EventDetailScreen
import com.manha.eventassettracker.ui.screens.EventsScreen
import com.manha.eventassettracker.ui.screens.HomeScreen
import com.manha.eventassettracker.ui.screens.LoginScreen
import com.manha.eventassettracker.ui.screens.QrGeneratorScreen
import com.manha.eventassettracker.ui.screens.QrRegisterScreen
import com.manha.eventassettracker.ui.screens.ScanScreen
import com.manha.eventassettracker.ui.screens.SettingsScreen
import com.manha.eventassettracker.ui.screens.StaffScreen
import com.manha.eventassettracker.viewmodel.AppViewModel

@Composable
fun AppNavHost(viewModel: AppViewModel) {
    val sessionLoaded by viewModel.sessionLoaded.collectAsState()

    if (!sessionLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val session by viewModel.session.collectAsState()
    val navController: NavHostController = rememberNavController()
    val startDestination = if (session.isLoggedIn) Routes.HOME else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(viewModel = viewModel) {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }
        }
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                navigate = { route -> navController.navigate(route) },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.EVENTS) {
            EventsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenEvent = { id -> navController.navigate(Routes.eventDetail(id)) }
            )
        }
        composable(Routes.EVENT_DETAIL) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            EventDetailScreen(viewModel = viewModel, eventId = eventId, onBack = { navController.popBackStack() })
        }
        composable(Routes.ASSETS) {
            AssetsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.QR_GENERATE) {
            QrGeneratorScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.QR_REGISTER) {
            QrRegisterScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.STAFF) {
            StaffScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.COMPARE) {
            CompareScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.ALL_DATA_REPORT) {
            AllDataReportScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
