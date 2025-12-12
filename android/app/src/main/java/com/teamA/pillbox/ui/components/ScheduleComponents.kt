package com.teamA.pillbox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek

/**
 * Days of week selector using filter chips.
 * Allows multi-selection of days.
 */
@Composable
fun DaysOfWeekSelector(
    selectedDays: Set<DayOfWeek>,
    onDaysChanged: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Select Days",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DayOfWeek.values().forEach { day ->
                val isSelected = selectedDays.contains(day)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newDays = if (isSelected) {
                            selectedDays - day
                        } else {
                            selectedDays + day
                        }
                        onDaysChanged(newDays)
                    },
                    label = {
                        Text(
                            text = day.name.take(3), // MON, TUE, etc.
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

/**
 * Time picker button that shows selected time and opens time picker dialog.
 */
@Composable
fun TimePickerButton(
    selectedTime: java.time.LocalTime?,
    onTimeSelected: (java.time.LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }
    
    OutlinedButton(
        onClick = { showTimePicker = true },
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = selectedTime?.let { 
                String.format("%02d:%02d", it.hour, it.minute)
            } ?: "Select Time",
            style = MaterialTheme.typography.bodyLarge
        )
    }
    
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = selectedTime ?: java.time.LocalTime.of(12, 0),
            onTimeSelected = { time ->
                onTimeSelected(time)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

/**
 * Time picker dialog using Material 3 TimePicker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: java.time.LocalTime,
    onTimeSelected: (java.time.LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = false // 12-hour format
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedTime = java.time.LocalTime.of(
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    onTimeSelected(selectedTime)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Compartment selector using radio buttons.
 * Allows selection of compartment 1 or 2.
 */
@Composable
fun CompartmentSelector(
    selectedCompartment: Int?,
    onCompartmentSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Select Compartment",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Compartment 1 Radio Button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedCompartment == 1,
                    onClick = { onCompartmentSelected(1) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Slot 1",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            // Compartment 2 Radio Button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedCompartment == 2,
                    onClick = { onCompartmentSelected(2) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Slot 2",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/**
 * Current schedule display card.
 */
@Composable
fun CurrentScheduleCard(
    schedule: com.teamA.pillbox.domain.MedicationSchedule,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                // Compartment badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Compartment ${schedule.compartmentNumber}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Days:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = schedule.daysOfWeek
                        .sortedBy { it.value }
                        .joinToString(", ") { it.name.take(3) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = String.format("%02d:%02d", schedule.time.hour, schedule.time.minute),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (schedule.medicationName != "Medication") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Name:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = schedule.medicationName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Display schedules grouped by compartment.
 * Shows all schedules in cards grouped by compartment number.
 */
@Composable
fun SchedulesGroupedByCompartment(
    schedules: List<com.teamA.pillbox.domain.MedicationSchedule>,
    onScheduleClick: ((com.teamA.pillbox.domain.MedicationSchedule) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Group schedules by compartment
        val schedulesByCompartment = schedules.groupBy { it.compartmentNumber }
        
        // Compartment 1
        if (schedulesByCompartment.containsKey(1)) {
            Text(
                text = "Compartment 1",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            schedulesByCompartment[1]?.forEach { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    onClick = onScheduleClick
                )
            }
        }
        
        // Compartment 2
        if (schedulesByCompartment.containsKey(2)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Compartment 2",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            schedulesByCompartment[2]?.forEach { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    onClick = onScheduleClick
                )
            }
        }
    }
}

/**
 * Individual schedule card for display in grouped list.
 */
@Composable
private fun ScheduleCard(
    schedule: com.teamA.pillbox.domain.MedicationSchedule,
    onClick: ((com.teamA.pillbox.domain.MedicationSchedule) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick(schedule) }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = schedule.medicationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Comp ${schedule.compartmentNumber}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Text(
                text = "Days: ${schedule.daysOfWeek.sortedBy { it.value }.joinToString(", ") { it.name.take(3) }}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Time: ${String.format("%02d:%02d", schedule.time.hour, schedule.time.minute)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

