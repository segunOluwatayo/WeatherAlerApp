//package com.example.weatheralertapp.com.example.weatheralertapp
//
//import retrofit2.http.GET
//import retrofit2.http.Query
//
//// Interface for Weather API calls
//interface WeatherService {
//    // Retrieval of the current weather data for a specific location
//    @GET("data/2.5/weather")
//    suspend fun getCurrentWeather(
//        @Query("lat") lat: Double,
//        @Query("lon") lon: Double,
//        @Query("appid") appid: String,
//        @Query("units") units: String = "metric"
//    ): WeatherResponse
//
//    // Retrieval of the geolocation data based on a city name
//    @GET("geo/1.0/direct")
//    suspend fun getLocationFromCityName(
//        @Query("q") cityName: String,
//        @Query("limit") limit: Int = 1,
//        @Query("appid") apiKey: String
//    ): List<GeocodingResponse>
//
//    // Retrieveal of weather alerts for a specific location
//    @GET("data/2.5/onecall")
//    suspend fun getWeatherAlerts(
//        @Query("lat") lat: Double,
//        @Query("lon") lon: Double,
//        @Query("exclude") exclude: String = "current,minutely,hourly,daily",
//        @Query("appid") apiKey: String
//    ): AlertsResponse
//}
//
//data class Sys(
//    val country: String
//)
//
//// The data model which will represent the weather response for current weather
//data class WeatherResponse(
//    val main: Main,
//    val wind: Wind,
//    val weather: List<Weather>,
//    val name: String,
//    val sys: Sys
//)
//
//// The sub-model which will represent the main weather attributes in the current weather response
//data class Main(
//    val temp: Double,
//    val humidity: Int,
//    val pressure: Double
//)
//
//// The sub-model which will represent the wind attributes in the current weather response
//data class Wind(
//    val speed: Double
//)
//
//// The sub-model which will representing the weather
//data class Weather(
//    val description: String,
//    val icon: String
//)
//
//// Data model for geolocation lookup response based on city name
//data class GeocodingResponse(
//    val name: String,
//    val lat: Double,
//    val lon: Double,
//    val country: String
//)
//
//// Data model representing the weather alerts response
//data class AlertsResponse(
//    val alerts: List<AlertItem>?
//)
//
//// Data model for an individual weather alert item
//data class AlertItem(
//    val sender_name: String,
//    val event: String,
//    val start: Long,
//    val end: Long,
//    val description: String,
//    val tags: List<String>
//)
//
package com.example.weatheralertapp.com.example.weatheralertapp

