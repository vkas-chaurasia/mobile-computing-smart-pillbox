package com.teamA.pillbox.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamA.pillbox.domain.MedicationSchedule
import com.teamA.pillbox.repository.ScheduleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

/**
 * ViewModel for the Schedule Setup screen.
 * 
 * Uses ScheduleRepository for data persistence.
 */
class ScheduleViewModel(
    application: Application,
    private val scheduleRepository: ScheduleRepository
) : AndroidViewModel(application) {

    private val TAG = "ScheduleViewModel"

    // All schedules from repository (reactive)
    val allSchedules: StateFlow<List<MedicationSchedule>> = scheduleRepository.getAllSchedules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
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
        // Observe all schedules and selected compartment to update UI state
        viewModelScope.launch {
            combine(allSchedules, _selectedCompartment) { schedules, selectedCompartment ->
                // Find schedule for selected compartment
                val scheduleForCompartment = selectedCompartment?.let { comp ->
                    schedules.firstOrNull { it.compartmentNumber == comp }
                }
                
                when {
                    scheduleForCompartment != null -> {
                        _uiState.value = ScheduleUiState.Loaded(scheduleForCompartment)
                        // Load schedule data into form fields if not already set
                        if (_selectedDays.value.isEmpty() && _selectedTime.value == null) {
                            _selectedDays.value = scheduleForCompartment.daysOfWeek
                            _selectedTime.value = scheduleForCompartment.time
                            _medicationName.value = scheduleForCompartment.medicationName
                            validateSchedule()
                        }
                    }
                    schedules.isEmpty() -> {
                        _uiState.value = ScheduleUiState.Empty
                    }
                    else -> {
                        // No schedule for selected compartment, but schedules exist
                        _uiState.value = ScheduleUiState.Empty
                    }
                }
            }.collect()
        }
    }

    /**
     * Update selected compartment.
     * Loads existing schedule for that compartment if one exists.
     */
    fun updateSelectedCompartment(compartmentNumber: Int) {
        _selectedCompartment.value = compartmentNumber
        
        // Load existing schedule for this compartment into form fields
        val existingSchedule = allSchedules.value.firstOrNull { 
            it.compartmentNumber == compartmentNumber 
        }
        
        if (existingSchedule != null) {
            // Load existing schedule data
            _selectedDays.value = existingSchedule.daysOfWeek
            _selectedTime.value = existingSchedule.time
            _medicationName.value = existingSchedule.medicationName
        } else {
            // Clear form for new schedule
            _selectedDays.value = emptySet()
            _selectedTime.value = null
            _medicationName.value = "Medication"
        }
        
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
     * Save the current schedule to repository.
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

        viewModelScope.launch {
            try {
                // Check if we're updating an existing schedule for this compartment
                // Find by compartment number (not compartment + time, so we can update time)
                val existingSchedule = allSchedules.value.firstOrNull { 
                    it.compartmentNumber == compartmentNumber
                }

                val schedule = MedicationSchedule(
                    id = existingSchedule?.id ?: UUID.randomUUID().toString(),
                    compartmentNumber = compartmentNumber,
                    medicationName = name,
                    daysOfWeek = days,
                    time = time,
                    pillCount = 1, // Always 1 for MVP
                    isActive = true,
                    createdAt = existingSchedule?.createdAt ?: System.currentTimeMillis()
                )

                scheduleRepository.saveSchedule(schedule)
                
                Log.d(TAG, if (existingSchedule != null) "Schedule updated: $schedule" else "Schedule created: $schedule")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving schedule", e)
                _uiState.value = ScheduleUiState.Error("Failed to save schedule: ${e.message}")
            }
        }
    }

    /**
     * Delete a schedule by ID.
     */
    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            try {
                scheduleRepository.deleteSchedule(id)
                Log.d(TAG, "Schedule deleted: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting schedule", e)
                _uiState.value = ScheduleUiState.Error("Failed to delete schedule: ${e.message}")
            }
        }
    }

    /**
     * Reset/clear the current form (does not delete from repository).
     */
    fun resetSchedule() {
        val currentCompartment = _selectedCompartment.value
        _selectedCompartment.value = null
        _selectedDays.value = emptySet()
        _selectedTime.value = null
        _medicationName.value = "Medication"
        _isValid.value = false
        
        // If there was a schedule for the current compartment, delete it
        currentCompartment?.let { comp ->
            viewModelScope.launch {
                val scheduleToDelete = allSchedules.value.firstOrNull { 
                    it.compartmentNumber == comp 
                }
                scheduleToDelete?.let {
                    deleteSchedule(it.id)
                }
            }
        }
        
        Log.d(TAG, "Schedule form reset")
    }

    /**
     * Reset all schedules (delete all from repository).
     * Also clears the form fields.
     */
    fun resetAllSchedules() {
        viewModelScope.launch {
            try {
                scheduleRepository.resetAllSchedules()
                // Clear form fields
                _selectedCompartment.value = null
                _selectedDays.value = emptySet()
                _selectedTime.value = null
                _medicationName.value = "Medication"
                _isValid.value = false
                _uiState.value = ScheduleUiState.Empty
                Log.d(TAG, "All schedules reset")
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting schedules", e)
                _uiState.value = ScheduleUiState.Error("Failed to reset schedules: ${e.message}")
            }
        }
    }

    class Factory(
        private val application: Application,
        private val scheduleRepository: ScheduleRepository? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ScheduleViewModel(
                    application,
                    scheduleRepository ?: ScheduleRepository(application)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

