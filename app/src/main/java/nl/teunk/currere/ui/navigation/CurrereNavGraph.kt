package nl.teunk.currere.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import nl.teunk.currere.CurrereApp
import nl.teunk.currere.ui.detail.DetailScreen
import nl.teunk.currere.ui.detail.DetailViewModel
import nl.teunk.currere.ui.diary.DiaryScreen
import nl.teunk.currere.ui.diary.DiaryViewModel
import nl.teunk.currere.ui.permission.HEALTH_PERMISSIONS
import nl.teunk.currere.ui.permission.PermissionScreen
import nl.teunk.currere.ui.scanner.QrScannerScreen
import nl.teunk.currere.ui.settings.SettingsScreen
import nl.teunk.currere.ui.settings.SettingsViewModel
import nl.teunk.currere.ui.setup.ManualSetupScreen
import nl.teunk.currere.ui.setup.SetupViewModel
import java.time.Instant

@Composable
fun CurrereNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as CurrereApp
    val client = HealthConnectClient.getOrCreate(context)
    val lifecycleOwner = LocalLifecycleOwner.current

    var startDestinationResolved by rememberSaveable { mutableStateOf(false) }
    var hasPermissions by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val granted = client.permissionController.getGrantedPermissions()
        hasPermissions = granted.containsAll(HEALTH_PERMISSIONS)
        startDestinationResolved = true
    }

    // Re-check permissions when app resumes (user may revoke via system settings)
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val granted = client.permissionController.getGrantedPermissions()
            if (!granted.containsAll(HEALTH_PERMISSIONS)) {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute != null && !currentRoute.contains("PermissionRoute")) {
                    navController.navigate(PermissionRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    if (!startDestinationResolved) return

    val startDestination: Any = if (hasPermissions) DiaryRoute else PermissionRoute

    // Shared SetupViewModel for QR scanner and manual setup flows
    val setupViewModel: SetupViewModel = viewModel {
        SetupViewModel(app.apiClient, app.credentialsManager, app)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<PermissionRoute> {
            PermissionScreen(
                onPermissionsGranted = {
                    navController.navigate(DiaryRoute) {
                        popUpTo<PermissionRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<DiaryRoute> {
            val diaryViewModel: DiaryViewModel = viewModel {
                DiaryViewModel(
                    runSessionRepository = app.runSessionRepository,
                    syncStatusStore = app.syncStatusStore,
                    syncRepository = app.syncRepository,
                    appContext = app,
                )
            }
            DiaryScreen(
                viewModel = diaryViewModel,
                onRunClick = { session ->
                    navController.navigate(
                        DetailRoute(
                            sessionId = session.id,
                            startTimeEpochMilli = session.startTime.toEpochMilli(),
                            endTimeEpochMilli = session.endTime.toEpochMilli(),
                        )
                    )
                },
                onSettingsClick = {
                    navController.navigate(SettingsRoute)
                },
            )
        }

        composable<DetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DetailRoute>()
            val detailViewModel: DetailViewModel = viewModel {
                DetailViewModel(
                    healthConnectSource = app.healthConnectSource,
                    sessionId = route.sessionId,
                    startTime = Instant.ofEpochMilli(route.startTimeEpochMilli),
                    endTime = Instant.ofEpochMilli(route.endTimeEpochMilli),
                )
            }
            DetailScreen(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable<SettingsRoute> {
            val settingsViewModel: SettingsViewModel = viewModel {
                SettingsViewModel(app.credentialsManager, app.syncStatusStore, app)
            }
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onScanQrCode = { navController.navigate(QrScannerRoute) },
                onManualSetup = { navController.navigate(ManualSetupRoute) },
            )
        }

        composable<QrScannerRoute> {
            QrScannerScreen(
                onQrScanned = { baseUrl, token ->
                    setupViewModel.connectWithCredentials(baseUrl, token)
                    navController.navigate(ManualSetupRoute) {
                        popUpTo<SettingsRoute>()
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<ManualSetupRoute> {
            ManualSetupScreen(
                viewModel = setupViewModel,
                onBack = { navController.popBackStack() },
                onConnected = {
                    navController.navigate(SettingsRoute) {
                        popUpTo<DiaryRoute>()
                    }
                },
            )
        }
    }
}
