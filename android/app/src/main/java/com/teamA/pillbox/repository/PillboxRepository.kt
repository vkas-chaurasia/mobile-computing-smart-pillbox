package com.teamA.pillbox.repository

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.teamA.pillbox.ble.Pillbox
import com.teamA.pillbox.ble.PillboxManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class PillboxRepository(private val context: Context) {

    private var manager: PillboxManager? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(Pillbox.State.NOT_AVAILABLE)
    val state: StateFlow<Pillbox.State> = _state

    private val _sensorData = MutableStateFlow("light:N/A;tilt:N/A")
    val sensorData: StateFlow<String> = _sensorData

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel

    private val _modelNumber = MutableStateFlow("N/A")
    val modelNumber: StateFlow<String> = _modelNumber

    private val _manufacturerName = MutableStateFlow("N/A")
    val manufacturerName: StateFlow<String> = _manufacturerName

    fun connect(device: BluetoothDevice) {
        manager?.release()

        val newManager = PillboxManager(context)
        manager = newManager

        scope.launch {
            newManager.connectToPillbox(device)
        }

        newManager.state.observe(_state)
        newManager.sensorData.observe(_sensorData)
        newManager.batteryLevel.observe(_batteryLevel)
        newManager.modelNumber.observe(_modelNumber)
        newManager.manufacturerName.observe(_manufacturerName)

    }

    fun release() {
        manager?.release()
        manager = null
        _state.value = Pillbox.State.NOT_AVAILABLE
        _sensorData.value = "light:N/A;tilt:N/A"
        _batteryLevel.value = 0
        _modelNumber.value = "N/A"
        _manufacturerName.value = "N/A"
    }

    suspend fun readDeviceInfo() {
        manager?.readDeviceInfo()
    }

    suspend fun readBatteryLevel() {
        manager?.readBattery()
    }

    suspend fun sendCommand(command: String) {
        manager?.sendCommand(command)
    }

    private fun <T> StateFlow<T>.observe(mutableStateFlow: MutableStateFlow<T>) {
        scope.launch {
            this@observe.collect { value ->
                mutableStateFlow.value = value
            }
        }
    }
}
