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
import com.teamA.pillbox.ui.components.*
import com.teamA.pillbox.viewmodel.PillboxViewModel

/**
 * Settings Screen - Screen 5
 * Allows users to configure app settings, sensor thresholds, and view device information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PillboxViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    // Placeholder state (will be replaced with SettingsViewModel later)
    val selectedTheme = remember { mutableStateOf(AppTheme.SYSTEM) }
    val lightThreshold1 = remember { mutableStateOf(40) }
    val lightThreshold2 = remember { mutableStateOf(40) }
    val tiltThreshold = remember { mutableStateOf(1) }
    val compartment1State = remember { mutableStateOf(CompartmentState.LOADED) }
    val compartment2State = remember { mutableStateOf(CompartmentState.LOADED) }
    val notificationsEnabled = remember { mutableStateOf(true) }
    
    // Get live sensor values from PillboxViewModel
    val lightValue1 by viewModel.lightSensorValue.collectAsState()
    val lightValue2 by viewModel.lightSensorValue2.collectAsState()
    val tiltValue by viewModel.tiltSensorValue.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val modelNumber by viewModel.modelNumber.collectAsState()
    val manufacturerName by viewModel.manufacturerName.collectAsState()
    
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
                onDisconnect = { viewModel.disconnect() }
            )

            // Theme Selector
            ThemeSelectorCard(
                selectedTheme = selectedTheme.value,
                onThemeSelected = { selectedTheme.value = it }
            )

            // Sensor Thresholds Section
            SensorThresholdsCard(
                lightThreshold1 = lightThreshold1.value,
                lightThreshold2 = lightThreshold2.value,
                tiltThreshold = tiltThreshold.value,
                lightValue1 = lightValue1,
                lightValue2 = lightValue2,
                tiltValue = tiltValue,
                onLightThreshold1Changed = { lightThreshold1.value = it },
                onLightThreshold2Changed = { lightThreshold2.value = it },
                onTiltThresholdChanged = { tiltThreshold.value = it }
            )

            // Compartment States Section
            CompartmentStatesCard(
                compartment1State = compartment1State.value,
                compartment2State = compartment2State.value,
                onCompartment1StateChanged = { compartment1State.value = it },
                onCompartment2StateChanged = { compartment2State.value = it }
            )

            // Notification Settings
            NotificationSettingsCard(
                notificationsEnabled = notificationsEnabled.value,
                onNotificationsEnabledChanged = { notificationsEnabled.value = it }
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
                        // Placeholder: Reset logic will be implemented when repositories are connected
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
