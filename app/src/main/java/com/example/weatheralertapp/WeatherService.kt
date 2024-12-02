
package com.example.weatheralertapp.com.example.weatheralertapp



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
        @Query("buffer") buffer: Double = 1.0,
        @Query("apikey") apikey: String = Constants.TOMORROW_API_KEY
    ): AlertsResponse

}

// Helper function to create proper GeoJSON Point
fun createLocationGeoJson(latitude: Double, longitude: Double): String {
    return """
        {
            "type": "Point",
            "coordinates": [$longitude, $latitude]
        }
    """.trimIndent()
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
    val tags: List<String>,
    val distance: Double?,
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
                    .filter { it.isNotBlank() && it.toLowerCase() != "unknown" },
                distance = event.eventValues.distance  // Get distance from eventValues
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