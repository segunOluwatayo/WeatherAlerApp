package com.example.weatheralertapp.database

import android.app.Application

class WeatherAlertApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}