package com.example.weatheralertapp.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.*
import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import androidx.work.await
import com.example.weatheralertapp.R
import com.example.weatheralertapp.worker.WeatherAlertWorker
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import com.example.weatheralertapp.weatherapi.WeatherCodeUtil
import com.example.weatheralertapp.weatherapi.WeatherService
import com.example.weatheralertapp.weatherapi.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// Data class for representing the state of the location
data class LocationState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val cityName: String = "",
    val country: String = "",
    val formattedLocation: String = ""
)

// Data class for representing the state of the weather
data class WeatherState(
    val temperature: Int = 0,
    val humidity: Double = 0.0,
    val windSpeed: Double = 0.0,
    val pressure: Double = 0.0,
    val precipitationProbability: Double = 0.0,
    val weatherCode: Int = 1000,
    val weatherDescription: String = "",
    val locationName: String = "",
    val country: String = "",
    val localSensorData: LocalSensorData = LocalSensorData()
)

data class LocalSensorData(
    val temperature: Float = 0f,
    val pressure: Float = 0f,
    val humidity: Float = 0f,
    val pressureTrend: PressureTrend = PressureTrend.STABLE,
    val lastUpdated: Long = 0L,
    val isAvailable: Boolean = false,
    val hasTemperature: Boolean = false,
    val hasHumidity: Boolean = false,
    val hasPressure: Boolean = false
)

enum class PressureTrend {
    FALLING_FAST, FALLING, STABLE, RISING, RISING_FAST
}

