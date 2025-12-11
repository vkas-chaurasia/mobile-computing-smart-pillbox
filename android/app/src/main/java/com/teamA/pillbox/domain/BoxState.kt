package com.teamA.pillbox.domain

/**
 * State of the pillbox lid (open or closed).
 * Determined by the tilt sensor value.
 */
enum class BoxState {
    /**
     * Box lid is open (tilted).
     * Tilt sensor value >= threshold.
     */
    OPEN,
    
    /**
     * Box lid is closed (stable).
     * Tilt sensor value < threshold.
     */
    CLOSED
}
