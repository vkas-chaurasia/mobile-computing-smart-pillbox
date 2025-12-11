package com.teamA.pillbox.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamA.pillbox.domain.MedicationSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

/**
 * ViewModel for the Schedule Setup screen.
 * 
 * Currently uses in-memory storage.
 */
class ScheduleViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val TAG = "ScheduleViewModel"

    // In-memory storage (will be replaced with repository)
    // Placeholder: Store multiple schedules (will be replaced with repository)
    private val _allSchedules = mutableListOf<MedicationSchedule>()
    private var _currentSchedule: MedicationSchedule? = null

    private val _uiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Empty)
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    // Form fields state
    private val _selectedCompartment = MutableStateFlow<Int?>(null)
    val selectedCompartment: StateFlow<Int?> = _selectedCompartment.asStateFlow()

    private val _selectedDays = MutableStateFlow<Set<DayOfWeek>>(emptySet())
    val selectedDays: StateFlow<Set<DayOfWeek>> = _selectedDays.asStateFlow()

    private val _selectedTime = MutableStateFlow<LocalTime?>(null)
    val selectedTime: StateFlow<LocalTime?> = _selectedTime.asStateFlow()

    private val _medicationName = MutableStateFlow<String>("Medication")
    val medicationName: StateFlow<String> = _medicationName.asStateFlow()

    // Validation state
    private val _isValid = MutableStateFlow(false)
    val isValid: StateFlow<Boolean> = _isValid.asStateFlow()

    init {
        loadSchedule()
    }

    /**
     * Load existing schedules from in-memory storage.
     * Later, this will load from ScheduleRepository.
     */
    fun loadSchedule() {
        // Placeholder: Load all schedules (will be replaced with repository)
        if (_allSchedules.isNotEmpty()) {
            _uiState.value = ScheduleUiState.Loaded(_allSchedules.first())
        } else {
            _currentSchedule?.let { schedule ->
                _selectedCompartment.value = schedule.compartmentNumber
                _selectedDays.value = schedule.daysOfWeek
                _selectedTime.value = schedule.time
                _medicationName.value = schedule.medicationName
                _uiState.value = ScheduleUiState.Loaded(schedule)
                validateSchedule()
            } ?: run {
                _uiState.value = ScheduleUiState.Empty
                _isValid.value = false
            }
        }
    }

    /**
     * Get all schedules (placeholder - will be replaced with repository).
     */
    fun getAllSchedules(): List<MedicationSchedule> {
        return _allSchedules.ifEmpty { _currentSchedule?.let { listOf(it) } ?: emptyList() }
    }

    /**
     * Update selected compartment.
     */
    fun updateSelectedCompartment(compartmentNumber: Int) {
        _selectedCompartment.value = compartmentNumber
        validateSchedule()
    }

    /**
     * Update selected days of week.
     */
    fun updateSelectedDays(days: Set<DayOfWeek>) {
        _selectedDays.value = days
        validateSchedule()
    }

    /**
     * Update selected time.
     */
    fun updateSelectedTime(time: LocalTime) {
        _selectedTime.value = time
        validateSchedule()
    }

    /**
     * Update medication name.
     */
    fun updateMedicationName(name: String) {
        _medicationName.value = name.ifBlank { "Medication" }
    }

    /**
     * Validate current form state.
     * Schedule is valid if:
     * - Compartment is selected (1 or 2)
     * - At least one day is selected
     * - Time is selected
     */
    fun validateSchedule() {
        val compartmentValid = _selectedCompartment.value in 1..2
        val daysValid = _selectedDays.value.isNotEmpty()
        val timeValid = _selectedTime.value != null
        
        _isValid.value = compartmentValid && daysValid && timeValid
        
        if (!compartmentValid) {
            Log.d(TAG, "Validation failed: No compartment selected")
        }
        if (!daysValid) {
            Log.d(TAG, "Validation failed: No days selected")
        }
        if (!timeValid) {
            Log.d(TAG, "Validation failed: No time selected")
        }
    }

    /**
     * Save the current schedule.
     * Later, this will save to ScheduleRepository.
     */
    fun saveSchedule() {
        if (!_isValid.value) {
            Log.w(TAG, "Cannot save: Schedule is not valid")
            return
        }

        val compartmentNumber = _selectedCompartment.value ?: run {
            Log.e(TAG, "Cannot save: Compartment is null")
            return
        }
        val days = _selectedDays.value
        val time = _selectedTime.value ?: run {
            Log.e(TAG, "Cannot save: Time is null")
            return
        }
        val name = _medicationName.value.ifBlank { "Medication" }

        try {
            val schedule = MedicationSchedule(
                id = _currentSchedule?.id ?: UUID.randomUUID().toString(),
                compartmentNumber = compartmentNumber,
                medicationName = name,
                daysOfWeek = days,
                time = time,
                pillCount = 1, // Always 1 for MVP
                isActive = true,
                createdAt = _currentSchedule?.createdAt ?: System.currentTimeMillis()
            )

            // Save to in-memory storage (will be replaced with repository)
            // Placeholder: Add to list of schedules
            val existingIndex = _allSchedules.indexOfFirst { 
                it.id == schedule.id || 
                (it.compartmentNumber == schedule.compartmentNumber && it.time == schedule.time)
            }
            if (existingIndex >= 0) {
                _allSchedules[existingIndex] = schedule
            } else {
                _allSchedules.add(schedule)
            }
            _currentSchedule = schedule
            _uiState.value = ScheduleUiState.Loaded(schedule)
            
            Log.d(TAG, "Schedule saved: $schedule")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving schedule", e)
            _uiState.value = ScheduleUiState.Error("Failed to save schedule: ${e.message}")
        }
    }

    /**
     * Reset/clear the current schedule.
     * Later, this will delete from ScheduleRepository.
     */
    fun resetSchedule() {
        _currentSchedule = null
        _selectedCompartment.value = null
        _selectedDays.value = emptySet()
        _selectedTime.value = null
        _medicationName.value = "Medication"
        _isValid.value = false
        _uiState.value = ScheduleUiState.Empty
        
        Log.d(TAG, "Schedule reset")
    }

    /**
     * Get current schedule if it exists.
     */
    fun getCurrentSchedule(): MedicationSchedule? = _currentSchedule

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ScheduleViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

