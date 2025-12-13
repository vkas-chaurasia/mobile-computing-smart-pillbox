package com.teamA.pillbox.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Singleton DataStore instance for app settings.
 * Using the preferencesDataStore property delegate ensures only one instance
 * is created for the pillbox_settings file throughout the app lifecycle.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pillbox_settings")