// ViewModel for managing home screen data and functionality
class HomeViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Added a MutableStateFlow for refresh state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Retrofit instance for API calls
    private val retrofit = Retrofit.Builder()
        .baseUrl(Constants.TOMORROW_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val weatherService = retrofit.create(WeatherService::class.java)
    private val geocoder = Geocoder(application, Locale.getDefault())

    // Mutable state flow for the current location
    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState

    // Mutable state flow for the current weather
    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState

    // Mutable state flow for saved locations
    private val _savedLocations = MutableStateFlow<List<LocationState>>(emptyList())
    val savedLocations: StateFlow<List<LocationState>> = _savedLocations.asStateFlow()

    private val sensorManager: SensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var pressureSensor: Sensor? = null
    private var temperatureSensor: Sensor? = null
    private var humiditySensor: Sensor? = null

    // Keep track of pressure readings for trend analysis
    private val pressureReadings = mutableListOf<PressureReading>()
    private data class PressureReading(val value: Float, val timestamp: Long)

    private val gson = Gson()

    init {
        initializeSensors()
        loadLocationsFromPrefs()
    }

    private fun initializeSensors() {
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)

        println("HomeViewModel: Available sensors:") // Debug log
        println("Pressure: ${pressureSensor != null}")
        println("Temperature: ${temperatureSensor != null}")
        println("Humidity: ${humiditySensor != null}")

        // Register available sensors with a higher sampling rate
        pressureSensor?.let {
            val success = sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
            println("Pressure sensor registration: $success") // Debug log
        }
        temperatureSensor?.let {
            val success = sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
            println("Temperature sensor registration: $success") // Debug log
        }
        humiditySensor?.let {
            val success = sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
            println("Humidity sensor registration: $success") // Debug log
        }

        val sensorsAvailable = pressureSensor != null || temperatureSensor != null || humiditySensor != null
        _weatherState.update { current ->
            current.copy(
                localSensorData = LocalSensorData(
                    isAvailable = sensorsAvailable,
                    pressure = current.localSensorData.pressure,
                    temperature = current.localSensorData.temperature,
                    humidity = current.localSensorData.humidity,
                    pressureTrend = current.localSensorData.pressureTrend,
                    lastUpdated = current.localSensorData.lastUpdated
                )
            )
        }
        println("HomeViewModel: Sensors available set to $sensorsAvailable") // Debug log

        // Update weather state with sensor availability
        _weatherState.update { current ->
            current.copy(
                localSensorData = current.localSensorData.copy(
                    isAvailable = pressureSensor != null || temperatureSensor != null || humiditySensor != null,
                    hasPressure = pressureSensor != null,
                    hasTemperature = temperatureSensor != null,
                    hasHumidity = humiditySensor != null
                )
            )
        }
    }
    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        viewModelScope.launch(Dispatchers.Default) {  // Process sensor data on background thread
            event?.let { sensorEvent ->
                val sensorValue = sensorEvent.values[0]

                when (sensorEvent.sensor.type) {
                    Sensor.TYPE_PRESSURE -> {
                        withContext(Dispatchers.Main) {
                            handlePressureReading(sensorValue)
                        }
                    }
                    Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                        println("Temperature sensor reading: $sensorValue") // Debug log
                        withContext(Dispatchers.Main) {
                            handleTemperatureReading(sensorValue)
                        }
                    }
                    Sensor.TYPE_RELATIVE_HUMIDITY -> {
                        println("Humidity sensor reading: $sensorValue") // Debug log
                        withContext(Dispatchers.Main) {
                            handleHumidityReading(sensorValue)
                        }
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

    private fun handlePressureReading(pressure: Float) {
        _weatherState.update { current ->
            current.copy(
                localSensorData = current.localSensorData.copy(
                    pressure = pressure,
                    pressureTrend = calculatePressureTrend(),
                    lastUpdated = System.currentTimeMillis(),
                    hasPressure = true,
                    isAvailable = true
                )
            )
        }
    }

    private fun handleTemperatureReading(temperature: Float) {
        _weatherState.update { current ->
            current.copy(
                localSensorData = current.localSensorData.copy(
                    temperature = temperature,
                    lastUpdated = System.currentTimeMillis(),
                    hasTemperature = true,
                    isAvailable = true
                )
            )
        }
    }

    private fun handleHumidityReading(humidity: Float) {
        _weatherState.update { current ->
            current.copy(
                localSensorData = current.localSensorData.copy(
                    humidity = humidity,
                    lastUpdated = System.currentTimeMillis(),
                    hasHumidity = true,
                    isAvailable = true
                )
            )
        }
    }

    private fun calculatePressureTrend(): PressureTrend {
        if (pressureReadings.size < 2) return PressureTrend.STABLE

        val pressureChange = pressureReadings.last().value - pressureReadings.first().value
        return when {
            pressureChange < -2.0f -> PressureTrend.FALLING_FAST
            pressureChange < -0.5f -> PressureTrend.FALLING
            pressureChange > 2.0f -> PressureTrend.RISING_FAST
            pressureChange > 0.5f -> PressureTrend.RISING
            else -> PressureTrend.STABLE
        }
    }

    // Fetch the current location using the fused location provider
    fun fetchLocation(context: Context) {
        viewModelScope.launch {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            // Check for location permissions
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@launch
            }

            // Get the last known location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        updateLocationState(it.latitude, it.longitude)
                    }
                }
        }
    }

    // Update the location state and fetch weather data for the given coordinates
    private fun updateLocationState(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                // Save location for background worker
                getApplication<Application>().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putFloat("last_latitude", latitude.toFloat())
                    .putFloat("last_longitude", longitude.toFloat())
                    .apply()

                println("HomeViewModel: Saved location for background worker - lat: $latitude, lon: $longitude")

                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()

                // Enhanced city name resolution
                val cityName = address?.let { addr ->
                    when {
                        // Try to get the most specific name without "County" prefix
                        !addr.locality.isNullOrBlank() -> addr.locality
                        !addr.subAdminArea.isNullOrBlank() -> addr.subAdminArea.removePrefix("County ").trim()
                        !addr.adminArea.isNullOrBlank() -> addr.adminArea.removePrefix("County ").trim()
                        else -> "Unknown Location"
                    }
                } ?: "Unknown Location"

                val country = address?.countryName ?: "Unknown Country"

                println("HomeViewModel: Resolved address - City: $cityName, Country: $country")

                _locationState.value = LocationState(
                    latitude = latitude,
                    longitude = longitude,
                    cityName = cityName,
                    country = country,
                    formattedLocation = "$latitude,$longitude"
                )
                fetchWeatherData(latitude, longitude)
            } catch (e: Exception) {
                println("HomeViewModel: Error updating location state: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Fetch weather data for the given location coordinates
    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val location = "$latitude,$longitude"
                val response = weatherService.getCurrentWeather(location = location)

                // Keep existing sensor data when updating weather state
                _weatherState.update { current ->
                    current.copy(
                        temperature = response.data.values.temperature.toInt(),
                        humidity = response.data.values.humidity,
                        windSpeed = response.data.values.windSpeed,
                        pressure = response.data.values.pressureSurfaceLevel,
                        precipitationProbability = response.data.values.precipitationProbability,
                        weatherCode = response.data.values.weatherCode,
                        weatherDescription = WeatherCodeUtil.getWeatherDescription(response.data.values.weatherCode),
                        locationName = _locationState.value.cityName,
                        country = _locationState.value.country,
                        // Preserve the existing sensor data
                        localSensorData = current.localSensorData
                    )
                }
            } catch (e: Exception) {
                println("Error fetching weather data: ${e.message}")
            }
        }
    }

    // Search for a location by name and update the state
    fun searchLocation(cityName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val addresses = geocoder.getFromLocationName(cityName, 1)
                addresses?.firstOrNull()?.let { address ->
                    updateLocationState(address.latitude, address.longitude)
                }
            } catch (e: Exception) {
                println("Error searching location: ${e.message}")
            }  finally {
                _isLoading.value = false
            }
        }
    }

    // Trigger a notification for significant pressure changes
    @SuppressLint("MissingPermission")
    private fun triggerPressureAlert(pressureChange: Float) {
        val context = getApplication<Application>().applicationContext

        val notificationId = 1
        val channelId = "pressure_alerts"

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle("Pressure Alert")
            .setContentText("Significant pressure change detected: ${String.format("%.1f", pressureChange)} hPa")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pressure Alerts"
            val descriptionText = "Notifications for significant pressure changes"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    // Save the current location to the list of saved locations and update SharedPreferences
    fun saveCurrentLocation() {
        val currentLocation = _locationState.value
        if (currentLocation.latitude != 0.0 && currentLocation.longitude != 0.0) {
            if (!_savedLocations.value.any { it.formattedLocation == currentLocation.formattedLocation }) {
                _savedLocations.value += currentLocation
                saveLocationsToPrefs()
            } else {
                // Show a Toast message indicating that the location is already saved
                Toast.makeText(
                    getApplication(),
                    "Location already saved",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Delete a location from the list of saved locations and update SharedPreferences
    fun deleteLocation(location: LocationState) {
        _savedLocations.value = _savedLocations.value.toMutableList().also {
            it.remove(location)
        }
        saveLocationsToPrefs()
    }

    // Save locations to SharedPreferences
    private fun saveLocationsToPrefs() {
        val json = gson.toJson(_savedLocations.value)
        getApplication<Application>().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("saved_locations", json)
            .apply()
    }

    // Load locations from SharedPreferences
    private fun loadLocationsFromPrefs() {
        val prefs = getApplication<Application>().getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("saved_locations", null)
        if (json != null) {
            val type = object : TypeToken<List<LocationState>>() {}.type
            _savedLocations.value = gson.fromJson(json, type)
        }
    }

    // Set the current location and fetch weather data for it
    fun setCurrentLocation(location: LocationState) {
        _locationState.value = location
        fetchWeatherData(location.latitude, location.longitude)
    }

    @SuppressLint("RestrictedApi")
    fun checkWorkerStatus() {
        viewModelScope.launch {
            try {
                val workInfos = WorkManager.getInstance(getApplication())
                    .getWorkInfosForUniqueWork(WeatherAlertWorker.WORK_NAME)
                    .await()

                if (workInfos.isEmpty()) {
                    println("No workers found")
                } else {
                    workInfos.forEach { workInfo ->
                        println("Worker Status: ${workInfo.state}")
                        println("Worker Tags: ${workInfo.tags}")
                        println("Worker Run Attempt Count: ${workInfo.runAttemptCount}")
                    }
                }
            } catch (e: Exception) {
                println("Error checking worker status: ${e.message}")
            }
        }
    }
    // Functions to set the refresh state
    fun refreshData(context: Context) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Fetch the latest location and weather data
                fetchLocation(context)
                delay(500)
            } catch (e: Exception) {
                // Handle any errors if necessary
                println("Error refreshing data: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    companion object {
        private const val PRESSURE_THRESHOLD = 2.0f // Threshold for triggering a pressure alert
    }
}