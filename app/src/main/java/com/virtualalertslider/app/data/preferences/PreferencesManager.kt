package com.virtualalertslider.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.domain.model.SliderSettings
import com.virtualalertslider.app.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "virtual_alert_slider_prefs")

class PreferencesManager(
    private val context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore
) {

    private object PreferencesKeys {
        val LAST_SELECTED_MODE = stringPreferencesKey("last_selected_mode")
        val DEFAULT_MODE = stringPreferencesKey("default_mode")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val BYPASS_STUCK_SLIDER = booleanPreferencesKey("bypass_stuck_slider")
    }

    val selectedModeFlow: Flow<AlertMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val modeId = preferences[PreferencesKeys.LAST_SELECTED_MODE]
            AlertMode.fromId(modeId)
        }

    val rawSelectedModeFlow: Flow<AlertMode?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val modeId = preferences[PreferencesKeys.LAST_SELECTED_MODE]
            if (modeId != null) AlertMode.fromId(modeId) else null
        }

    val settingsFlow: Flow<SliderSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            SliderSettings(
                defaultMode = AlertMode.fromId(preferences[PreferencesKeys.DEFAULT_MODE]),
                hapticFeedbackEnabled = preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] ?: true,
                themePreference = ThemePreference.fromName(preferences[PreferencesKeys.THEME_PREFERENCE]),
                bypassStuckSlider = preferences[PreferencesKeys.BYPASS_STUCK_SLIDER] ?: true
            )
        }

    suspend fun saveSelectedMode(mode: AlertMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SELECTED_MODE] = mode.id
        }
    }

    suspend fun saveDefaultMode(mode: AlertMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_MODE] = mode.id
        }
    }

    suspend fun saveHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    suspend fun saveThemePreference(themePreference: ThemePreference) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_PREFERENCE] = themePreference.name
        }
    }

    suspend fun saveBypassStuckSlider(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BYPASS_STUCK_SLIDER] = enabled
        }
    }
}
