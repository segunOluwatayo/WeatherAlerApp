package com.example.weatheralertapp

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

// HomeScreen composable is the main UI component for the home page
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchLocation(context)
    }

    Scaffold { padding ->
        HomeContent(viewModel, context, Modifier.padding(padding))
    }
}

// Main content composable for displaying location and weather details
@Composable
fun HomeContent(
    viewModel: HomeViewModel,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    // Observes changes in location and weather state using StateFlow, automatically re-rendering on updates(This is built with milestone 2 in mind)
    val locationState by viewModel.locationState.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display location information
        Text(
            text = "Location: ${locationState.latitude}, ${locationState.longitude}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Display temperature, humidity, wind speed, and precipitation information
        Text(
            text = "Temperature: ${weatherState.temperature}°C",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Humidity: ${weatherState.humidity}%",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Wind Speed: ${weatherState.windSpeed} km/h",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Precipitation: ${weatherState.precipitation}%",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Button to share the weather info, triggering the shareWeatherInfo function
        Button(onClick = {
            shareWeatherInfo(
                context,
                locationState,
                weatherState
            )
        }) {
            Text("Share Weather Info")
        }
    }
}

// Function to handle sharing weather information via an Intent
@SuppressLint("QueryPermissionsNeeded")
fun shareWeatherInfo(
    context: android.content.Context,
    location: LocationState,
    weather: WeatherState
) {
    // Format for the weather information for sharing
    val weatherInfo = """
        Current Weather:
        Location: (${location.latitude}, ${location.longitude})
        Temperature: ${weather.temperature}°C
        Humidity: ${weather.humidity}%
        Wind Speed: ${weather.windSpeed} km/h
        Precipitation: ${weather.precipitation}%
    """.trimIndent()

    // Sets up the intent for sharing the weather info text
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, weatherInfo)
        type = "text/plain"
    }

    // Checks if any app can handle the sharing intent
    if (shareIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(Intent.createChooser(shareIntent, "Share Weather Info via"))
    } else {
        Toast.makeText(
            context,
            "No app available to share weather information.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
