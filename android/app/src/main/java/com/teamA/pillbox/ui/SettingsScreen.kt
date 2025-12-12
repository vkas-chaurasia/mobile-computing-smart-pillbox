package com.teamA.pillbox.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamA.pillbox.domain.AppTheme
import com.teamA.pillbox.domain.CompartmentState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teamA.pillbox.ui.components.*
import com.teamA.pillbox.viewmodel.PillboxViewModel
import com.teamA.pillbox.viewmodel.SettingsViewModel

/**
 * Settings Screen - Screen 5
 * Allows users to configure app settings, sensor thresholds, and view device information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    pillboxViewModel: PillboxViewModel,
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(
        androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
    )),
    onNavigateBack: (() -> Unit)? = null
) {
    // Get settings from SettingsViewModel
    val selectedTheme by settingsViewModel.theme.collectAsState()
    val sensorThresholds by settingsViewModel.sensorThresholds.collectAsState()
    val compartment1State by settingsViewModel.compartment1State.collectAsState()
    val compartment2State by settingsViewModel.compartment2State.collectAsState()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsState()
    
    // Get live sensor values from PillboxViewModel
    val lightValue1 by pillboxViewModel.lightSensorValue.collectAsState()
    val lightValue2 by pillboxViewModel.lightSensorValue2.collectAsState()
    val tiltValue by pillboxViewModel.tiltSensorValue.collectAsState()
    val connectionState by pillboxViewModel.connectionState.collectAsState()
    val batteryLevel by pillboxViewModel.batteryLevel.collectAsState()
    val modelNumber by pillboxViewModel.modelNumber.collectAsState()
    val manufacturerName by pillboxViewModel.manufacturerName.collectAsState()
    
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Device Info Card
            DeviceInfoCard(
                isConnected = connectionState == com.teamA.pillbox.ble.Pillbox.State.READY,
                deviceName = "Pillbox Device",
                batteryLevel = if (connectionState == com.teamA.pillbox.ble.Pillbox.State.READY) batteryLevel else null,
                manufacturerName = manufacturerName,
                modelNumber = modelNumber,
                onDisconnect = { pillboxViewModel.disconnect() }
            )

            // Theme Selector
            ThemeSelectorCard(
                selectedTheme = selectedTheme,
                onThemeSelected = { settingsViewModel.setTheme(it) }
            )

            // Sensor Thresholds Section
            SensorThresholdsCard(
                lightThreshold1 = sensorThresholds.lightThreshold1,
                lightThreshold2 = sensorThresholds.lightThreshold2,
                tiltThreshold = sensorThresholds.tiltThreshold,
                lightValue1 = lightValue1,
                lightValue2 = lightValue2,
                tiltValue = tiltValue,
                onLightThreshold1Changed = { settingsViewModel.setLightThreshold1(it) },
                onLightThreshold2Changed = { settingsViewModel.setLightThreshold2(it) },
                onTiltThresholdChanged = { settingsViewModel.setTiltThreshold(it) }
            )

            // Compartment States Section
            CompartmentStatesCard(
                compartment1State = compartment1State,
                compartment2State = compartment2State,
                onCompartment1StateChanged = { settingsViewModel.setCompartmentState(1, it) },
                onCompartment2StateChanged = { settingsViewModel.setCompartmentState(2, it) }
            )

            // Notification Settings
            NotificationSettingsCard(
                notificationsEnabled = notificationsEnabled,
                onNotificationsEnabledChanged = { settingsViewModel.setNotificationsEnabled(it) }
            )

            // About Section
            AboutCard()

            // Reset All Data Button
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Data")
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Data") },
            text = { 
                Text("Are you sure you want to reset all data? This will delete all schedules, history, and settings. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.resetAllData()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
