package com.teamA.pillbox.domain

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Medication schedule configuration.
 * Represents a single medication schedule (one pill per day) for a specific compartment.
 */
data class MedicationSchedule(
    /**
     * Unique identifier for this schedule.
     */
    val id: String,
    
    /**
     * Compartment number (1 or 2) for which this schedule applies.
     */
    val compartmentNumber: Int,
    
    /**
     * Optional medication name.
     * Default: "Medication"
     */
    val medicationName: String = "Medication",
    
    /**
     * Days of the week when medication should be taken.
     * Example: [MONDAY, WEDNESDAY, FRIDAY]
     */
    val daysOfWeek: Set<DayOfWeek>,
    
    /**
     * Time of day when medication should be taken.
     * Example: 16:00 (4:00 PM)
     */
    val time: LocalTime,
    
    /**
     * Number of pills per dose.
     * Currently always 1 for MVP, but kept for future extensibility.
     */
    val pillCount: Int = 1,
    
    /**
     * Whether this schedule is currently active.
     */
    val isActive: Boolean = true,
    
    /**
     * Timestamp when this schedule was created.
     */
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(compartmentNumber in 1..2) { "Compartment number must be 1 or 2" }
        require(daysOfWeek.isNotEmpty()) { "At least one day must be selected" }
        require(pillCount > 0) { "Pill count must be positive" }
    }
}

