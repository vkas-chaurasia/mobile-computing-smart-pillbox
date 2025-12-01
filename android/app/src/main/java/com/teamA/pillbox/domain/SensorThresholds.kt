package com.teamA.pillbox.domain

/**
 * Configurable thresholds for sensor-based pill detection.
 * 
 * TODO: Adjust thresholds after connecting with real device
 */
data class SensorThresholds(
    /**
     * Light sensor threshold percentage (0-100).
     * Pill detection requires light value to exceed this threshold.
     * Default: 40%
     */
    val lightThreshold: Int = 40, // TODO: Adjust after connecting with real device
    
    /**
     * Tilt sensor threshold.
     * Pill detection requires tilt value to equal or exceed this threshold.
     * Default: 1 (box opened)
     */
    val tiltThreshold: Int = 1    // TODO: Adjust after connecting with real device
) {
    init {
        require(lightThreshold in 0..100) { "Light threshold must be between 0 and 100" }
        require(tiltThreshold >= 0) { "Tilt threshold must be non-negative" }
    }
}

