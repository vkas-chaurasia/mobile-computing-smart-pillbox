package com.teamA.pillbox.ui

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamA.pillbox.domain.AppTheme
import com.teamA.pillbox.viewmodel.PillboxViewModel
import com.teamA.pillbox.ble.Pillbox
import com.teamA.pillbox.domain.BoxState
import com.teamA.pillbox.domain.CompartmentState
import androidx.compose.material3.Typography


@Composable
fun PillboxTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    
    // Determine if we should use dark theme based on the selected theme
    val useDarkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme
    }
    
    val customDarkColors = darkColorScheme(
        primary = Color(0xFF4CAF50),
        onPrimary = Color.White,
        secondary = Color(0xFFFF9800),
        surface = Color(0xFF212121),
        onSurface = Color.White,
        background = Color(0xFF121212)
    )
    
    val customLightColors = lightColorScheme(
        primary = Color(0xFF4CAF50),
        onPrimary = Color.White,
        secondary = Color(0xFFFF9800),
        surface = Color(0xFFF5F5F5),
        onSurface = Color(0xFF212121),
        background = Color.White
    )
    
    val colorScheme = if (useDarkTheme) customDarkColors else customLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillboxScannerScreen(
    uiState: PillboxViewModel.UiState,
    onScanClicked: () -> Unit,
    onDeviceSelected: (device: android.bluetooth.BluetoothDevice, name: String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    pairedDevices: List<com.teamA.pillbox.database.entities.PairedDeviceEntity> = emptyList(),
    onConnectPairedDevice: ((macAddress: String, deviceName: String) -> Unit)? = null,
    onUnpairDevice: ((macAddress: String) -> Unit)? = null
) {

    val (isScanning, scannedDevices) = when (uiState) {
        is PillboxViewModel.UiState.Scanning -> uiState.isScanningActive to uiState.scannedDevices
        else -> false to emptyList()
    }
    
    var showUnpairDialog by remember { mutableStateOf<com.teamA.pillbox.database.entities.PairedDeviceEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pillbox Scanner", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onScanClicked,
                icon = { Icon(Icons.Default.Search, contentDescription = "Scan") },
                text = { Text(if (isScanning) "Scanning..." else "Scan for Pillbox") },
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Paired Devices Section
            if (pairedDevices.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 16.dp, 16.dp, 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Paired Devices",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        pairedDevices.forEach { device ->
                            PairedDeviceListItem(
                                device = device,
                                onConnect = {
                                    onConnectPairedDevice?.invoke(device.macAddress, device.deviceName)
                                },
                                onForget = {
                                    showUnpairDialog = device
                                }
                            )
                            if (device != pairedDevices.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
                
                // Divider between paired and scan sections
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        "OR",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
            }
            
            // Scanning Section
            if (isScanning && scannedDevices.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Searching for Pillboxes...",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else if (!isScanning && scannedDevices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No devices found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap 'Scan' to search for new devices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Text(
                    "Available Devices",
                    modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scannedDevices) { result ->
                        DeviceListItem(result) { name ->
                            onDeviceSelected(result.device, name)
                        }
                    }
                }
            }
        }
        
        // Unpair Confirmation Dialog
        showUnpairDialog?.let { device ->
            AlertDialog(
                onDismissRequest = { showUnpairDialog = null },
                title = { Text("Forget Device?") },
                text = {
                    Text("Are you sure you want to forget ${device.deviceName}? You'll need to scan for it again to reconnect.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onUnpairDevice?.invoke(device.macAddress)
                            showUnpairDialog = null
                        }
                    ) {
                        Text("Forget", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUnpairDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}


@SuppressLint("MissingPermission")
@Composable
fun DeviceListItem(result: ScanResult, onClick: (String) -> Unit) {
    val deviceName = result.device.name ?: result.scanRecord?.deviceName ?: "N/A"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(deviceName) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = result.device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                text = "${result.rssi} dBm",
                style = MaterialTheme.typography.bodyMedium,
                color = if (result.rssi > -60) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillboxControlScreen(
    viewModel: PillboxViewModel,
    deviceName: String,
    scheduleViewModel: com.teamA.pillbox.viewmodel.ScheduleViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.teamA.pillbox.viewmodel.ScheduleViewModel.Factory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
    ),
    historyViewModel: com.teamA.pillbox.viewmodel.HistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.teamA.pillbox.viewmodel.HistoryViewModel.Factory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
    ),
    settingsViewModel: com.teamA.pillbox.viewmodel.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.teamA.pillbox.viewmodel.SettingsViewModel.Factory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val modelNumber by viewModel.modelNumber.collectAsState()
    val manufacturerName by viewModel.manufacturerName.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()

    val lightValue by viewModel.lightSensorValue.collectAsState()
    val lightValue2 by viewModel.lightSensorValue2.collectAsState()
    val tiltValue by viewModel.tiltSensorValue.collectAsState()

    // Get all schedules from repository (reactive)
    val allSchedules by scheduleViewModel.allSchedules.collectAsState()
    
    // Get schedules per compartment
    val schedule1 = allSchedules.firstOrNull { it.compartmentNumber == 1 }
    val schedule2 = allSchedules.firstOrNull { it.compartmentNumber == 2 }
    
    // Get today's records per compartment (reactive)
    val todayRecord1 by historyViewModel.getTodayRecord(1).collectAsState()
    val todayRecord2 by historyViewModel.getTodayRecord(2).collectAsState()

    // ***FIXED***: Get compartment states from SettingsViewModel instead of hardcoded values
    val compartment1State by settingsViewModel.compartment1State.collectAsState()
    val compartment2State by settingsViewModel.compartment2State.collectAsState()

    // Determine box state from tilt sensor (placeholder logic)
    val boxState = remember(tiltValue) {
        if (tiltValue >= 1) BoxState.OPEN else BoxState.CLOSED
    }

    LaunchedEffect(connectionState) {
        if (connectionState == Pillbox.State.READY) {
            viewModel.readDeviceInfo()
            viewModel.readBatteryLevel()
        }
    }

    // Handle "Mark as Taken" for compartment 1
    val onMarkAsTaken1: () -> Unit = {
        schedule1?.let { schedule ->
            val today = java.time.LocalDate.now()
            val now = java.time.LocalDateTime.now()
            
            // Create consumption record
            val record = com.teamA.pillbox.domain.ConsumptionRecord(
                id = java.util.UUID.randomUUID().toString(),
                compartmentNumber = 1,
                date = today,
                scheduledTime = schedule.time,
                consumedTime = now,
                status = com.teamA.pillbox.domain.ConsumptionStatus.TAKEN,
                detectionMethod = com.teamA.pillbox.domain.DetectionMethod.MANUAL
            )
            
            historyViewModel.createRecord(record)
        }
        Unit
    }

    // Handle "Mark as Taken" for compartment 2
    val onMarkAsTaken2: () -> Unit = {
        schedule2?.let { schedule ->
            val today = java.time.LocalDate.now()
            val now = java.time.LocalDateTime.now()
            
            // Create consumption record
            val record = com.teamA.pillbox.domain.ConsumptionRecord(
                id = java.util.UUID.randomUUID().toString(),
                compartmentNumber = 2,
                date = today,
                scheduledTime = schedule.time,
                consumedTime = now,
                status = com.teamA.pillbox.domain.ConsumptionStatus.TAKEN,
                detectionMethod = com.teamA.pillbox.domain.DetectionMethod.MANUAL
            )
            
            historyViewModel.createRecord(record)
        }
        Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deviceName, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                navigationIcon = {
                    IconButton(onClick = { viewModel.disconnect() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Disconnect"
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            when (connectionState) {
                Pillbox.State.LOADING -> ConnectionStatusCard(
                    status = "Connecting...",
                    icon = Icons.Default.Info
                )
                Pillbox.State.READY -> {
                    ConnectionStatusCard(
                        status = "Connected",
                        icon = Icons.Default.CheckCircle,
                        batteryLevel = batteryLevel
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    // Box State Indicator
                    com.teamA.pillbox.ui.components.BoxStateIndicator(boxState = boxState)
                    Spacer(Modifier.height(24.dp))
                    
                    // Compartment 1 Card
                    com.teamA.pillbox.ui.components.CompartmentCard(
                        compartmentNumber = 1,
                        compartmentState = compartment1State,
                        lightSensorValue = lightValue,
                        schedule = schedule1,
                        todayRecord = todayRecord1,
                        onMarkAsTaken = onMarkAsTaken1
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    // Compartment 2 Card
                    com.teamA.pillbox.ui.components.CompartmentCard(
                        compartmentNumber = 2,
                        compartmentState = compartment2State,
                        lightSensorValue = lightValue2,
                        schedule = schedule2,
                        todayRecord = todayRecord2,
                        onMarkAsTaken = onMarkAsTaken2
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    // ***NEW***: All Schedules Card
                    AllSchedulesCard(allSchedules = allSchedules)
                }
                Pillbox.State.NOT_AVAILABLE -> {
                    ConnectionStatusCard(
                        status = "Disconnected",
                        icon = Icons.Default.Close
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    // Box State Indicator (placeholder when disconnected)
                    com.teamA.pillbox.ui.components.BoxStateIndicator(boxState = BoxState.CLOSED)
                    Spacer(Modifier.height(24.dp))
                    
                    // Compartment 1 Card (placeholder data when disconnected)
                    com.teamA.pillbox.ui.components.CompartmentCard(
                        compartmentNumber = 1,
                        compartmentState = compartment1State,
                        lightSensorValue = 0, // N/A when disconnected
                        schedule = schedule1,
                        todayRecord = todayRecord1,
                        onMarkAsTaken = onMarkAsTaken1
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    // Compartment 2 Card (placeholder data when disconnected)
                    com.teamA.pillbox.ui.components.CompartmentCard(
                        compartmentNumber = 2,
                        compartmentState = compartment2State,
                        lightSensorValue = 0, // N/A when disconnected
                        schedule = schedule2,
                        todayRecord = todayRecord2,
                        onMarkAsTaken = onMarkAsTaken2
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    // ***NEW***: All Schedules Card (show even when disconnected)
                    AllSchedulesCard(allSchedules = allSchedules)
                }
            }
        }
    }
}


@Composable
fun ConnectionStatusCard(
    status: String,
    icon: ImageVector,
    batteryLevel: Int? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(status, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            if (batteryLevel != null) {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = "Battery Level",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(4.dp))
                Text("$batteryLevel%", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun AuxiliaryDataCard(
    manufacturerName: String,
    modelNumber: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Device Info",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            DataRow("Manufacturer:", manufacturerName, Icons.Default.Build)
            DataRow("Model:", modelNumber, Icons.Default.Home)
        }
    }
}

@Composable
fun SensorDataCard(lightValue: Int, lightValue2: Int, tiltValue: Int) {
    val tiltText = when (tiltValue) {
        1 -> "TILTED (Pillbox Moved)"
        0 -> "STABLE"
        else -> "N/A"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Live Sensor Data",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))

            // Compartment 1
            Text("Slot 1", style = MaterialTheme.typography.titleMedium)
            DataRow("Light Exposure:", "$lightValue %", Icons.Default.WbSunny)

            Spacer(Modifier.height(16.dp))

            // Compartment 2
            Text("Slot 2", style = MaterialTheme.typography.titleMedium)
            DataRow("Light Exposure:", "$lightValue2 %", Icons.Default.WbSunny)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            DataRow("Pillbox State:", tiltText, if (tiltValue == 1) Icons.Default.Warning else Icons.Default.Check)
        }
    }
}


@Composable
fun DataRow(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(140.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * All Schedules Card for Dashboard
 * Shows all medication schedules grouped by compartment
 */
@Composable
fun AllSchedulesCard(
    allSchedules: List<com.teamA.pillbox.domain.MedicationSchedule>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "All Schedules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            if (allSchedules.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No schedules set",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Go to Schedule screen to create one",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                com.teamA.pillbox.ui.components.SchedulesGroupedByCompartment(
                    schedules = allSchedules,
                    onScheduleClick = null // No click action on dashboard
                )
            }
        }
    }
}

@Composable
fun BluetoothDisabledScreen(onRequestEnable: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bluetooth is Disabled", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("This app requires Bluetooth to be enabled to scan for the Pillbox.", textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestEnable) {
            Text("Enable Bluetooth")
        }
    }
}

/**
 * List item for a paired device showing connection info and actions.
 */
@Composable
fun PairedDeviceListItem(
    device: com.teamA.pillbox.database.entities.PairedDeviceEntity,
    onConnect: () -> Unit,
    onForget: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.deviceName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = device.macAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Connected ${device.connectionCount} time${if (device.connectionCount != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Forget button
            IconButton(
                onClick = onForget,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Forget device",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            // Connect button
            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Connect")
            }
        }
    }
}

