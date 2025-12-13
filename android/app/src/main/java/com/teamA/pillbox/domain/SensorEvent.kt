package com.teamA.pillbox.domain

import java.time.LocalDateTime

/**
 * Represents a sensor detection event.
 * Used to communicate detection results from PillDetectionLogic.
 */
data class SensorEvent(
    /**
     * Compartment number (1 or 2) for which this detection applies.
     */
    val compartmentNumber: Int,
    
    /**
     * Whether a pill was detected as taken.
     * true = pill detected (tilt open AND light > threshold)
     * false = box opened but pill not taken (tilt open BUT light <= threshold)
     */
    val detected: Boolean,
    
    /**
     * Current state of the pillbox lid.
     */
    val boxState: BoxState,
    
    /**
     * Timestamp when this event occurred.
     */
    val timestamp: LocalDateTime,
    
    /**
     * Light sensor value at the time of detection.
     */
    val lightValue: Int,
    
    /**
     * Tilt sensor value at the time of detection.
     */
    val tiltValue: Int,
    
    /**
     * Light threshold that was used for this detection.
     */
    val lightThreshold: Int,
    
    /**
     * Tilt threshold that was used for this detection.
     */
    val tiltThreshold: Int
) {
    init {
        require(compartmentNumber in 1..2) { "Compartment number must be 1 or 2" }
    }
}
