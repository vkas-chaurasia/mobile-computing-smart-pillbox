package com.teamA.pillbox.ble

import kotlinx.coroutines.flow.StateFlow

interface Pillbox {
    val lightLevel: StateFlow<Int>
    val tiltState: StateFlow<Int>

    val state: StateFlow<State>
    val batteryLevel: StateFlow<Int>
    val modelNumber: StateFlow<String>
    val manufacturerName: StateFlow<String>


    fun release()

    suspend fun readDeviceInfo(): Boolean
    suspend fun readBattery()

    enum class State {
        NOT_AVAILABLE,
        LOADING,
        READY
    }
}
