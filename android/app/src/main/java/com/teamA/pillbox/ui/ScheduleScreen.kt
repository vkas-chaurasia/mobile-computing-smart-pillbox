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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teamA.pillbox.ui.components.*
import com.teamA.pillbox.viewmodel.ScheduleUiState
import com.teamA.pillbox.viewmodel.ScheduleViewModel

/**
 * Allows users to configure medication schedule (days, time, name).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = viewModel(factory = ScheduleViewModel.Factory(
        androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
    )),
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCompartment by viewModel.selectedCompartment.collectAsState()
    val selectedDays by viewModel.selectedDays.collectAsState()
    val selectedTime by viewModel.selectedTime.collectAsState()
    val medicationName by viewModel.medicationName.collectAsState()
    val isValid by viewModel.isValid.collectAsState()
    
    // Get all schedules for display (reactive from repository)
    val allSchedules by viewModel.allSchedules.collectAsState()
    
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medication Schedule", fontWeight = FontWeight.Bold) },
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
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        enabled = allSchedules.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Reset Schedule"
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // All Schedules Grouped by Compartment
            if (allSchedules.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Spacer(modifier = Modifier.height(16.dp))
                        SchedulesGroupedByCompartment(
                            schedules = allSchedules
                        )
                    }
                }
            } else {
                // Empty state message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No schedules set",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Configure your medication schedule below",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Error state
            when (val state = uiState) {
                is ScheduleUiState.Error -> {
                    val errorMessage = state.message
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                else -> { /* Other states handled above */ }
            }

            // Schedule Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Schedule Configuration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Compartment Selector
                    CompartmentSelector(
                        selectedCompartment = selectedCompartment,
                        onCompartmentSelected = viewModel::updateSelectedCompartment
                    )

                    // Medication Name (Optional)
                    OutlinedTextField(
                        value = medicationName,
                        onValueChange = viewModel::updateMedicationName,
                        label = { Text("Medication Name (Optional)") },
                        placeholder = { Text("Enter medication name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null
                            )
                        }
                    )

                    // Days of Week Selector
                    DaysOfWeekSelector(
                        selectedDays = selectedDays,
                        onDaysChanged = viewModel::updateSelectedDays
                    )

                    // Time Picker
                    Column {
                        Text(
                            text = "Select Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        TimePickerButton(
                            selectedTime = selectedTime,
                            onTimeSelected = viewModel::updateSelectedTime
                        )
                    }

                    // Save Button
                    // Check if there's an existing schedule for the selected compartment
                    val hasExistingSchedule = selectedCompartment?.let { comp ->
                        allSchedules.any { it.compartmentNumber == comp }
                    } ?: false
                    
                    Button(
                        onClick = { viewModel.saveSchedule() },
                        enabled = isValid,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (hasExistingSchedule) Icons.Default.Edit else Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasExistingSchedule) "Update Schedule" else "Save Schedule"
                        )
                    }

                    // Validation message
                    if (!isValid && (selectedCompartment != null || selectedDays.isNotEmpty() || selectedTime != null)) {
                        val missingFields = mutableListOf<String>()
                        if (selectedCompartment == null) missingFields.add("compartment")
                        if (selectedDays.isEmpty()) missingFields.add("days")
                        if (selectedTime == null) missingFields.add("time")
                        
                        Text(
                            text = "Please select: ${missingFields.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Schedule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedCompartment == null) {
                        Text(
                            text = "No slot selected. Please select a slot to reset, or reset all schedules.",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Are you sure you want to reset the schedule for Slot $selectedCompartment? This action cannot be undone.")
                    }
                    if (allSchedules.isNotEmpty()) {
                        Text(
                            text = "You can also reset all schedules at once.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            confirmButton = {
                // Show appropriate buttons based on state
                if (selectedCompartment != null && allSchedules.isNotEmpty()) {
                    // Both options available: show in a Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.resetSchedule()
                                showResetDialog = false
                            }
                        ) {
                            Text("Reset Slot $selectedCompartment", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(
                            onClick = {
                                viewModel.resetAllSchedules()
                                showResetDialog = false
                            }
                        ) {
                            Text("Reset All", color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else if (selectedCompartment != null) {
                    // Only reset selected slot
                    TextButton(
                        onClick = {
                            viewModel.resetSchedule()
                            showResetDialog = false
                        }
                    ) {
                        Text("Reset Slot $selectedCompartment", color = MaterialTheme.colorScheme.error)
                    }
                } else if (allSchedules.isNotEmpty()) {
                    // Only reset all available
                    TextButton(
                        onClick = {
                            viewModel.resetAllSchedules()
                            showResetDialog = false
                        }
                    ) {
                        Text("Reset All", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    // No action available (shouldn't happen, but handle gracefully)
                    TextButton(
                        onClick = { showResetDialog = false }
                    ) {
                        Text("OK")
                    }
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

