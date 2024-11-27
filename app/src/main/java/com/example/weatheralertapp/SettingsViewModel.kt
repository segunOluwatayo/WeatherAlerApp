
package com.example.weatheralertapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferencesDao = AppDatabase.getDatabase(application).userPreferencesDao()

    private val _preferences = MutableStateFlow(UserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            try {
                val prefs = userPreferencesDao.getPreferences()
                _preferences.value = prefs ?: UserPreferences()
            } catch (e: Exception) {
                _saveStatus.value = SaveStatus.Error("Failed to load preferences: ${e.message}")
            }
        }
    }

    fun updatePreferences(
        fires: Boolean = preferences.value.fires,
        wind: Boolean = preferences.value.wind,
        floods: Boolean = preferences.value.floods,
        winter: Boolean = preferences.value.winter,
        thunderstorms: Boolean = preferences.value.thunderstorms,
        temperature: Boolean = preferences.value.temperature,
        tropical: Boolean = preferences.value.tropical,
        marine: Boolean = preferences.value.marine,
        fog: Boolean = preferences.value.fog,
        tornado: Boolean = preferences.value.tornado,
        notificationsEnabled: Boolean = preferences.value.notificationsEnabled,
        themeMode: ThemeMode = preferences.value.themeMode
    ) {

        viewModelScope.launch {
            try {
                println("Updating theme mode to: $themeMode")
                _saveStatus.value = SaveStatus.Saving
                val updatedPreferences = preferences.value.copy(
                    fires = fires,
                    wind = wind,
                    floods = floods,
                    winter = winter,
                    thunderstorms = thunderstorms,
                    temperature = temperature,
                    tropical = tropical,
                    marine = marine,
                    fog = fog,
                    tornado = tornado,
                    notificationsEnabled = notificationsEnabled,
                    themeMode = themeMode // Update theme mode
                )
                // Wait for the database operation to complete
                userPreferencesDao.insertPreferences(updatedPreferences)

                // Restart the worker to pick up new preferences
                WeatherAlertWorker.startImmediateCheck(getApplication())
                _preferences.value = updatedPreferences
                userPreferencesDao.insertPreferences(updatedPreferences)
                _saveStatus.value = SaveStatus.Success
            } catch (e: Exception) {
                _saveStatus.value = SaveStatus.Error("Failed to save preferences: ${e.message}")
            }
        }
    }
}


sealed class SaveStatus {
    data object Idle : SaveStatus()
    data object Saving : SaveStatus()
    data object Success : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}

