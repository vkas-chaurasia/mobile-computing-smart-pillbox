package com.teamA.pillbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamA.pillbox.domain.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Medication status card showing today's schedule and status.
 */
@Composable
fun MedicationStatusCard(
    schedule: MedicationSchedule?,
    todayRecord: com.teamA.pillbox.domain.ConsumptionRecord?,
    onMarkAsTaken: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val todayDayOfWeek = today.dayOfWeek
    
    // Check if medication is scheduled for today
    val isScheduledToday = schedule?.let {
        it.isActive && it.daysOfWeek.contains(todayDayOfWeek)
    } ?: false

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Medication",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusBadge(
                    status = todayRecord?.status ?: if (isScheduledToday) ConsumptionStatus.PENDING else null
                )
            }

            if (!isScheduledToday) {
                // No schedule for today
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No medication scheduled for today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Schedule exists for today
                schedule?.let { sched ->
                    // Next dose time
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scheduled: ${String.format("%02d:%02d", sched.time.hour, sched.time.minute)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Countdown timer (only if not taken)
                    if (todayRecord?.status != ConsumptionStatus.TAKEN) {
                        CountdownTimer(
                            scheduledTime = sched.time,
                            isMissed = todayRecord?.status == ConsumptionStatus.MISSED
                        )
                    }

                    // Taken time (if taken)
                    todayRecord?.let { record ->
                        if (record.status == ConsumptionStatus.TAKEN && record.consumedTime != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Taken at: ${String.format("%02d:%02d", record.consumedTime!!.hour, record.consumedTime!!.minute)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Mark as Taken button
                    if (todayRecord?.status != ConsumptionStatus.TAKEN) {
                        Button(
                            onClick = onMarkAsTaken,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark as Taken")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Countdown timer showing time until next dose or time since missed.
 */
@Composable
fun CountdownTimer(
    scheduledTime: LocalTime,
    isMissed: Boolean
) {
    val currentTime = remember { mutableStateOf(LocalTime.now()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = LocalTime.now()
            kotlinx.coroutines.delay(1000) // Update every second
        }
    }

    val now = currentTime.value
    val scheduledDateTime = LocalDateTime.of(LocalDate.now(), scheduledTime)
    val currentDateTime = LocalDateTime.of(LocalDate.now(), now)

    val (timeText, color) = if (isMissed) {
        // Show how long ago it was missed
        val minutesAgo = ChronoUnit.MINUTES.between(scheduledDateTime, currentDateTime)
        val hoursAgo = minutesAgo / 60
        val minsAgo = minutesAgo % 60
        
        if (hoursAgo > 0) {
            "Missed ${hoursAgo}h ${minsAgo}m ago" to MaterialTheme.colorScheme.error
        } else {
            "Missed ${minutesAgo}m ago" to MaterialTheme.colorScheme.error
        }
    } else if (currentDateTime.isBefore(scheduledDateTime)) {
        // Countdown to scheduled time
        val minutesUntil = ChronoUnit.MINUTES.between(currentDateTime, scheduledDateTime)
        val hoursUntil = minutesUntil / 60
        val minsUntil = minutesUntil % 60
        
        if (hoursUntil > 0) {
            "In ${hoursUntil}h ${minsUntil}m" to MaterialTheme.colorScheme.secondary
        } else {
            "In ${minsUntil}m" to MaterialTheme.colorScheme.secondary
        }
    } else {
        // Past scheduled time but not marked as missed yet
        val minutesPast = ChronoUnit.MINUTES.between(scheduledDateTime, currentDateTime)
        val hoursPast = minutesPast / 60
        val minsPast = minutesPast % 60
        
        if (hoursPast > 0) {
            "${hoursPast}h ${minsPast}m past due" to MaterialTheme.colorScheme.error
        } else {
            "${minsPast}m past due" to MaterialTheme.colorScheme.error
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isMissed) Icons.Default.Warning else Icons.Default.AccessTime,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun StatusBadge(status: ConsumptionStatus?) {
    if (status == null) return
    
    val (text, color) = when (status) {
        ConsumptionStatus.TAKEN -> "Taken" to MaterialTheme.colorScheme.primary
        ConsumptionStatus.MISSED -> "Missed" to MaterialTheme.colorScheme.error
        ConsumptionStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.secondary
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Box state indicator showing if the pillbox lid is open or closed.
 */
@Composable
fun BoxStateIndicator(
    boxState: BoxState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (boxState == BoxState.OPEN) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (boxState == BoxState.OPEN) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Box State",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (boxState == BoxState.OPEN) "Open" else "Closed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (boxState == BoxState.OPEN) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Compartment card showing status for a specific compartment (1 or 2).
 */
@Composable
fun CompartmentCard(
    compartmentNumber: Int,
    compartmentState: CompartmentState,
    lightSensorValue: Int,
    schedule: MedicationSchedule?,
    todayRecord: ConsumptionRecord?,
    onMarkAsTaken: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    
    // Check if medication is scheduled for today for this compartment
    // Uses the new isActiveOn() method which checks start date and day of week
    val isScheduledToday = schedule?.isActiveOn(today) ?: false

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with compartment number and state
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compartment $compartmentNumber",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                CompartmentStateBadge(state = compartmentState)
            }

            // Compartment state indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (compartmentState) {
                        CompartmentState.LOADED -> Icons.Default.CheckCircle
                        CompartmentState.EMPTY -> Icons.Default.Warning
                        CompartmentState.UNKNOWN -> Icons.Default.Help
                    },
                    contentDescription = null,
                    tint = when (compartmentState) {
                        CompartmentState.LOADED -> MaterialTheme.colorScheme.primary
                        CompartmentState.EMPTY -> MaterialTheme.colorScheme.error
                        CompartmentState.UNKNOWN -> MaterialTheme.colorScheme.secondary
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "State: ${compartmentState.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Light sensor value
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Light Sensor: $lightSensorValue%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Medication status for this compartment
            if (!isScheduledToday) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No medication scheduled for today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                schedule?.let { sched ->
                    // Next dose time
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scheduled: ${String.format("%02d:%02d", sched.time.hour, sched.time.minute)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Status badge
                    StatusBadge(
                        status = todayRecord?.status ?: ConsumptionStatus.PENDING
                    )

                    // Countdown timer (only if not taken)
                    if (todayRecord?.status != ConsumptionStatus.TAKEN) {
                        CountdownTimer(
                            scheduledTime = sched.time,
                            isMissed = todayRecord?.status == ConsumptionStatus.MISSED
                        )
                    }

                    // Taken time (if taken)
                    todayRecord?.let { record ->
                        if (record.status == ConsumptionStatus.TAKEN && record.consumedTime != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Taken at: ${String.format("%02d:%02d", record.consumedTime!!.hour, record.consumedTime!!.minute)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Mark as Taken button
                    if (todayRecord?.status != ConsumptionStatus.TAKEN) {
                        Button(
                            onClick = onMarkAsTaken,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark as Taken")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Badge showing compartment state (LOADED/EMPTY/UNKNOWN).
 */
@Composable
private fun CompartmentStateBadge(state: CompartmentState) {
    val (text, color) = when (state) {
        CompartmentState.LOADED -> "Loaded" to MaterialTheme.colorScheme.primary
        CompartmentState.EMPTY -> "Empty" to MaterialTheme.colorScheme.error
        CompartmentState.UNKNOWN -> "Unknown" to MaterialTheme.colorScheme.secondary
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

