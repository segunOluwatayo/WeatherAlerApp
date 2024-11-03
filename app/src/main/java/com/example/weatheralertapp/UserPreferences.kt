
package com.example.weatheralertapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 1, // Singleton pattern
    val storms: Boolean = true,
    val heatwaves: Boolean = true,
    val floods: Boolean = true,
    val coldSnaps: Boolean = true,
    val notificationsEnabled: Boolean = true
)
