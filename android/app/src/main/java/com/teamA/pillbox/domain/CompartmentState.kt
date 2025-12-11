package com.teamA.pillbox.domain

/**
 * State of a pillbox compartment.
 * Used to track whether a compartment is loaded with pills or empty.
 */
enum class CompartmentState {
    /**
     * Compartment is loaded with pills.
     * Light sensor value is below threshold (pill blocking light).
     */
    LOADED,
    
    /**
     * Compartment is empty (no pills).
     * Light sensor value is above threshold (no pill blocking light).
     */
    EMPTY,
    
    /**
     * Compartment state is unknown.
     * Used when sensor data is unavailable or not yet calibrated.
     */
    UNKNOWN
}