//import retrofit2.http.GET
//import retrofit2.http.Query
//import com.example.weatheralertapp.Constants
//import com.example.weatheralertapp.LocationState
//
//interface WeatherService {
//    @GET("weather/realtime")
//    suspend fun getCurrentWeather(
//        @Query("location") location: String,
//        @Query("units") units: String = "metric",
//        @Query("apikey") apikey: String = Constants.TOMORROW_API_KEY
//    ): WeatherResponse
//
//    @GET("weather/forecast")
//    suspend fun getWeatherForecast(
//        @Query("location") location: String,
//        @Query("timesteps") timesteps: String = "1h",
//        @Query("units") units: String = "metric",
//        @Query("apikey") apikey: String = Constants.TOMORROW_API_KEY
//    ): ForecastResponse
//
//    // Updated alerts endpoint to match Tomorrow.io's structure
//    @GET("map/alerts")
//    suspend fun getWeatherAlerts(
//        @Query("location") location: String,
//        @Query("apikey") apikey: String = Constants.TOMORROW_API_KEY,
//        @Query("timestamp") timestamp: String = "now"
//    ): AlertsResponse
//}
//
//// Main Weather Response
//data class WeatherResponse(
//    val data: WeatherData,
//    val location: Location
//)
//
//data class WeatherData(
//    val time: String,
//    val values: WeatherValues
//)
//
//data class WeatherValues(
//    val temperature: Double,
//    val humidity: Double,
//    val windSpeed: Double,
//    val pressureSurfaceLevel: Double,
//    val precipitationProbability: Double,
//    val weatherCode: Int
//)
//
//// Location Data
//data class Location(
//    val lat: Double,
//    val lon: Double,
//    val name: String?,
//    val type: String
//)
//
//// Forecast Response
//data class ForecastResponse(
//    val timelines: Timelines,
//    val location: Location
//)
//
//data class Timelines(
//    val hourly: List<TimelineData>
//)
//
//data class TimelineData(
//    val time: String,
//    val values: WeatherValues
//)
//data class AlertItem(
//    val sender_name: String,
//    val event: String,
//    val start: Long,
//    val end: Long,
//    val description: String,
//    val tags: List<String>
//)
//
//data class AlertsUiState(
//    val currentLocation: LocationState = LocationState(),
//    val alerts: List<AlertItem> = emptyList(),
//    val savedLocations: List<LocationState> = emptyList(),
//    val isLoading: Boolean = false,
//    val error: String? = null
//)
//
//
//// Alerts Response
//data class AlertsResponse(
//    val data: AlertsData
//)
//
//data class AlertsData(
//    val events: List<AlertEvent>
//)
//
//data class AlertEvent(
//    val title: String,
//    val description: String,
//    val onset: String,
//    val ends: String,
//    val severity: String = "",
//    val urgency: String = "",
//    val source: String = "",
//    val id: String
//)
//
//// Weather Code Utility
//object WeatherCodeUtil {
//    fun getWeatherDescription(code: Int): String {
//        return when (code) {
//            1000 -> "Clear, Sunny"
//            1100 -> "Mostly Clear"
//            1101 -> "Partly Cloudy"
//            1102 -> "Mostly Cloudy"
//            1001 -> "Cloudy"
//            2000 -> "Fog"
//            2100 -> "Light Fog"
//            4000 -> "Drizzle"
//            4001 -> "Rain"
//            4200 -> "Light Rain"
//            4201 -> "Heavy Rain"
//            5000 -> "Snow"
//            5001 -> "Flurries"
//            5100 -> "Light Snow"
//            5101 -> "Heavy Snow"
//            6000 -> "Freezing Drizzle"
//            6001 -> "Freezing Rain"
//            6200 -> "Light Freezing Rain"
//            6201 -> "Heavy Freezing Rain"
//            7000 -> "Ice Pellets"
//            7101 -> "Heavy Ice Pellets"
//            7102 -> "Light Ice Pellets"
//            8000 -> "Thunderstorm"
//            else -> "Unknown"
//        }
//    }
//
//    fun getWeatherIcon(code: Int): String {
//        return when (code) {
//            1000 -> "ic_clear"
//            1100, 1101 -> "ic_partly_cloudy"
//            1102, 1001 -> "ic_cloudy"
//            2000, 2100 -> "ic_fog"
//            4000, 4200 -> "ic_light_rain"
//            4001, 4201 -> "ic_rain"
//            5000, 5001, 5100, 5101 -> "ic_snow"
//            6000, 6001, 6200, 6201 -> "ic_freezing_rain"
//            7000, 7101, 7102 -> "ic_sleet"
//            8000 -> "ic_thunderstorm"
//            else -> "ic_unknown"
//        }
//    }
//}

import android.annotation.SuppressLint
import com.example.weatheralertapp.Constants
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("weather/realtime")
    suspend fun getCurrentWeather(
        @Query("location") location: String,
        @Query("units") units: String = "metric",
        @Query("apikey") apikey: String = Constants.TOMORROW_API_KEY
    ): WeatherResponse

    @GET("events")
    suspend fun getWeatherAlerts(
        @Query("location") location: String,  // Will be formatted as "lat,lon"
        @Query("insights") insights: List<String> = listOf("fires", "wind", "winter", "thunderstorms", "floods", "temperature", "tropical", "marine", "fog", "tornado"),
        @Query("buffer") buffer: Float = 1f,
        @Query("apikey") apikey: String = Constants.TOMORROW_API_KEY
    ): AlertsResponse

}

