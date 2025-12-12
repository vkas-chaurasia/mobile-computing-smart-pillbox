package com.teamA.pillbox.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamA.pillbox.domain.AppTheme
import com.teamA.pillbox.domain.CompartmentState
import com.teamA.pillbox.domain.SensorThresholds
import com.teamA.pillbox.repository.HistoryRepository
import com.teamA.pillbox.repository.ScheduleRepository
import com.teamA.pillbox.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 * Manages app settings, sensor thresholds, compartment states, and notifications.
 */
class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val historyRepository: HistoryRepository
) : AndroidViewModel(application) {

    private val TAG = "SettingsViewModel"

    // Theme preference
    val theme: StateFlow<AppTheme> = settingsRepository.theme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    // Sensor thresholds
    val sensorThresholds: StateFlow<SensorThresholds> = settingsRepository.sensorThresholds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SensorThresholds()
        )

    // Compartment states
    val compartment1State: StateFlow<CompartmentState> = settingsRepository.compartment1State
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CompartmentState.UNKNOWN
        )

    val compartment2State: StateFlow<CompartmentState> = settingsRepository.compartment2State
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CompartmentState.UNKNOWN
        )

    // Notification preferences
    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.notificationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    /**
     * Set theme preference.
     */
    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            try {
                settingsRepository.setTheme(theme)
                Log.d(TAG, "Theme set to: $theme")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting theme", e)
            }
        }
    }

    /**
     * Set light threshold for compartment 1.
     */
    fun setLightThreshold1(threshold: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setLightThreshold1(threshold)
                Log.d(TAG, "Light threshold 1 set to: $threshold")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting light threshold 1", e)
            }
        }
    }

    /**
     * Set light threshold for compartment 2.
     */
    fun setLightThreshold2(threshold: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setLightThreshold2(threshold)
                Log.d(TAG, "Light threshold 2 set to: $threshold")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting light threshold 2", e)
            }
        }
    }

    /**
     * Set tilt threshold (shared for both compartments).
     */
    fun setTiltThreshold(threshold: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.setTiltThreshold(threshold)
                Log.d(TAG, "Tilt threshold set to: $threshold")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting tilt threshold", e)
            }
        }
    }

    /**
     * Set all sensor thresholds at once.
     */
    fun setSensorThresholds(thresholds: SensorThresholds) {
        viewModelScope.launch {
            try {
                settingsRepository.setSensorThresholds(thresholds)
                Log.d(TAG, "Sensor thresholds set: $thresholds")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting sensor thresholds", e)
            }
        }
    }

    /**
     * Set compartment state for a specific compartment.
     */
    fun setCompartmentState(compartmentNumber: Int, state: CompartmentState) {
        viewModelScope.launch {
            try {
                settingsRepository.setCompartmentState(compartmentNumber, state)
                Log.d(TAG, "Compartment $compartmentNumber state set to: $state")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting compartment state", e)
            }
        }
    }

    /**
     * Set notification enabled preference.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setNotificationsEnabled(enabled)
                Log.d(TAG, "Notifications enabled set to: $enabled")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting notifications enabled", e)
            }
        }
    }

    /**
     * Reset all data (schedules, history, and settings).
     */
    fun resetAllData() {
        viewModelScope.launch {
            try {
                scheduleRepository.resetAllSchedules()
                historyRepository.deleteAllRecords()
                settingsRepository.resetAllSettings()
                Log.d(TAG, "All data reset")
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting all data", e)
            }
        }
    }

    class Factory(
        private val application: Application,
        private val settingsRepository: SettingsRepository? = null,
        private val scheduleRepository: ScheduleRepository? = null,
        private val historyRepository: HistoryRepository? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(
                    application,
                    settingsRepository ?: SettingsRepository(application),
                    scheduleRepository ?: ScheduleRepository(application),
                    historyRepository ?: HistoryRepository(application)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
