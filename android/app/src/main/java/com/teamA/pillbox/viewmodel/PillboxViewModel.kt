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
import com.teamA.pillbox.domain.BoxState
import com.teamA.pillbox.domain.ConsumptionRecord
import com.teamA.pillbox.domain.ConsumptionStatus
import com.teamA.pillbox.domain.DetectionMethod
import com.teamA.pillbox.domain.MedicationSchedule
import com.teamA.pillbox.domain.SensorEvent
import com.teamA.pillbox.repository.HistoryRepository
import com.teamA.pillbox.repository.PillboxRepository
import com.teamA.pillbox.repository.ScheduleRepository
import com.teamA.pillbox.repository.SettingsRepository
import com.teamA.pillbox.sensor.PillDetectionLogic
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class PillboxViewModel(
    application: Application,
    val repository: PillboxRepository,
    private val scanner: PillboxScanner,
    private val scheduleRepository: ScheduleRepository = ScheduleRepository(application),
    private val historyRepository: HistoryRepository = HistoryRepository(application),
    private val settingsRepository: SettingsRepository = SettingsRepository(application)
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

    // Box state (derived from tilt sensor)
    val boxState: StateFlow<BoxState> = combine(
        tiltSensorValue,
        settingsRepository.sensorThresholds
    ) { tilt, thresholds ->
        PillDetectionLogic().getBoxState(tilt, thresholds.tiltThreshold)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BoxState.CLOSED
    )

    // Pill detection logic instance
    private val pillDetectionLogic = PillDetectionLogic()

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

        // Monitor sensor data and detect pill removal
        startPillDetection()
    }

    /**
     * Start monitoring sensor data for pill detection.
     * Observes sensor values and creates consumption records when pills are detected.
     */
    private fun startPillDetection() {
        viewModelScope.launch {
            combine(
                lightSensorValue,
                lightSensorValue2,
                tiltSensorValue,
                settingsRepository.sensorThresholds,
                scheduleRepository.getAllSchedules()
            ) { light1, light2, tilt, thresholds, schedules ->
                // Detect pill removal for both compartments
                val events = pillDetectionLogic.detectPillRemovalForBothCompartments(
                    lightValue1 = light1,
                    lightValue2 = light2,
                    tiltValue = tilt,
                    thresholds = thresholds
                )

                // Process each detection event
                events.forEach { event ->
                    if (event.detected) {
                        handlePillDetection(event, schedules)
                    }
                }

                // Return unit (we're just using this for side effects)
                Unit
            }.collect()
        }
    }

    /**
     * Handle a pill detection event by creating a consumption record.
     * 
     * @param event Sensor event indicating pill detection
     * @param schedules All active schedules
     */
    private suspend fun handlePillDetection(
        event: SensorEvent,
        schedules: List<MedicationSchedule>
    ) {
        try {
            val today = LocalDate.now()
            val currentTime = LocalTime.now()

            // Find active schedule for this compartment
            val schedule = schedules.firstOrNull { schedule ->
                schedule.compartmentNumber == event.compartmentNumber &&
                schedule.isActive &&
                schedule.daysOfWeek.contains(today.dayOfWeek)
            }

            if (schedule != null) {
                // Check if a record already exists for today
                val existingRecord = historyRepository.getTodayRecord(event.compartmentNumber).first()

                if (existingRecord == null) {
                    // Create new consumption record
                    val record = ConsumptionRecord(
                        id = UUID.randomUUID().toString(),
                        compartmentNumber = event.compartmentNumber,
                        date = today,
                        scheduledTime = schedule.time,
                        consumedTime = LocalDateTime.now(),
                        status = ConsumptionStatus.TAKEN,
                        detectionMethod = DetectionMethod.SENSOR
                    )

                    historyRepository.createRecord(record)
                    Log.d("PillboxVM", "Created consumption record for compartment ${event.compartmentNumber}: $record")
                } else if (existingRecord.status != ConsumptionStatus.TAKEN) {
                    // Update existing record to TAKEN
                    val updatedRecord = existingRecord.copy(
                        consumedTime = LocalDateTime.now(),
                        status = ConsumptionStatus.TAKEN,
                        detectionMethod = DetectionMethod.SENSOR
                    )
                    historyRepository.updateRecord(updatedRecord)
                    Log.d("PillboxVM", "Updated consumption record for compartment ${event.compartmentNumber}: $updatedRecord")
                }
            } else {
                Log.d("PillboxVM", "Pill detected for compartment ${event.compartmentNumber} but no active schedule found")
            }
        } catch (e: Exception) {
            Log.e("PillboxVM", "Error handling pill detection", e)
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
        pillDetectionLogic.reset()
    }

    class Factory(
        private val application: Application,
        private val repository: PillboxRepository,
        private val scanner: PillboxScanner,
        private val scheduleRepository: ScheduleRepository? = null,
        private val historyRepository: HistoryRepository? = null,
        private val settingsRepository: SettingsRepository? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PillboxViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PillboxViewModel(
                    application,
                    repository,
                    scanner,
                    scheduleRepository ?: ScheduleRepository(application),
                    historyRepository ?: HistoryRepository(application),
                    settingsRepository ?: SettingsRepository(application)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
