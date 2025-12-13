package com.teamA.pillbox.sensor

import com.teamA.pillbox.domain.BoxState
import com.teamA.pillbox.domain.SensorEvent
import com.teamA.pillbox.domain.SensorThresholds
import java.time.LocalDateTime

/**
 * Logic for detecting pill removal based on sensor data.
 * Implements per-compartment detection algorithm.
 * 
 * Detection rules:
 * - Box must be opened (tilt >= tiltThreshold)
 * - Light sensor must exceed threshold for the compartment (light > lightThreshold)
 * - Both conditions must be met simultaneously for detection
 */
class PillDetectionLogic {

    // Previous sensor values for state change detection
    private var previousTilt: Int = 0
    private var previousLight1: Int = 0
    private var previousLight2: Int = 0

    /**
     * Detect pill removal for a specific compartment.
     * 
     * @param compartmentNumber Compartment number (1 or 2)
     * @param lightValue Current light sensor value for the compartment
     * @param tiltValue Current tilt sensor value
     * @param thresholds Sensor thresholds to use
     * @return SensorEvent if detection occurred, null otherwise
     */
    fun detectPillRemoval(
        compartmentNumber: Int,
        lightValue: Int,
        tiltValue: Int,
        thresholds: SensorThresholds
    ): SensorEvent? {
        require(compartmentNumber in 1..2) { "Compartment number must be 1 or 2" }

        val lightThreshold = thresholds.getLightThreshold(compartmentNumber)
        val tiltThreshold = thresholds.tiltThreshold

        // Determine box state
        val boxState = if (tiltValue >= tiltThreshold) BoxState.OPEN else BoxState.CLOSED

        // Check if box just opened (transition from closed to open)
        val boxJustOpened = tiltValue >= tiltThreshold && previousTilt < tiltThreshold

        // Get previous light value for this compartment
        val previousLight = when (compartmentNumber) {
            1 -> previousLight1
            2 -> previousLight2
            else -> 0
        }

        // Update previous values
        previousTilt = tiltValue
        when (compartmentNumber) {
            1 -> previousLight1 = lightValue
            2 -> previousLight2 = lightValue
        }

        // Detection logic: Box opened AND light exceeds threshold
        val detected = boxJustOpened && lightValue > lightThreshold

        // Only return event if box just opened (to avoid duplicate detections)
        return if (boxJustOpened) {
            SensorEvent(
                compartmentNumber = compartmentNumber,
                detected = detected,
                boxState = boxState,
                timestamp = LocalDateTime.now(),
                lightValue = lightValue,
                tiltValue = tiltValue,
                lightThreshold = lightThreshold,
                tiltThreshold = tiltThreshold
            )
        } else {
            null
        }
    }

    /**
     * Detect pill removal for both compartments simultaneously.
     * 
     * @param lightValue1 Current light sensor value for compartment 1
     * @param lightValue2 Current light sensor value for compartment 2
     * @param tiltValue Current tilt sensor value
     * @param thresholds Sensor thresholds to use
     * @return List of SensorEvents (can be 0, 1, or 2 events)
     */
    fun detectPillRemovalForBothCompartments(
        lightValue1: Int,
        lightValue2: Int,
        tiltValue: Int,
        thresholds: SensorThresholds
    ): List<SensorEvent> {
        val events = mutableListOf<SensorEvent>()

        // Check if box just opened (only check once)
        val boxJustOpened = tiltValue >= thresholds.tiltThreshold && previousTilt < thresholds.tiltThreshold

        if (boxJustOpened) {
            val lightThreshold1 = thresholds.getLightThreshold(1)
            val lightThreshold2 = thresholds.getLightThreshold(2)
            val tiltThreshold = thresholds.tiltThreshold
            val boxState = BoxState.OPEN
            val timestamp = LocalDateTime.now()

            // Detect for compartment 1
            val detected1 = lightValue1 > lightThreshold1
            if (detected1) {
                events.add(
                    SensorEvent(
                        compartmentNumber = 1,
                        detected = true,
                        boxState = boxState,
                        timestamp = timestamp,
                        lightValue = lightValue1,
                        tiltValue = tiltValue,
                        lightThreshold = lightThreshold1,
                        tiltThreshold = tiltThreshold
                    )
                )
            }

            // Detect for compartment 2
            val detected2 = lightValue2 > lightThreshold2
            if (detected2) {
                events.add(
                    SensorEvent(
                        compartmentNumber = 2,
                        detected = true,
                        boxState = boxState,
                        timestamp = timestamp,
                        lightValue = lightValue2,
                        tiltValue = tiltValue,
                        lightThreshold = lightThreshold2,
                        tiltThreshold = tiltThreshold
                    )
                )
            }

            // Update previous values after checking both compartments
            previousTilt = tiltValue
            previousLight1 = lightValue1
            previousLight2 = lightValue2
        } else {
            // Update previous values even if no detection
            previousTilt = tiltValue
            previousLight1 = lightValue1
            previousLight2 = lightValue2
        }

        return events
    }

    /**
     * Get current box state based on tilt sensor.
     * 
     * @param tiltValue Current tilt sensor value
     * @param tiltThreshold Tilt threshold
     * @return Current box state
     */
    fun getBoxState(tiltValue: Int, tiltThreshold: Int): BoxState {
        return if (tiltValue >= tiltThreshold) BoxState.OPEN else BoxState.CLOSED
    }

    /**
     * Determine compartment state based on light sensor value.
     * 
     * @param lightValue Current light sensor value (0-100)
     * @param lightThreshold Light threshold setting
     * @return CompartmentState (LOADED if light < threshold, EMPTY if light >= threshold)
     */
    fun determineCompartmentState(
        lightValue: Int,
        lightThreshold: Int
    ): com.teamA.pillbox.domain.CompartmentState {
        return if (lightValue >= lightThreshold) {
            // High light value = no pill blocking light = EMPTY
            com.teamA.pillbox.domain.CompartmentState.EMPTY
        } else {
            // Low light value = pill blocking light = LOADED
            com.teamA.pillbox.domain.CompartmentState.LOADED
        }
    }

    /**
     * Reset previous sensor values (useful for testing or reconnection).
     */
    fun reset() {
        previousTilt = 0
        previousLight1 = 0
        previousLight2 = 0
    }
}
