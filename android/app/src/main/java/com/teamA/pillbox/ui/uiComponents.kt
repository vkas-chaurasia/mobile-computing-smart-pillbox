package com.teamA.pillbox.ui

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import androidx.compose.foundation.clickable
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
import com.teamA.pillbox.viewmodel.PillboxViewModel
import com.teamA.pillbox.ble.Pillbox
import com.teamA.pillbox.domain.BoxState
import com.teamA.pillbox.domain.CompartmentState
import androidx.compose.material3.Typography


@Composable
fun PillboxTheme(content: @Composable () -> Unit) {
    val customColors = darkColorScheme(
        primary = Color(0xFF4CAF50),
        onPrimary = Color.White,
        secondary = Color(0xFFFF9800),
        surface = Color(0xFF212121),
        onSurface = Color.White,
        background = Color(0xFF121212)
    )

    MaterialTheme(
        colorScheme = customColors,
        typography = Typography(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillboxScannerScreen(
    uiState: PillboxViewModel.UiState,
    onScanClicked: () -> Unit,
    onDeviceSelected: (device: android.bluetooth.BluetoothDevice, name: String) -> Unit
) {

    val (isScanning, scannedDevices) = when (uiState) {
        is PillboxViewModel.UiState.Scanning -> uiState.isScanningActive to uiState.scannedDevices
        else -> false to emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pillbox Scanner", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
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
                    Text("No devices found. Tap 'Scan' to begin.", color = MaterialTheme.colorScheme.onBackground)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
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
    )
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val modelNumber by viewModel.modelNumber.collectAsState()
    val manufacturerName by viewModel.manufacturerName.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()

    val lightValue by viewModel.lightSensorValue.collectAsState()
    val lightValue2 by viewModel.lightSensorValue2.collectAsState()
    val tiltValue by viewModel.tiltSensorValue.collectAsState()

    // Medication schedule and history (placeholder - will be updated when repositories are connected)
    val scheduleUiState by scheduleViewModel.uiState.collectAsState()
    val currentSchedule = when (val state = scheduleUiState) {
        is com.teamA.pillbox.viewmodel.ScheduleUiState.Loaded -> state.schedule
        else -> null
    }
    val todayRecord = historyViewModel.getTodayRecord()

    // Placeholder: Get schedules for each compartment (will be replaced with real data later)
    // For now, we'll use the current schedule if it matches the compartment, otherwise null
    val schedule1 = currentSchedule?.takeIf { it.compartmentNumber == 1 }
    val schedule2 = currentSchedule?.takeIf { it.compartmentNumber == 2 }
    
    // Placeholder: Get today's records per compartment (will be replaced with real data later)
    val todayRecord1 = todayRecord?.takeIf { it.compartmentNumber == 1 }
    val todayRecord2 = todayRecord?.takeIf { it.compartmentNumber == 2 }

    // Placeholder: Compartment states (will be replaced with real data from SettingsRepository later)
    val compartment1State = remember { mutableStateOf(CompartmentState.LOADED) }
    val compartment2State = remember { mutableStateOf(CompartmentState.LOADED) }

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
                        compartmentState = compartment1State.value,
                        lightSensorValue = lightValue,
                        schedule = schedule1,
                        todayRecord = todayRecord1,
                        onMarkAsTaken = onMarkAsTaken1
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    // Compartment 2 Card
                    com.teamA.pillbox.ui.components.CompartmentCard(
                        compartmentNumber = 2,
                        compartmentState = compartment2State.value,
                        lightSensorValue = lightValue2,
                        schedule = schedule2,
                        todayRecord = todayRecord2,
                        onMarkAsTaken = onMarkAsTaken2
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    SensorDataCard(lightValue = lightValue, lightValue2 = lightValue2, tiltValue = tiltValue)
                    Spacer(Modifier.height(24.dp))
                    AuxiliaryDataCard(
                        manufacturerName = manufacturerName,
                        modelNumber = modelNumber
                    )
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
                        compartmentState = compartment1State.value,
                        lightSensorValue = 0, // N/A when disconnected
                        schedule = schedule1,
                        todayRecord = todayRecord1,
                        onMarkAsTaken = onMarkAsTaken1
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    // Compartment 2 Card (placeholder data when disconnected)
                    com.teamA.pillbox.ui.components.CompartmentCard(
                        compartmentNumber = 2,
                        compartmentState = compartment2State.value,
                        lightSensorValue = 0, // N/A when disconnected
                        schedule = schedule2,
                        todayRecord = todayRecord2,
                        onMarkAsTaken = onMarkAsTaken2
                    )
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
            Text("Compartment 1", style = MaterialTheme.typography.titleMedium)
            DataRow("Light Exposure:", "$lightValue %", Icons.Default.WbSunny)

            Spacer(Modifier.height(16.dp))

            // Compartment 2
            Text("Compartment 2", style = MaterialTheme.typography.titleMedium)
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
