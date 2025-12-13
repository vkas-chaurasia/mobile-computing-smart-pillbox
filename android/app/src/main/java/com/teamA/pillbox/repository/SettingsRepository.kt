package com.teamA.pillbox.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamA.pillbox.data.dataStore
import com.teamA.pillbox.domain.AppTheme
import com.teamA.pillbox.domain.CompartmentState
import com.teamA.pillbox.domain.SensorThresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing app settings.
 * Uses DataStore (Preferences) for persistence.
 * Stores theme, sensor thresholds, compartment states, and notification preferences.
 */
class SettingsRepository(context: Context) {
    
    // Use the singleton DataStore instance from AppDataStore.kt
    private val dataStore = context.dataStore

    // Preference keys
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val LIGHT_THRESHOLD_1 = intPreferencesKey("light_threshold_1")
        val LIGHT_THRESHOLD_2 = intPreferencesKey("light_threshold_2")
        val TILT_THRESHOLD = intPreferencesKey("tilt_threshold")
        val COMPARTMENT_1_STATE = stringPreferencesKey("compartment_1_state")
        val COMPARTMENT_2_STATE = stringPreferencesKey("compartment_2_state")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    // Default values
    private val defaultTheme = AppTheme.SYSTEM
    private val defaultLightThreshold1 = 40
    private val defaultLightThreshold2 = 40
    private val defaultTiltThreshold = 1
    private val defaultCompartmentState = CompartmentState.UNKNOWN
    private val defaultNotificationsEnabled = true

    /**
     * Get current theme preference.
     * @return Flow of theme preference
     */
    val theme: Flow<AppTheme> = dataStore.data.map { preferences ->
        val themeString = preferences[Keys.THEME] ?: defaultTheme.name
        try {
            AppTheme.valueOf(themeString)
        } catch (e: IllegalArgumentException) {
            defaultTheme
        }
    }

    /**
     * Set theme preference.
     * @param theme Theme to set
     */
    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME] = theme.name
        }
    }

    /**
     * Get sensor thresholds.
     * @return Flow of sensor thresholds
     */
    val sensorThresholds: Flow<SensorThresholds> = dataStore.data.map { preferences ->
        SensorThresholds(
            lightThreshold1 = preferences[Keys.LIGHT_THRESHOLD_1] ?: defaultLightThreshold1,
            lightThreshold2 = preferences[Keys.LIGHT_THRESHOLD_2] ?: defaultLightThreshold2,
            tiltThreshold = preferences[Keys.TILT_THRESHOLD] ?: defaultTiltThreshold
        )
    }

    /**
     * Set light threshold for compartment 1.
     * @param threshold Threshold value (0-100)
     */
    suspend fun setLightThreshold1(threshold: Int) {
        require(threshold in 0..100) { "Light threshold must be between 0 and 100" }
        dataStore.edit { preferences ->
            preferences[Keys.LIGHT_THRESHOLD_1] = threshold
        }
    }

    /**
     * Set light threshold for compartment 2.
     * @param threshold Threshold value (0-100)
     */
    suspend fun setLightThreshold2(threshold: Int) {
        require(threshold in 0..100) { "Light threshold must be between 0 and 100" }
        dataStore.edit { preferences ->
            preferences[Keys.LIGHT_THRESHOLD_2] = threshold
        }
    }

    /**
     * Set tilt threshold (shared for both compartments).
     * @param threshold Threshold value (>= 0)
     */
    suspend fun setTiltThreshold(threshold: Int) {
        require(threshold >= 0) { "Tilt threshold must be non-negative" }
        dataStore.edit { preferences ->
            preferences[Keys.TILT_THRESHOLD] = threshold
        }
    }

    /**
     * Set all sensor thresholds at once.
     * @param thresholds Sensor thresholds to set
     */
    suspend fun setSensorThresholds(thresholds: SensorThresholds) {
        dataStore.edit { preferences ->
            preferences[Keys.LIGHT_THRESHOLD_1] = thresholds.lightThreshold1
            preferences[Keys.LIGHT_THRESHOLD_2] = thresholds.lightThreshold2
            preferences[Keys.TILT_THRESHOLD] = thresholds.tiltThreshold
        }
    }

    /**
     * Get compartment state for compartment 1.
     * @return Flow of compartment 1 state
     */
    val compartment1State: Flow<CompartmentState> = dataStore.data.map { preferences ->
        val stateString = preferences[Keys.COMPARTMENT_1_STATE] ?: defaultCompartmentState.name
        try {
            CompartmentState.valueOf(stateString)
        } catch (e: IllegalArgumentException) {
            defaultCompartmentState
        }
    }

    /**
     * Get compartment state for compartment 2.
     * @return Flow of compartment 2 state
     */
    val compartment2State: Flow<CompartmentState> = dataStore.data.map { preferences ->
        val stateString = preferences[Keys.COMPARTMENT_2_STATE] ?: defaultCompartmentState.name
        try {
            CompartmentState.valueOf(stateString)
        } catch (e: IllegalArgumentException) {
            defaultCompartmentState
        }
    }

    /**
     * Set compartment state for a specific compartment.
     * @param compartmentNumber Compartment number (1 or 2)
     * @param state Compartment state to set
     */
    suspend fun setCompartmentState(compartmentNumber: Int, state: CompartmentState) {
        require(compartmentNumber in 1..2) { "Compartment number must be 1 or 2" }
        dataStore.edit { preferences ->
            when (compartmentNumber) {
                1 -> preferences[Keys.COMPARTMENT_1_STATE] = state.name
                2 -> preferences[Keys.COMPARTMENT_2_STATE] = state.name
            }
        }
    }

    /**
     * Get notification enabled preference.
     * @return Flow of notification enabled state
     */
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.NOTIFICATIONS_ENABLED] ?: defaultNotificationsEnabled
    }

    /**
     * Set notification enabled preference.
     * @param enabled Whether notifications are enabled
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * Reset all settings to defaults.
     */
    suspend fun resetAllSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
