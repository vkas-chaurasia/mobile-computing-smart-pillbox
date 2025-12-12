package com.teamA.pillbox.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamA.pillbox.domain.ConsumptionRecord
import com.teamA.pillbox.domain.ConsumptionStatus
import com.teamA.pillbox.domain.Statistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/**
 * ViewModel for the History screen.
 * 
 * Currently uses in-memory storage.
 * Later, this will be connected to HistoryRepository.
 */
class HistoryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val TAG = "HistoryViewModel"

    // In-memory storage (will be replaced with repository)
    private val _allRecords = mutableListOf<ConsumptionRecord>()

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    // Filter state
    private val _selectedFilter = MutableStateFlow<ConsumptionStatus?>(null)
    val selectedFilter: StateFlow<ConsumptionStatus?> = _selectedFilter.asStateFlow()

    // Filtered records based on selected filter
    private val _filteredRecords = MutableStateFlow<List<ConsumptionRecord>>(emptyList())
    val filteredRecords: StateFlow<List<ConsumptionRecord>> = _filteredRecords.asStateFlow()

    // Statistics
    private val _statistics = MutableStateFlow<Statistics?>(null)
    val statistics: StateFlow<Statistics?> = _statistics.asStateFlow()

    init {
        loadHistory()
    }

    /**
     * Load all history records from in-memory storage.
     * Later, this will load from HistoryRepository.
     */
    fun loadHistory() {
        try {
            val records = _allRecords.sortedByDescending { 
                it.date.atTime(it.scheduledTime)
            }
            
            val stats = calculateStatistics(records)
            _statistics.value = stats

            when {
                records.isEmpty() -> {
                    _uiState.value = HistoryUiState.Empty(stats)
                    _filteredRecords.value = emptyList()
                }
                else -> {
                    _uiState.value = HistoryUiState.Loaded(records, stats)
                    applyFilter(_selectedFilter.value)
                }
            }
            
            Log.d(TAG, "History loaded: ${records.size} records")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading history", e)
            _uiState.value = HistoryUiState.Error("Failed to load history: ${e.message}")
        }
    }

    /**
     * Apply filter to history records.
     * @param status Filter by status (null = show all)
     */
    fun applyFilter(status: ConsumptionStatus?) {
        _selectedFilter.value = status
        
        val allRecords = when (_uiState.value) {
            is HistoryUiState.Loaded -> (_uiState.value as HistoryUiState.Loaded).records
            else -> emptyList()
        }
        
        val filtered = if (status == null) {
            allRecords
        } else {
            allRecords.filter { it.status == status }
        }
        
        _filteredRecords.value = filtered
        Log.d(TAG, "Filter applied: $status, showing ${filtered.size} records")
    }

    /**
     * Calculate statistics from records.
     * Default period: Last 7 days to today.
     */
    private fun calculateStatistics(records: List<ConsumptionRecord>): Statistics {
        val today = LocalDate.now()
        val startDate = today.minusDays(7) // Last 7 days
        val endDate = today

        // Filter records within date range
        val periodRecords = records.filter { 
            it.date.isAfter(startDate.minusDays(1)) && 
            !it.date.isAfter(endDate)
        }

        val totalScheduled = periodRecords.size
        val totalTaken = periodRecords.count { it.status == ConsumptionStatus.TAKEN }
        val totalMissed = periodRecords.count { it.status == ConsumptionStatus.MISSED }
        val totalPending = periodRecords.count { it.status == ConsumptionStatus.PENDING }

        val compliancePercentage = Statistics.calculateCompliance(totalTaken, totalMissed)
        val currentStreak = calculateStreak(records)

        return Statistics(
            startDate = startDate,
            endDate = endDate,
            totalScheduled = totalScheduled,
            totalTaken = totalTaken,
            totalMissed = totalMissed,
            totalPending = totalPending,
            compliancePercentage = compliancePercentage,
            currentStreak = currentStreak
        )
    }

    /**
     * Calculate current streak of consecutive days with medication taken.
     * Streak counts backwards from today.
     */
    private fun calculateStreak(records: List<ConsumptionRecord>): Int {
        val today = LocalDate.now()
        var streak = 0
        var currentDate = today

        // Sort records by date (descending)
        val sortedRecords = records
            .filter { it.status == ConsumptionStatus.TAKEN }
            .sortedByDescending { it.date }
            .groupBy { it.date }

        // Count consecutive days from today backwards
        while (true) {
            val hasRecord = sortedRecords.containsKey(currentDate) && 
                           sortedRecords[currentDate]?.any { it.status == ConsumptionStatus.TAKEN } == true
            
            if (hasRecord) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }

    /**
     * Get today's consumption record if it exists.
     */
    fun getTodayRecord(): ConsumptionRecord? {
        val today = LocalDate.now()
        return _allRecords.find { it.date == today }
    }

    /**
     * Create a new consumption record (for testing/manual entry).
     * Later, this will be handled by HistoryRepository.
     */
    fun createRecord(record: ConsumptionRecord) {
        _allRecords.add(record)
        loadHistory() // Reload to update UI
        Log.d(TAG, "Record created: $record")
    }

    /**
     * Update an existing consumption record.
     * Later, this will be handled by HistoryRepository.
     */
    fun updateRecord(record: ConsumptionRecord) {
        val index = _allRecords.indexOfFirst { it.id == record.id }
        if (index >= 0) {
            _allRecords[index] = record
            loadHistory() // Reload to update UI
            Log.d(TAG, "Record updated: $record")
        } else {
            Log.w(TAG, "Record not found for update: ${record.id}")
        }
    }

    /**
     * Get records by date range.
     */
    fun getRecordsByDateRange(start: LocalDate, end: LocalDate): List<ConsumptionRecord> {
        return _allRecords.filter { 
            it.date.isAfter(start.minusDays(1)) && !it.date.isAfter(end)
        }.sortedByDescending { it.date }
    }

    /**
     * Get records by status.
     */
    fun getRecordsByStatus(status: ConsumptionStatus): List<ConsumptionRecord> {
        return _allRecords.filter { it.status == status }
            .sortedByDescending { it.date }
    }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HistoryViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

