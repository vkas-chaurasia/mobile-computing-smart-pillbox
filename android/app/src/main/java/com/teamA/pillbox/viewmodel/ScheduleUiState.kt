package com.teamA.pillbox.viewmodel

import com.teamA.pillbox.domain.MedicationSchedule

/**
 * UI state for the Schedule Setup screen.
 */
sealed class ScheduleUiState {
    /**
     * Initial state - no schedule exists.
     */
    object Empty : ScheduleUiState()
    
    /**
     * Schedule exists and is loaded.
     */
    data class Loaded(
        val schedule: MedicationSchedule
    ) : ScheduleUiState()
    
    /**
     * Loading state (for future use when connecting to repository).
     */
    object Loading : ScheduleUiState()
    
    /**
     * Error state (for future use when connecting to repository).
     */
    data class Error(
        val message: String
    ) : ScheduleUiState()
}

