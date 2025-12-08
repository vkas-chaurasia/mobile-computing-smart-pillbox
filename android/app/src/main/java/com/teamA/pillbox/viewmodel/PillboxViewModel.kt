package com.teamA.pillbox.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamA.pillbox.BlePermissionHelper
import com.teamA.pillbox.ble.Pillbox
import com.teamA.pillbox.ble.PillboxScanner
import com.teamA.pillbox.repository.PillboxRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PillboxViewModel(
    application: Application,
    val repository: PillboxRepository,
    private val scanner: PillboxScanner
) : AndroidViewModel(application) {

    sealed class UiState {
        object Idle : UiState()
        object BluetoothDisabled : UiState()
        data class Scanning(val scannedDevices: List<ScanResult> = emptyList(), val isScanningActive: Boolean = false) : UiState()
        data class Connected(val deviceName: String?) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // --- These flows come directly from the repository ---
    val connectionState: StateFlow<Pillbox.State> = repository.state
    val batteryLevel: StateFlow<Int> = repository.batteryLevel
    val modelNumber: StateFlow<String> = repository.modelNumber
    val manufacturerName: StateFlow<String> = repository.manufacturerName

    val lightSensorValue: StateFlow<Int> = repository.lightLevel

    // ***FIXED***: Add the new lightSensorValue2 flow from the repository.
    val lightSensorValue2: StateFlow<Int> = repository.lightLevel2

    val tiltSensorValue: StateFlow<Int> = repository.tiltState

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("PillboxVM", "Coroutine failed: ${throwable.message}", throwable)
    }

    init {
        // Keep the scanner state in sync with the UI State
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

    // --- The rest of the ViewModel is unchanged ---

    fun startScan(permissionHelper: BlePermissionHelper) {
        if (!permissionHelper.hasRequiredPermissions()) {
            Log.w("PillboxVM", "startScan called without permissions.")
            return
        }
        if (!permissionHelper.isBluetoothEnabled()) {
            _uiState.value = UiState.BluetoothDisabled
            return
        }
        _uiState.value = UiState.Scanning(isScanningActive = true)
        scanner.startScan()
    }

    fun onDeviceSelected(device: BluetoothDevice, deviceName: String?) {
        scanner.stopScan()
        _uiState.value = UiState.Connected(deviceName)
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.connect(device)
        }
    }

    fun disconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.release()
        }
        _uiState.value = UiState.Idle
    }

    fun onPermissionsResult(hasPermissions: Boolean, permissionHelper: BlePermissionHelper) {
        if (hasPermissions) {
            startScan(permissionHelper)
        } else {
            _uiState.value = UiState.Idle
        }
    }

    fun checkBluetoothAndPermissions(permissionHelper: BlePermissionHelper) {
        if (!permissionHelper.isBluetoothEnabled()) {
            _uiState.value = UiState.BluetoothDisabled
            return
        }
        if (!permissionHelper.hasRequiredPermissions()) {
            _uiState.value = UiState.Idle
            return
        }
        if (_uiState.value !is UiState.Connected && _uiState.value !is UiState.Scanning) {
            _uiState.value = UiState.Idle
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
        repository.release()
    }

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
