
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

    init {
        viewModelScope.launch {
            val prefs = userPreferencesDao.getPreferences()
            _preferences.value = prefs ?: UserPreferences()
        }
    }

    fun updatePreferences(
        storms: Boolean = preferences.value.storms,
        heatwaves: Boolean = preferences.value.heatwaves,
        floods: Boolean = preferences.value.floods,
        coldSnaps: Boolean = preferences.value.coldSnaps,
        notificationsEnabled: Boolean = preferences.value.notificationsEnabled
    ) {
        val updatedPreferences = preferences.value.copy(
            storms = storms,
            heatwaves = heatwaves,
            floods = floods,
            coldSnaps = coldSnaps,
            notificationsEnabled = notificationsEnabled
        )
        viewModelScope.launch {
            userPreferencesDao.insertPreferences(updatedPreferences)
            _preferences.value = updatedPreferences
        }
    }
}
