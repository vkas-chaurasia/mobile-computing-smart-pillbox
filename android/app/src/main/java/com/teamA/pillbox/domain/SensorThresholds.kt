package com.teamA.pillbox.domain

/**
 * Configurable thresholds for sensor-based pill detection.
 * Supports per-compartment light sensor thresholds.
 * 
 * TODO: Adjust thresholds after connecting with real device
 */
data class SensorThresholds(
    /**
     * Light sensor threshold percentage (0-100) for compartment 1.
     * Pill detection requires light value to exceed this threshold.
     * Default: 40%
     */
    val lightThreshold1: Int = 40, // TODO: Adjust after connecting with real device
    
    /**
     * Light sensor threshold percentage (0-100) for compartment 2.
     * Pill detection requires light value to exceed this threshold.
     * Default: 40%
     */
    val lightThreshold2: Int = 40, // TODO: Adjust after connecting with real device
    
    /**
     * Tilt sensor threshold (shared for both compartments).
     * Pill detection requires tilt value to equal or exceed this threshold.
     * Default: 1 (box opened)
     */
    val tiltThreshold: Int = 1    // TODO: Adjust after connecting with real device
) {
    init {
        require(lightThreshold1 in 0..100) { "Light threshold 1 must be between 0 and 100" }
        require(lightThreshold2 in 0..100) { "Light threshold 2 must be between 0 and 100" }
        require(tiltThreshold >= 0) { "Tilt threshold must be non-negative" }
    }
    
    /**
     * Get light threshold for a specific compartment.
     * @param compartmentNumber Compartment number (1 or 2)
     * @return Light threshold for the specified compartment
     */
    fun getLightThreshold(compartmentNumber: Int): Int {
        return when (compartmentNumber) {
            1 -> lightThreshold1
            2 -> lightThreshold2
            else -> throw IllegalArgumentException("Compartment number must be 1 or 2")
        }
    }
}

