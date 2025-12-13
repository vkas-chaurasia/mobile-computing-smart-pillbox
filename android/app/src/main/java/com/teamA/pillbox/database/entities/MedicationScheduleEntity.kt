package com.teamA.pillbox.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.teamA.pillbox.database.converters.Converters

/**
 * Room entity for medication schedules.
 * Maps to domain model MedicationSchedule.
 */
@Entity(tableName = "medication_schedules")
data class MedicationScheduleEntity(
    @PrimaryKey
    val id: String,
    
    /**
     * Compartment number (1 or 2) for which this schedule applies.
     */
    val compartmentNumber: Int,
    
    /**
     * Medication name.
     */
    val medicationName: String,
    
    /**
     * Start date as epoch day (days since 1970-01-01).
     * Schedule only applies from this date onwards.
     */
    val startDateEpochDay: Long,
    
    /**
     * Days of week as comma-separated string (e.g., "MONDAY,WEDNESDAY,FRIDAY").
     * Converted using Converters.
     */
    val daysOfWeekString: String,
    
    /**
     * Time in milliseconds since midnight.
     * Converted using Converters.
     */
    val timeMillis: Long,
    
    /**
     * Number of pills per dose.
     */
    val pillCount: Int,
    
    /**
     * Whether this schedule is currently active.
     */
    val isActive: Boolean,
    
    /**
     * Timestamp when this schedule was created.
     */
    val createdAt: Long
)
