package com.teamA.pillbox.ble

import kotlinx.coroutines.flow.StateFlow

interface Pillbox {

    enum class State {
        LOADING,
        READY,
        NOT_AVAILABLE
    }

    val state: StateFlow<State>

    val sensorData: StateFlow<String>

    val batteryLevel: StateFlow<Int>

    val modelNumber: StateFlow<String>

    val manufacturerName: StateFlow<String>

    fun release()

    suspend fun readDeviceInfo(): Boolean

    suspend fun readBattery()

    suspend fun sendCommand(command: String)
}
