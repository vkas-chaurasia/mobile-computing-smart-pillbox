package com.teamA.pillbox.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Record of medication consumption for a specific scheduled dose.
 */
data class ConsumptionRecord(
    /**
     * Unique identifier for this record.
     */
    val id: String,
    
    /**
     * Date for which this consumption record applies.
     */
    val date: LocalDate,
    
    /**
     * Scheduled time when medication should have been taken.
     */
    val scheduledTime: LocalTime,
    
    /**
     * Actual time when medication was consumed.
     * null if status is PENDING or MISSED.
     */
    val consumedTime: LocalDateTime?,
    
    /**
     * Current status of this consumption record.
     */
    val status: ConsumptionStatus,
    
    /**
     * Method used to detect consumption (if status is TAKEN).
     * null if status is PENDING or MISSED.
     */
    val detectionMethod: DetectionMethod?
) {
    init {
        // Validation: If status is TAKEN, consumedTime and detectionMethod should be set
        if (status == ConsumptionStatus.TAKEN) {
            require(consumedTime != null) { "Consumed time must be set when status is TAKEN" }
            require(detectionMethod != null) { "Detection method must be set when status is TAKEN" }
        }
        
        // Validation: If status is PENDING or MISSED, consumedTime should be null
        if (status == ConsumptionStatus.PENDING || status == ConsumptionStatus.MISSED) {
            require(consumedTime == null) { "Consumed time must be null when status is PENDING or MISSED" }
        }
    }
}

