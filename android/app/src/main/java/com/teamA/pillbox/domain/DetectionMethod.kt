package com.teamA.pillbox.domain

/**
 * Method used to detect medication consumption.
 */
enum class DetectionMethod {
    /**
     * Detected automatically via tilt and light sensors.
     */
    SENSOR,
    
    /**
     * Manually marked as taken by the user.
     */
    MANUAL
}

