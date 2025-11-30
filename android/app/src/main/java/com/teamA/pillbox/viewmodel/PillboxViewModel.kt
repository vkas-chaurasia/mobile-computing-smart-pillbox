package com.teamA.pillbox.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
// Import the BlePermissionHelper from the correct package
import com.teamA.pillbox.BlePermissionHelper
import com.teamA.pillbox.ble.Pillbox
import com.teamA.pillbox.ble.PillboxScanner
import com.teamA.pillbox.repository.PillboxRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * A unified ViewModel that manages the entire BLE lifecycle including scanning,
 * connection, and device interaction. It serves as the single source of truth for the UI.
 */
class PillboxViewModel(
    application: Application,
    // The repository is now public to allow the ControlScreen to collect its flows directly.
    val repository: PillboxRepository,
    private val scanner: PillboxScanner
) : AndroidViewModel(application) {

    // --- UI State Management ---

    /**
     * Represents the different states of the UI, from scanning to connected.
     * The UI should use this as the primary determinant of what to display.
     */
    sealed class UiState {
        object Idle : UiState()
        // RequestingPermissions state is removed, as this is now handled by the UI layer directly.
        object BluetoothDisabled : UiState()
        data class Scanning(val scannedDevices: List<ScanResult> = emptyList(), val isScanningActive: Boolean = false) : UiState()
        data class Connected(val deviceName: String?) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // --- Device Interaction Properties ---
    val connectionState: StateFlow<Pillbox.State> = repository.state
    val sensorData: StateFlow<String> = repository.sensorData // This is the raw string from the repo
    val batteryLevel: StateFlow<Int> = repository.batteryLevel
    val modelNumber: StateFlow<String> = repository.modelNumber
    val manufacturerName: StateFlow<String> = repository.manufacturerName

    // --- NEW: Parsed Sensor Values for the UI ---
    private val _lightSensorValue = MutableStateFlow(0)
    val lightSensorValue: StateFlow<Int> = _lightSensorValue.asStateFlow()

    private val _tiltSensorValue = MutableStateFlow(0)
    val tiltSensorValue: StateFlow<Int> = _tiltSensorValue.asStateFlow()
    // --- END OF NEW PROPERTIES ---

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("PillboxVM", "Coroutine failed: ${throwable.message}", throwable)
    }

    init {
        // --- NEW: This block observes the raw sensorData string and calls our parsing function ---
        repository.sensorData
            .onEach { data -> parseSensorData(data) }
            .launchIn(viewModelScope)
        // --- END OF NEW BLOCK ---

        // When the scanner updates its list, we update our UI state if we are in the scanning state.
        viewModelScope.launch {
            scanner.scannedDevices.collect { devices ->
                if (_uiState.value is UiState.Scanning) {
                    _uiState.value = UiState.Scanning(devices, scanner.isScanning.value)
                }
            }
        }
        viewModelScope.launch {
            scanner.isScanning.collect { isScanningActive ->
                if (_uiState.value is UiState.Scanning) {
                    val currentDevices = (_uiState.value as UiState.Scanning).scannedDevices
                    _uiState.value = UiState.Scanning(currentDevices, isScanningActive)
                }
            }
        }
    }

    // --- NEW: This function parses the "light:XX;tilt:Y" string ---
    /**
     * Parses the incoming sensor data string (e.g., "light:55;tilt:1")
     * and updates the dedicated state flows for each sensor value.
     */
    private fun parseSensorData(data: String) {
        if (data.isBlank()) return // Ignore empty strings

        try {
            data.split(';').forEach { part ->
                val pair = part.split(':')
                if (pair.size == 2) {
                    val key = pair[0].trim()
                    val value = pair[1].trim().toIntOrNull()
                    if (value != null) {
                        when (key) {
                            "light" -> _lightSensorValue.value = value
                            "tilt" -> _tiltSensorValue.value = value
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PillboxVM", "Failed to parse sensor data string: '$data'", e)
        }
    }
    // --- END OF NEW FUNCTION ---


    // --- Actions from the UI ---

    /**
     * Attempts to start a BLE scan.
     * The UI is responsible for checking permissions first and calling this method only when they are granted.
     */
    fun startScan(permissionHelper: BlePermissionHelper) {
        // The UI should have already checked for permissions. This is a fallback check.
        if (!permissionHelper.hasRequiredPermissions()) {
            // The UI will handle requesting permissions. ViewModel does nothing here.
            Log.w("PillboxVM", "startScan called without required permissions.")
            return
        }

        if (!permissionHelper.isBluetoothEnabled()) {
            _uiState.value = UiState.BluetoothDisabled
            return
        }

        // Set the state to Scanning and trigger the scanner.
        _uiState.value = UiState.Scanning(isScanningActive = true)
        scanner.startScan()
    }

    fun onDeviceSelected(device: BluetoothDevice, deviceName: String?) {
        scanner.stopScan()
        // Set the UI state to Connected immediately for a responsive UI.
        _uiState.value = UiState.Connected(deviceName)
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.connect(device)
            // Once connected, read initial static info
            repository.readDeviceInfo()
            repository.readBatteryLevel()
        }
    }

    /**
     * Called by the UI after the user responds to the permission dialog.
     */
    fun onPermissionsResult(hasPermissions: Boolean, permissionHelper: BlePermissionHelper) {
        if (hasPermissions) {
            // Permissions were granted, now we can proceed with the scan logic.
            startScan(permissionHelper)
        } else {
            // If permissions are denied, reset the state to Idle.
            // The UI can show a rationale if needed.
            _uiState.value = UiState.Idle
        }
    }

    /**
     * Checks the current Bluetooth and permission status and updates the UI state.
     * This is useful to call when the app resumes (e.g., from the ON_RESUME lifecycle event).
     */
    fun checkBluetoothAndPermissions(permissionHelper: BlePermissionHelper) {
        if (!permissionHelper.isBluetoothEnabled()) {
            _uiState.value = UiState.BluetoothDisabled
        } else if (!permissionHelper.hasRequiredPermissions()) {
            // If permissions are missing, revert to the Idle state. The UI will show
            // the "Scan" button, which triggers the permission request when clicked.
            _uiState.value = UiState.Idle
        } else {
            // If bluetooth and permissions are ok, and we aren't already connected or scanning,
            // ensure the state is Idle so the user can start a scan.
            if (_uiState.value !is UiState.Connected && _uiState.value !is UiState.Scanning) {
                _uiState.value = UiState.Idle
            }
        }
    }

    // --- Device Commands ---

    fun sendTestCommand() {
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.sendCommand("led:toggle") // Example command
        }
    }

    fun readDeviceInfo() {
        viewModelScope.launch(Dispatchers.IO) { repository.readDeviceInfo() }
    }

    fun readBatteryLevel() {
        viewModelScope.launch(Dispatchers.IO) { repository.readBatteryLevel() }
    }

    override fun onCleared() {
        super.onCleared()
        scanner.stopScan()
        repository.release() // Crucial: Release BLE resources
    }

    // --- ViewModel Factory ---

    class Factory(
        private val application: Application,
        private val repository: PillboxRepository,
        private val scanner: PillboxScanner
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PillboxViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PillboxViewModel(application, repository, scanner) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
