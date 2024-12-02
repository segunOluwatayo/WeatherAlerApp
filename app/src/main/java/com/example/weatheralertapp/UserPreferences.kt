
package com.example.weatheralertapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 1, // Singleton pattern
    val fires: Boolean = true,
    val wind: Boolean = true,
    val winter: Boolean = true,
    val thunderstorms: Boolean = true,
    val floods: Boolean = true,
    val temperature: Boolean = true,
    val tropical: Boolean = true,
    val marine: Boolean = true,
    val fog: Boolean = true,
    val tornado: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
