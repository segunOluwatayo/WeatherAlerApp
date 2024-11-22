package com.example.weatheralertapp

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class WeatherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeWorkManager()
    }

    private fun initializeWorkManager() {
        try {
            val config = Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()

            // Initialize WorkManager properly
            WorkManager.initialize(applicationContext, config)

            // Start periodic checks
            WeatherAlertWorker.startPeriodicChecks(this)

        } catch (e: Exception) {
            println("WorkManager initialization error: ${e.message}")
        }
    }
}