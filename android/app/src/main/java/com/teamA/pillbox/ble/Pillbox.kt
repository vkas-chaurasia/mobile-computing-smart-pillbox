package com.teamA.pillbox.ble

import kotlinx.coroutines.flow.StateFlow

/**
 * Contract interface for a connected Pillbox BLE device.
 * This interface defines the properties and actions available once a connection
 * to a Pillbox device is established.
 */
interface Pillbox {

    /**
     * Represents the connection and initialization state of the Pillbox device.
     */
    enum class State {
        LOADING, // Services are being discovered and characteristics initialized.
        READY,   // The device is connected and ready for interaction.
        NOT_AVAILABLE // The device is disconnected or the connection was lost.
    }

    /**
     * The current state of the connection to the Pillbox device.
     */
    val state: StateFlow<State>

    /**
     * A flow of sensor data received from the Pillbox.
     * The string format can be defined by the device's protocol.
     */
    val sensorData: StateFlow<String>

    /**
     * The current battery level of the Pillbox, as a percentage (0-100).
     */
    val batteryLevel: StateFlow<Int>

    /**
     * The model number string read from the Device Information Service.
     */
    val modelNumber: StateFlow<String>

    /**
     * The manufacturer name string read from the Device Information Service.
     */
    val manufacturerName: StateFlow<String>

    /**
     * Releases all resources associated with the current Pillbox connection.
     * This should be called to disconnect and clean up.
     */
    fun release()

    /**
     * Reads static device information like model number and manufacturer name.
     * This is typically called once after connection.
     *
     * @return `true` if the information was read successfully, `false` otherwise.
     */
    suspend fun readDeviceInfo(): Boolean

    /**
     * Performs a one-time read of the battery level from the device.
     * The result is communicated via the `batteryLevel` StateFlow.
     */
    suspend fun readBattery()

    /**
     * Sends a command string to the Pillbox device.
     *
     * @param command The command to be sent to the device's control characteristic.
     */
    suspend fun sendCommand(command: String)
}
