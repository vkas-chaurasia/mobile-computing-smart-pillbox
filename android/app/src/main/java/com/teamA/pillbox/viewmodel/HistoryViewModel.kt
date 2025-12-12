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
import com.teamA.pillbox.repository.HistoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for the History screen.
 * 
 * Uses HistoryRepository for data persistence.
 */
class HistoryViewModel(
    application: Application,
    private val historyRepository: HistoryRepository
) : AndroidViewModel(application) {

    private val TAG = "HistoryViewModel"

    // All records from repository (reactive)
    private val _allRecords = historyRepository.getAllRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
        // Observe all records and update UI state
        viewModelScope.launch {
            _allRecords.collect { records ->
                try {
                    val today = LocalDate.now()
                    val startDate = today.minusDays(7) // Last 7 days
                    val stats = historyRepository.getStatistics(startDate, today, null)
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
        }
    }

    /**
     * Apply filter to history records.
     * @param status Filter by status (null = show all)
     */
    fun applyFilter(status: ConsumptionStatus?) {
        _selectedFilter.value = status
        
        val allRecords = _allRecords.value
        
        val filtered = if (status == null) {
            allRecords
        } else {
            allRecords.filter { it.status == status }
        }
        
        _filteredRecords.value = filtered
        Log.d(TAG, "Filter applied: $status, showing ${filtered.size} records")
    }

    /**
     * Get today's consumption record for a specific compartment.
     * @param compartmentNumber Compartment number (1 or 2), or null for any compartment
     */
    fun getTodayRecord(compartmentNumber: Int? = null): StateFlow<ConsumptionRecord?> {
        return historyRepository.getTodayRecord(compartmentNumber)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    /**
     * Create a new consumption record.
     */
    fun createRecord(record: ConsumptionRecord) {
        viewModelScope.launch {
            try {
                historyRepository.createRecord(record)
                Log.d(TAG, "Record created: $record")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating record", e)
                _uiState.value = HistoryUiState.Error("Failed to create record: ${e.message}")
            }
        }
    }

    /**
     * Update an existing consumption record.
     */
    fun updateRecord(record: ConsumptionRecord) {
        viewModelScope.launch {
            try {
                historyRepository.updateRecord(record)
                Log.d(TAG, "Record updated: $record")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating record", e)
                _uiState.value = HistoryUiState.Error("Failed to update record: ${e.message}")
            }
        }
    }

    /**
     * Get records by date range.
     */
    fun getRecordsByDateRange(start: LocalDate, end: LocalDate): Flow<List<ConsumptionRecord>> {
        return historyRepository.getRecordsByDateRange(start, end)
    }

    /**
     * Get records by status.
     */
    fun getRecordsByStatus(status: ConsumptionStatus): Flow<List<ConsumptionRecord>> {
        return historyRepository.getRecordsByStatus(status)
    }

    /**
     * Get statistics for a date range and optional compartment.
     */
    fun getStatistics(
        startDate: LocalDate,
        endDate: LocalDate,
        compartmentNumber: Int? = null
    ): StateFlow<Statistics> {
        return flow {
            emit(historyRepository.getStatistics(startDate, endDate, compartmentNumber))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Statistics(
                startDate = startDate,
                endDate = endDate,
                totalScheduled = 0,
                totalTaken = 0,
                totalMissed = 0,
                totalPending = 0,
                compliancePercentage = 0.0,
                currentStreak = 0
            )
        )
    }

    class Factory(
        private val application: Application,
        private val historyRepository: HistoryRepository? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HistoryViewModel(
                    application,
                    historyRepository ?: HistoryRepository(application)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

