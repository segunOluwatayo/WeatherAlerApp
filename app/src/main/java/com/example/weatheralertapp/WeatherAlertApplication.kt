package com.example.weatheralertapp

import android.app.Application

class WeatherAlertApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}