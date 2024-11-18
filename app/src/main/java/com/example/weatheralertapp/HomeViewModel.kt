package com.example.weatheralertapp

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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatheralertapp.com.example.weatheralertapp.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import kotlin.math.abs

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
    val country: String = ""
)

// ViewModel for managing home screen data and functionality
class HomeViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

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

    // SensorManager and pressure sensor for monitoring pressure changes
    private val sensorManager: SensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var pressureSensor: Sensor? = null
    private var lastPressureReading: Float? = null

    init {
        // Register the pressure sensor if available
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Unregister the sensor listener when the ViewModel is cleared
        sensorManager.unregisterListener(this)
    }

    // Handle sensor changes for pressure monitoring
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_PRESSURE) {
                val currentPressure = it.values[0]
                lastPressureReading?.let { lastPressure ->
                    val pressureChange = currentPressure - lastPressure
                    if (abs(pressureChange) > PRESSURE_THRESHOLD) {
                        triggerPressureAlert(pressureChange)
                    }
                }
                lastPressureReading = currentPressure
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
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
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val cityName = addresses?.firstOrNull()?.locality ?: ""
                val country = addresses?.firstOrNull()?.countryName ?: ""

                // Update the location state
                _locationState.value = LocationState(
                    latitude = latitude,
                    longitude = longitude,
                    cityName = cityName,
                    country = country,
                    formattedLocation = "$latitude,$longitude"
                )
                // Fetch weather data for the updated location
                fetchWeatherData(latitude, longitude)
            } catch (e: Exception) {
                println("Error updating location state: ${e.message}")
            }
        }
    }

    // Fetch weather data for the given location coordinates
    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val location = "$latitude,$longitude"
                val response = weatherService.getCurrentWeather(location = location)

                // Update the weather state with API response
                _weatherState.value = WeatherState(
                    temperature = response.data.values.temperature.toInt(),
                    humidity = response.data.values.humidity,
                    windSpeed = response.data.values.windSpeed,
                    pressure = response.data.values.pressureSurfaceLevel,
                    precipitationProbability = response.data.values.precipitationProbability,
                    weatherCode = response.data.values.weatherCode,
                    weatherDescription = WeatherCodeUtil.getWeatherDescription(response.data.values.weatherCode),
                    locationName = _locationState.value.cityName,
                    country = _locationState.value.country
                )
            } catch (e: Exception) {
                println("Error fetching weather data: ${e.message}")
            }
        }
    }

    // Search for a location by name and update the state
    fun searchLocation(cityName: String) {
        viewModelScope.launch {
            try {
                val addresses = geocoder.getFromLocationName(cityName, 1)
                addresses?.firstOrNull()?.let { address ->
                    updateLocationState(address.latitude, address.longitude)
                }
            } catch (e: Exception) {
                println("Error searching location: ${e.message}")
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

    // Save the current location to the list of saved locations
    fun saveCurrentLocation() {
        val currentLocation = _locationState.value
        if (currentLocation.latitude != 0.0 && currentLocation.longitude != 0.0) {
            _savedLocations.value += currentLocation
        }
    }

    // Set the current location and fetch weather data for it
    fun setCurrentLocation(location: LocationState) {
        _locationState.value = location
        fetchWeatherData(location.latitude, location.longitude)
    }

    companion object {
        private const val PRESSURE_THRESHOLD = 2.0f // Threshold for triggering a pressure alert
    }
}
