
package com.example.weatheralertapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LocationState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class WeatherState(
    val temperature: Double = 0.0,
    val humidity: Int = 0,
    val windSpeed: Double = 0.0,
    val precipitation: Int = 0
)

class HomeViewModel : ViewModel() {

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState

    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState

    fun fetchLocation(context: Context) {
        viewModelScope.launch {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Handle permission not granted
                return@launch
            }

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        _locationState.value = LocationState(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        // Fetch weather data based on location
                        // For demonstration, using mock data
                        _weatherState.value = WeatherState(
                            temperature = 25.0,
                            humidity = 60,
                            windSpeed = 15.0,
                            precipitation = 20
                        )
                    } else {
                        // Handle location null
                    }
                }
                .addOnFailureListener {
                    // Handle failure
                }
        }
    }
}
