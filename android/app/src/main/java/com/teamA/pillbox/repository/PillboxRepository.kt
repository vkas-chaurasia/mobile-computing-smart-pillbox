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

/**
 * Repository that manages the lifecycle of the PillboxManager.
 * It creates and holds the manager instance for the connected device.
 */
class PillboxRepository(private val context: Context) {

    private var manager: PillboxManager? = null
    // A dedicated scope for the repository to manage observation jobs
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Exposed Flows - these will be updated when a manager is created.
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

    /**
     * Connects to a specific Bluetooth device by creating a new PillboxManager instance
     * and then initiating the connection.
     */
    fun connect(device: BluetoothDevice) {
        // Release any existing manager before creating a new one
        manager?.release()

        // *** FIX STARTS HERE ***

        // 1. Create a new manager for the selected device with only the context.
        val newManager = PillboxManager(context)
        manager = newManager // Assign it to the class property.

        // 2. Launch a coroutine in the repository's scope to call the suspend connect function.
        scope.launch {
            newManager.connectToPillbox(device)
        }

        // 3. Observe the flows from the new manager instance.
        newManager.state.observe(_state)
        newManager.sensorData.observe(_sensorData)
        newManager.batteryLevel.observe(_batteryLevel)
        newManager.modelNumber.observe(_modelNumber)
        newManager.manufacturerName.observe(_manufacturerName)

        // *** FIX ENDS HERE ***
    }

    /**
     * Releases the connection and all resources held by the PillboxManager.
     */
    fun release() {
        manager?.release()
        manager = null
        // Reset states
        _state.value = Pillbox.State.NOT_AVAILABLE
        _sensorData.value = "light:N/A;tilt:N/A"
        _batteryLevel.value = 0
        _modelNumber.value = "N/A"
        _manufacturerName.value = "N/A"
    }

    /**
     * Reads device information from the connected Pillbox.
     */
    suspend fun readDeviceInfo() {
        manager?.readDeviceInfo()
    }

    /**
     * Performs a one-time read of the battery level from the connected Pillbox.
     */
    suspend fun readBatteryLevel() {
        manager?.readBattery()
    }

    /**
     * Sends a command to the connected Pillbox.
     */
    suspend fun sendCommand(command: String) {
        manager?.sendCommand(command)
    }

    // Helper extension to observe a StateFlow and update a MutableStateFlow
    private fun <T> StateFlow<T>.observe(mutableStateFlow: MutableStateFlow<T>) {
        scope.launch {
            this@observe.collect { value ->
                mutableStateFlow.value = value
            }
        }
    }
}
