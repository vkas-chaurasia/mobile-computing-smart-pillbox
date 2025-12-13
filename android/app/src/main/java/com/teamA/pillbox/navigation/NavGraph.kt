package com.teamA.pillbox.navigation

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.teamA.pillbox.BlePermissionHelper
import com.teamA.pillbox.ui.*
import com.teamA.pillbox.viewmodel.PillboxViewModel
import kotlinx.coroutines.launch

/**
 * Main navigation graph for the app.
 * Handles navigation between all screens and bottom navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillboxNavGraph(
    viewModel: PillboxViewModel,
    permissionHelper: BlePermissionHelper,
    startDestination: String = NavigationRoutes.WELCOME
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Determine if we should show bottom navigation
    val showBottomNav = currentRoute in listOf(
        NavigationRoutes.DASHBOARD,
        NavigationRoutes.SCHEDULE,
        NavigationRoutes.HISTORY,
        NavigationRoutes.SETTINGS
    )

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNavigationBar(
                    currentRoute = currentRoute ?: "",
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Welcome Screen
            composable(NavigationRoutes.WELCOME) {
                val pairedDevices by viewModel.pairedDevices.collectAsState(initial = emptyList())
                val coroutineScope = rememberCoroutineScope()
                
                WelcomeScreen(
                    onGetStarted = {
                        navController.navigate(NavigationRoutes.DASHBOARD) {
                            popUpTo(NavigationRoutes.WELCOME) { inclusive = true }
                        }
                    },
                    onScanForDevice = {
                        navController.navigate(NavigationRoutes.SCANNER)
                    },
                    isDeviceConnected = viewModel.uiState.value is PillboxViewModel.UiState.Connected,
                    hasPairedDevices = pairedDevices.isNotEmpty(),
                    onAutoConnect = {
                        // Trigger auto-connect to most recent device
                        coroutineScope.launch {
                            val connected = viewModel.tryAutoConnect()
                            if (connected) {
                                // Navigation will happen automatically when connection succeeds
                                Log.d("NavGraph", "Auto-connect initiated")
                            }
                        }
                    }
                )
            }

            // Scanner Screen
            composable(NavigationRoutes.SCANNER) {
                val uiState by viewModel.uiState.collectAsState()
                val pairedDevices by viewModel.pairedDevices.collectAsState(initial = emptyList())
                
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    viewModel.onPermissionsResult(permissions.all { it.value }, permissionHelper)
                }

                val onScanClicked = {
                    if (permissionHelper.hasRequiredPermissions()) {
                        viewModel.startScan(permissionHelper)
                    } else {
                        permissionLauncher.launch(permissionHelper.getRequiredPermissions().toTypedArray())
                    }
                }

                when (val state = uiState) {
                    is PillboxViewModel.UiState.BluetoothDisabled -> {
                        BluetoothDisabledScreen(
                            onRequestEnable = { permissionHelper.requestEnableBluetooth() }
                        )
                    }
                    is PillboxViewModel.UiState.Connected -> {
                        // Navigate to dashboard when connected
                        LaunchedEffect(Unit) {
                            navController.navigate(NavigationRoutes.DASHBOARD) {
                                popUpTo(NavigationRoutes.SCANNER) { inclusive = true }
                            }
                        }
                    }
                    else -> {
                        PillboxScannerScreen(
                            uiState = state,
                            onScanClicked = onScanClicked,
                            onDeviceSelected = { device, name ->
                                viewModel.onDeviceSelected(device, name)
                                // Navigation to dashboard will happen automatically via LaunchedEffect above
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            pairedDevices = pairedDevices,
                            onConnectPairedDevice = { macAddress, deviceName ->
                                viewModel.connectToPairedDevice(macAddress, deviceName)
                            },
                            onUnpairDevice = { macAddress ->
                                viewModel.unpairDevice(macAddress)
                            }
                        )
                    }
                }
            }

            // Dashboard Screen
            composable(NavigationRoutes.DASHBOARD) {
                val uiState by viewModel.uiState.collectAsState()
                when (val state = uiState) {
                    is PillboxViewModel.UiState.Connected -> {
                        PillboxControlScreen(
                            viewModel = viewModel,
                            deviceName = state.deviceName ?: "Pillbox"
                        )
                    }
                    else -> {
                        // Show placeholder or navigate to scanner
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No device connected",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    navController.navigate(NavigationRoutes.SCANNER)
                                }
                            ) {
                                Text("Scan for Device")
                            }
                        }
                    }
                }
            }

            // Schedule Screen
            composable(NavigationRoutes.SCHEDULE) {
                ScheduleScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // History Screen
            composable(NavigationRoutes.HISTORY) {
                HistoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Settings Screen
            composable(NavigationRoutes.SETTINGS) {
                com.teamA.pillbox.ui.SettingsScreen(
                    pillboxViewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

/**
 * Bottom Navigation Bar component.
 */
@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        val items = listOf(
            BottomNavItem(
                route = NavigationRoutes.DASHBOARD,
                label = "Dashboard",
                icon = Icons.Default.Home
            ),
            BottomNavItem(
                route = NavigationRoutes.SCHEDULE,
                label = "Schedule",
                icon = Icons.Default.Schedule
            ),
            BottomNavItem(
                route = NavigationRoutes.HISTORY,
                label = "History",
                icon = Icons.Default.History
            ),
            BottomNavItem(
                route = NavigationRoutes.SETTINGS,
                label = "Settings",
                icon = Icons.Default.Settings
            )
        )

        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}

/**
 * Data class for bottom navigation items.
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