// Update the response data classes to match the events API response
data class AlertsResponse(
    val data: EventsData
)

data class EventsData(
    val events: List<Event>
)

data class Event(
    val insight: String,
    val startTime: String,
    val endTime: String,
    val updateTime: String?,
    val severity: String,
    val certainty: String,
    val urgency: String,
    val eventValues: EventValues,
    val triggerValues: Map<String, Any>? = null
)

data class EventValues(
    val origin: String?,
    val title: String,
    val description: String?,
    val instruction: String?,
    val location: GeoJsonLocation?,
    val distance: Double?,
    val direction: Double?
)

data class GeoJsonLocation(
    val type: String,
    // Changed to handle nested arrays of coordinates
    val coordinates: Any  // This will handle both single points and polygon coordinates
)

data class AlertItem(
    val sender_name: String,
    val event: String,
    val start: Long,
    val end: Long,
    val description: String,
    val tags: List<String>
) {
    companion object {
        fun fromEvent(event: Event): AlertItem {
            return AlertItem(
                sender_name = event.eventValues.origin ?: "Tomorrow.io",
                event = event.eventValues.title,
                start = event.startTime.toEpochMillis(),
                end = event.endTime.toEpochMillis(),
                description = event.eventValues.description ?: "",
                tags = listOfNotNull(event.severity, event.urgency, event.certainty)
                    .filter { it.isNotBlank() && it.toLowerCase() != "unknown" }
            )
        }
    }
}


// Response Data Classes
data class WeatherResponse(
    val data: WeatherData,
    val location: Location
)

data class WeatherData(
    val time: String,
    val values: WeatherValues
)

data class WeatherValues(
    val temperature: Double,
    val humidity: Double,
    val windSpeed: Double,
    val pressureSurfaceLevel: Double,
    val precipitationProbability: Double,
    val weatherCode: Int
)

data class Location(
    val lat: Double,
    val lon: Double,
    val name: String?,
    val type: String
)

//data class AlertsResponse(
//    val data: AlertsData
//)

data class AlertsData(
    val events: List<AlertEvent>
)

data class AlertEvent(
    val title: String,
    val description: String,
    val onset: String,
    val ends: String,
    val severity: String = "",
    val urgency: String = "",
    val source: String = "",
    val id: String
)

//data class AlertItem(
//    val sender_name: String,
//    val event: String,
//    val start: Long,
//    val end: Long,
//    val description: String,
//    val tags: List<String>
//)

object WeatherCodeUtil {
    fun getWeatherDescription(code: Int): String {
        return when (code) {
            1000 -> "Clear, Sunny"
            1100 -> "Mostly Clear"
            1101 -> "Partly Cloudy"
            1102 -> "Mostly Cloudy"
            1001 -> "Cloudy"
            2000 -> "Fog"
            2100 -> "Light Fog"
            4000 -> "Drizzle"
            4001 -> "Rain"
            4200 -> "Light Rain"
            4201 -> "Heavy Rain"
            5000 -> "Snow"
            5001 -> "Flurries"
            5100 -> "Light Snow"
            5101 -> "Heavy Snow"
            6000 -> "Freezing Drizzle"
            6001 -> "Freezing Rain"
            6200 -> "Light Freezing Rain"
            6201 -> "Heavy Freezing Rain"
            7000 -> "Ice Pellets"
            7101 -> "Heavy Ice Pellets"
            7102 -> "Light Ice Pellets"
            8000 -> "Thunderstorm"
            else -> "Unknown"
        }
    }
}

// Helper function to convert ISO 8601 string to epoch milliseconds
@SuppressLint("NewApi")
private fun String.toEpochMillis(): Long {
    return try {
        java.time.Instant.parse(this).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

