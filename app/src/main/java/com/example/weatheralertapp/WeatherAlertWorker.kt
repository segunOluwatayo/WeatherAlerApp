package com.example.weatheralertapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.weatheralertapp.com.example.weatheralertapp.AlertItem
import com.example.weatheralertapp.com.example.weatheralertapp.WeatherService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.Manifest
import com.example.weatheralertapp.com.example.weatheralertapp.GeoJsonLocation

// A class for performing background tasks to fetch and notify weather alerts
class WeatherAlertWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val alertProcessor = WeatherAlertProcessor(context)
    private val weatherService = Retrofit.Builder()
        .baseUrl(Constants.TOMORROW_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(WeatherService::class.java)

    // Functions for geospatial calculations
    object GeoUtils {
        fun isWithinRange(
            centerLat: Double,
            centerLon: Double,
            targetLat: Double,
            targetLon: Double,
            radiusKm: Double = 50.0
        ): Boolean {
            // Print input coordinates for verification
//            println("""
//            |Checking distance:
//            |Center: ${formatCoordinate(centerLat)}°N, ${formatCoordinate(centerLon)}°E
//            |Target: ${formatCoordinate(targetLat)}°N, ${formatCoordinate(targetLon)}°E
//        """.trimMargin())

            val R = 6371.0 // Earth's radius in kilometers

            // Convert to radians
            val centerLatRad = Math.toRadians(centerLat)
            val centerLonRad = Math.toRadians(centerLon)
            val targetLatRad = Math.toRadians(targetLat)
            val targetLonRad = Math.toRadians(targetLon)

            // Calculate differences
            val dLon = targetLonRad - centerLonRad
            val dLat = targetLatRad - centerLatRad

            // Haversine formula
            val a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                    Math.cos(centerLatRad) * Math.cos(targetLatRad) *
                    Math.sin(dLon/2) * Math.sin(dLon/2)

            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            val distance = R * c

            // Detailed logging with formatted numbers
//            println("""
//            |Distance Calculation Results:
//            |--------------------------------
//            |Distance: ${String.format("%.2f", distance)} km
//            |Radius limit: $radiusKm km
//            |Within range: ${distance <= radiusKm}
//            |--------------------------------
//        """.trimMargin())

            return distance <= radiusKm
        }

        // Check if any point in a polygon is within a specified radius of a central location
        fun isPolygonInRange(
            centerLat: Double,
            centerLon: Double,
            coordinates: List<List<List<Double>>>,
            radiusKm: Double = 50.0
        ): Boolean {
//            println("""
//            |Starting Polygon Range Check:
//            |--------------------------------
//            |Center: ${formatCoordinate(centerLat)}°N, ${formatCoordinate(centerLon)}°E
//            |Number of points: ${coordinates[0].size}
//            |Radius: $radiusKm km
//            |--------------------------------
//        """.trimMargin())

            // Check each vertex of the polygon
            val result = coordinates[0].any { coordinate ->
                // GeoJSON uses [longitude, latitude] order
                val targetLon = coordinate[0]
                val targetLat = coordinate[1]

                // Check if any point is within range
                isWithinRange(
                    centerLat = centerLat,
                    centerLon = centerLon,
                    targetLat = targetLat,
                    targetLon = targetLon,
                    radiusKm = radiusKm
                )
            }

//            println("Polygon check complete. Result: $result")
            return result
        }

        // Data class to uniquely identify alerts
        data class AlertKey(
            val title: String,
            val startTime: String,
            val endTime: String,
            val severity: String,
            val location: GeoJsonLocation?
        )

        private fun formatCoordinate(coord: Double): String {
            return String.format("%.6f", coord)
        }
    }

    // Primary function executed by the Worker
    override suspend fun doWork(): Result {
        try {
            println("WeatherAlertWorker: Starting background check")

            // Add retry handling
            if (runAttemptCount > MAX_RETRY_ATTEMPTS) {
                println("WeatherAlertWorker: Too many retry attempts")
                return Result.failure()
            }

            // Check battery status from input data
            val isBatteryLow = inputData.getBoolean(KEY_BATTERY_LOW, false)
            if (isBatteryLow) {
                println("WeatherAlertWorker: Running in battery saving mode")
            }

            clearExpiredAlerts() // Clear expired alerts first

            val sharedPrefs = applicationContext.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
            val userLat = sharedPrefs.getFloat("last_latitude", 0f).toDouble()
            val userLon = sharedPrefs.getFloat("last_longitude", 0f).toDouble()
            val radiusKm = sharedPrefs.getFloat("alert_radius_km", DEFAULT_RADIUS_KM)

            // Validate location data
            if (!isLocationValid(userLat, userLon)) {
                println("WeatherAlertWorker: Invalid location data (${userLat}, ${userLon})")
                return Result.retry()
            }

            // API call with error handling
            val response = try {
                weatherService.getWeatherAlerts(
                    location = "$userLat,$userLon",
                    apikey = Constants.TOMORROW_API_KEY
                )
            } catch (e: Exception) {
                println("WeatherAlertWorker: API error: ${e.message}")
                return if (isRecoverableError(e)) {
                    println("WeatherAlertWorker: Scheduling retry for recoverable error")
                    Result.retry()
                } else {
                    println("WeatherAlertWorker: Non-recoverable API error")
                    Result.failure()
                }
            }

            println("Processing ${response.data.events.size} total events")

            val alerts = response.data.events
                .filter { event ->
                    // First check if the alert should be processed based on user preferences
                    alertProcessor.shouldProcessAlert(event)
                }

                                .mapNotNull { event ->
                try {
                    val isInRange = event.eventValues.location?.let { location ->
                        when (location.type) {
                            "Polygon", "MultiPolygon" -> {
//                                println("Checking ${location.type} alert: ${event.eventValues.title}")
                                @Suppress("UNCHECKED_CAST")
                                when (location.type) {
                                    "Polygon" -> {
                                        val coordinates = location.coordinates as? List<List<List<Double>>>
                                        coordinates?.let {
                                            GeoUtils.isPolygonInRange(userLat, userLon, it, radiusKm.toDouble())
                                        } ?: false
                                    }
                                    "MultiPolygon" -> {
                                        val coordinates = location.coordinates as? List<List<List<List<Double>>>>
                                        coordinates?.any { polygon ->
                                            GeoUtils.isPolygonInRange(userLat, userLon, polygon, radiusKm.toDouble())
                                        } ?: false
                                    }
                                    else -> false
                                }
                            }
                            "Point" -> {
//                                println("Checking point alert: ${event.eventValues.title}")
                                @Suppress("UNCHECKED_CAST")
                                val coordinates = location.coordinates as? List<Double>
                                coordinates?.let {
                                    GeoUtils.isWithinRange(
                                        centerLat = userLat,
                                        centerLon = userLon,
                                        targetLat = coordinates[1],
                                        targetLon = coordinates[0],
                                        radiusKm = radiusKm.toDouble()
                                    )
                                } ?: false
                            }
                            else -> {
                                println("Unsupported location type: ${location.type} for ${event.eventValues.title}")
                                false
                            }
                        }
                    } ?: run {
                        println("No location data for alert: ${event.eventValues.title}")
                        false
                    }

                    if (isInRange) {
//                        println("Alert in range: ${event.eventValues.title}")
                        AlertItem.fromEvent(event)
                    } else {
//                        println("Alert out of range: ${event.eventValues.title}")
                        null
                    }
                } catch (e: Exception) {
                    println("Error processing event: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }

            val newAlerts = alerts.filter { alert ->
                if (isNewAlert(alert)) {
                    println("Found new alert: ${alert.event}")
                    saveAlertToHistory(alert)
                    true
                } else {
                    println("Skipping existing alert: ${alert.event}")
                    false
                }
            }

//            println("""
//            Alert Processing Results:
//            ------------------------
//            Total events received: ${response.data.events.size}
//            Alerts in range: ${alerts.size}
//            New alerts: ${newAlerts.size}
//            Battery saving mode: $isBatteryLow
//            Attempt number: $runAttemptCount
//            ------------------------
//        """.trimMargin())

            if (newAlerts.isNotEmpty()) {
                processAlerts(newAlerts)
            }

            // Schedule next work based on conditions
            scheduleNextWork(newAlerts.isNotEmpty())

            return Result.success()

        } catch (e: Exception) {
            println("WeatherAlertWorker: Critical error: ${e.message}")
            e.printStackTrace()
            return if (isRecoverableError(e)) {
                println("WeatherAlertWorker: Scheduling retry for recoverable error")
                Result.retry()
            } else {
                println("WeatherAlertWorker: Non-recoverable error")
                Result.failure()
            }
        }
    }

    // Validation check if the location is within acceptable bounds
    private fun isLocationValid(lat: Double, lon: Double): Boolean {
        return lat != 0.0 && lon != 0.0 &&
                lat >= -90 && lat <= 90 &&
                lon >= -180 && lon <= 180
    }

    private fun isRecoverableError(error: Exception): Boolean {
        return error is java.io.IOException ||
                error is retrofit2.HttpException ||
                error is java.net.SocketTimeoutException
    }

    private fun clearExpiredAlerts() {
        val sharedPrefs = applicationContext.getSharedPreferences("alert_history", Context.MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()
        val editor = sharedPrefs.edit()
        var cleared = 0

        sharedPrefs.all.forEach { (key, value) ->
            if (value is Long) {
                // Check if the alert is older than 6 hours
                if (currentTime - value > 6 * 60 * 60 * 1000) {
                    editor.remove(key)
                    cleared++
                }
            }
        }

        if (cleared > 0) {
            editor.apply()
            println("Cleared $cleared expired alerts from history")
        }
    }





    private fun isInRadius(
        userLat: Double,
        userLon: Double,
        alertLat: Double,
        alertLon: Double,
        radiusKm: Float
    ): Boolean {
        val R = 6371 // Earth's radius in kilometers

        val latDistance = Math.toRadians(alertLat - userLat)
        val lonDistance = Math.toRadians(alertLon - userLon)

        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(alertLat)) *
                Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distance = R * c

        return distance <= radiusKm
    }


    // Process and send notifications for new alerts
    private fun processAlerts(alerts: List<AlertItem>) {
        val maxNotifications = 20 // Keep below system limit of 25

        alerts.take(maxNotifications).forEach { alert ->
            try {
                // Check if we've already notified for this alert recently
                if (!hasRecentNotification(alert)) {
                    println("WeatherAlertWorker: Sending notification for: ${alert.event}")
                    sendNotification(applicationContext, alert)
                    saveAlertToHistory(alert)
                    recordNotificationSent(alert)
                    println("WeatherAlertWorker: Successfully processed alert: ${alert.event}")
                } else {
//                    println("WeatherAlertWorker: Skipping duplicate notification for: ${alert.event}")
                }
            } catch (e: Exception) {
                println("WeatherAlertWorker: Error sending notification: ${e.message}")
            }
        }
    }

    private fun hasRecentNotification(alert: AlertItem): Boolean {
        val sharedPrefs = applicationContext.getSharedPreferences("notification_history", Context.MODE_PRIVATE)
        val notificationKey = "${alert.event}_notification"
        val lastNotificationTime = sharedPrefs.getLong(notificationKey, 0)
        val currentTime = System.currentTimeMillis()

        // Check if we've sent a notification for this alert in the last hour
        return (currentTime - lastNotificationTime) < TimeUnit.HOURS.toMillis(1)
    }

    private fun recordNotificationSent(alert: AlertItem) {
        val sharedPrefs = applicationContext.getSharedPreferences("notification_history", Context.MODE_PRIVATE)
        val notificationKey = "${alert.event}_notification"
        sharedPrefs.edit().putLong(notificationKey, System.currentTimeMillis()).apply()
    }

    private fun scheduleNextWork(alertsFound: Boolean) {
        val delay = if (alertsFound) {
            NORMAL_INTERVAL
        } else {
            BATTERY_SAVING_INTERVAL
        }

        val workRequest = OneTimeWorkRequestBuilder<WeatherAlertWorker>()
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .setConstraints(getWorkerConstraints())
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }


    private fun scheduleDelayedWork(delayMinutes: Int) {
        val workRequest = OneTimeWorkRequestBuilder<WeatherAlertWorker>()
            .setInitialDelay(delayMinutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(getWorkerConstraints())
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }

    private fun getWorkerConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private fun isNewAlert(alert: AlertItem): Boolean {
        val sharedPrefs = applicationContext.getSharedPreferences("alert_history", Context.MODE_PRIVATE)
        val alertKey = "${alert.event}_${alert.start}_${alert.end}"
        val exists = sharedPrefs.contains(alertKey)
//        println("Checking alert: $alertKey - Exists in history: $exists")
        return !exists
    }

    private fun saveAlertToHistory(alert: AlertItem) {
        val sharedPrefs = applicationContext.getSharedPreferences("alert_history", Context.MODE_PRIVATE)
        val alertKey = "${alert.event}_${alert.start}_${alert.end}"
        sharedPrefs.edit()
            .putLong(alertKey, System.currentTimeMillis())
            .apply()
        println("Saved new alert to history: $alertKey")
    }

    // Send a notification for a specific alert
    private fun sendNotification(context: Context, alert: AlertItem) {
        val notificationManager = NotificationManagerCompat.from(context)

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for severe weather alerts"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle(alert.event)
            .setContentText(alert.description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(alert.description))

            // Add severity color
            .setColor(when (alert.tags.firstOrNull()?.lowercase()) {
                "severe" -> Color.RED
                "moderate" -> Color.YELLOW
                else -> Color.BLUE
            })

            // Add intent to open app when notification is tapped
            .setContentIntent(getPendingIntent(context))
            .build()

        // Show the notification
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(
                alert.hashCode(), // Unique ID for each alert
                notification
            )
        }
    }

    // Helper function to create PendingIntent
    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val WORK_NAME = "WeatherAlertCheck"
        private const val CHANNEL_ID = "weather_alerts"
        private const val DEFAULT_RADIUS_KM = 50f
        private const val KEY_BATTERY_LOW = "key_battery_low"
        private const val NORMAL_INTERVAL = 15L  // 15 minutes
        private const val BATTERY_SAVING_INTERVAL = 30L // 30 minutes
        private const val MINIMUM_WEATHER_CHECK_INTERVAL = 15L // Minimum 15 minutes
        private const val MAX_RETRY_ATTEMPTS = 3

        fun startPeriodicChecks(context: Context, radiusKm: Float = DEFAULT_RADIUS_KM) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<WeatherAlertWorker>(
                MINIMUM_WEATHER_CHECK_INTERVAL,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInputData(workDataOf(
                    "radius_km" to radiusKm
                ))
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicWork
                )

            println("WeatherAlertWorker: Periodic checks scheduled")
        }

        fun stopChecks(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            println("WeatherAlertWorker: Checks stopped")
        }

        // Add method to update check interval
        fun updateCheckInterval(context: Context, intervalMinutes: Long) {
            // Ensure minimum interval
            val safeInterval = maxOf(intervalMinutes, MINIMUM_WEATHER_CHECK_INTERVAL)

            val newWork = PeriodicWorkRequestBuilder<WeatherAlertWorker>(
                safeInterval,
                TimeUnit.MINUTES
            )
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    newWork
                )
        }

        fun startImmediateCheck(context: Context, radiusKm: Float = DEFAULT_RADIUS_KM) {
            println("WeatherAlertWorker: [DEBUG] Scheduling immediate check")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val immediateWork = OneTimeWorkRequestBuilder<WeatherAlertWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(
                    "radius_km" to radiusKm
                ))
                .build()

            val workRequest = OneTimeWorkRequestBuilder<WeatherAlertWorker>()
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

            println("WeatherAlertWorker: [DEBUG] Immediate check scheduled")
        }
        }
    }