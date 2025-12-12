package com.teamA.pillbox.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.teamA.pillbox.database.converters.Converters

/**
 * Room entity for consumption records.
 * Maps to domain model ConsumptionRecord.
 */
@Entity(tableName = "consumption_records")
data class ConsumptionRecordEntity(
    @PrimaryKey
    val id: String,
    
    /**
     * Compartment number (1 or 2) from which the medication was taken.
     */
    val compartmentNumber: Int,
    
    /**
     * Date as epoch days.
     * Converted using Converters.
     */
    val dateEpochDays: Long,
    
    /**
     * Scheduled time in milliseconds since midnight.
     * Converted using Converters.
     */
    val scheduledTimeMillis: Long,
    
    /**
     * Actual consumed time in epoch milliseconds (null if not consumed).
     * Converted using Converters.
     */
    val consumedTimeMillis: Long?,
    
    /**
     * Status as string (PENDING, TAKEN, MISSED).
     * Converted using Converters.
     */
    val statusString: String,
    
    /**
     * Detection method as string (SENSOR, MANUAL) or null.
     * Converted using Converters.
     */
    val detectionMethodString: String?
)
