package com.example.weatheralertapp

import android.content.Context
import com.example.weatheralertapp.com.example.weatheralertapp.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherAlertProcessor(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val preferencesDao = database.userPreferencesDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun shouldProcessAlert(event: Event): Boolean {
        val preferences = withContext(Dispatchers.IO) {
            preferencesDao.getPreferences() ?: UserPreferences()
        }

        if (!preferences.notificationsEnabled) {
            return false
        }

        // Map Tomorrow.io event types to our preference categories
        return when {
            event.insight.contains("fires", ignoreCase = true) -> preferences.fires
            event.insight.contains("wind", ignoreCase = true) -> preferences.wind
            event.insight.contains("winter", ignoreCase = true) -> preferences.winter
            event.insight.contains("thunderstorm", ignoreCase = true) -> preferences.thunderstorms
            event.insight.contains("flood", ignoreCase = true) -> preferences.floods
            event.insight.contains("temperature", ignoreCase = true) -> preferences.temperature
            event.insight.contains("tropical", ignoreCase = true) -> preferences.tropical
            event.insight.contains("marine", ignoreCase = true) -> preferences.marine
            event.insight.contains("fog", ignoreCase = true) -> preferences.fog
            event.insight.contains("tornado", ignoreCase = true) -> preferences.tornado
            else -> false  // Don't process alerts that don't match any category
        }
    }
}