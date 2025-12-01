package com.teamA.pillbox.viewmodel

import com.teamA.pillbox.domain.ConsumptionRecord
import com.teamA.pillbox.domain.Statistics

/**
 * UI state for the History screen.
 */
sealed class HistoryUiState {
    /**
     * Initial state - loading history.
     */
    object Loading : HistoryUiState()
    
    /**
     * History loaded successfully.
     */
    data class Loaded(
        val records: List<ConsumptionRecord>,
        val statistics: Statistics
    ) : HistoryUiState()
    
    /**
     * Empty state - no history records.
     */
    data class Empty(
        val statistics: Statistics
    ) : HistoryUiState()
    
    /**
     * Error state.
     */
    data class Error(
        val message: String
    ) : HistoryUiState()
}

