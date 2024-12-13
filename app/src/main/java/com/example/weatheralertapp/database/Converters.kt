
package com.example.weatheralertapp.database

import androidx.room.TypeConverter
import com.example.weatheralertapp.theme.ThemeMode

class Converters {
    @TypeConverter
    fun fromThemeMode(value: ThemeMode): String = value.name

    @TypeConverter
    fun toThemeMode(value: String): ThemeMode = ThemeMode.valueOf(value)
}
