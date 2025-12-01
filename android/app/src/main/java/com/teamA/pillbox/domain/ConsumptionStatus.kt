package com.teamA.pillbox.domain

/**
 * Status of medication consumption for a scheduled dose.
 */
enum class ConsumptionStatus {
    /**
     * Scheduled time hasn't arrived yet.
     */
    PENDING,
    
    /**
     * Medication was successfully consumed.
     */
    TAKEN,
    
    /**
     * Scheduled time passed (with grace period) without consumption.
     */
    MISSED
}

