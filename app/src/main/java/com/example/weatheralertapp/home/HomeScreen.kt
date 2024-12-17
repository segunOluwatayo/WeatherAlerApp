package com.example.weatheralertapp.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatheralertapp.R
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Main composable screen for the weather application.
// Handles location permissions, weather display, and user interactions.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context.applicationContext as Application)
    )

    // Track permission state
    var permissionsGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.all { it.value }
        if (permissionsGranted) {
            viewModel.fetchLocation(context)
        }
    }

    // Check existing permission status
    val hasFineLocationPermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasCoarseLocationPermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // Request permissions on launch if not already granted
    LaunchedEffect(Unit) {
        if (hasFineLocationPermission && hasCoarseLocationPermission) {
            permissionsGranted = true
            viewModel.fetchLocation(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Weather Alert",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        content = { padding ->
            if (permissionsGranted) {
                // Implement SwipeRefresh
                val isRefreshing by viewModel.isRefreshing.collectAsState()

                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing),
                    onRefresh = { viewModel.refreshData(context) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    WeatherContent(viewModel, Modifier.fillMaxSize())
                }
            } else {
                PermissionRequiredContent(
                    isPermissionDenied = !hasFineLocationPermission && !hasCoarseLocationPermission,
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                )
            }
        }
    )
}

// Main weather content wrapper composable.
// Delegates to HomeContent with required context and modifiers.
@Composable
fun WeatherContent(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    HomeContent(viewModel, LocalContext.current, modifier)
}

// Primary content composable for the home screen.
// Displays weather information, search functionality, and saved locations.
@Composable
fun HomeContent(
    viewModel: HomeViewModel,
    context: Context,
    modifier: Modifier = Modifier
) {
    val locationState by viewModel.locationState.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val savedLocations by viewModel.savedLocations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search Location") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotEmpty()) {
                                    viewModel.searchLocation(searchQuery)
                                }
                            }
                        ),
                        enabled = !isLoading && !isRefreshing
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    viewModel.searchLocation(searchQuery)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isLoading && !isRefreshing
                        ) {
                            Text("Search")
                        }

                        Button(
                            onClick = { viewModel.saveCurrentLocation() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isLoading && !isRefreshing
                        ) {
                            Text("Save Location")
                        }
                    }

                    // Show loading indicator when searching or refreshing
                    if (isLoading || isRefreshing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isLoading) "Searching location..." else "Refreshing data...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Current weather display card (only show if not loading or refreshing)
                    if (!isLoading && !isRefreshing) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${weatherState.locationName}, ${weatherState.country}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Icon(
                                    painter = painterResource(id = getWeatherIcon(weatherState.weatherCode)),
                                    contentDescription = weatherState.weatherDescription,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(96.dp)
                                )

                                Text(
                                    text = "${weatherState.temperature}°C",
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                Text(
                                    text = weatherState.weatherDescription,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Divider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    thickness = 1.dp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    WeatherDetailItem(
                                        icon = Icons.Rounded.WaterDrop,
                                        label = "Humidity",
                                        value = "${weatherState.humidity}%"
                                    )
                                    WeatherDetailItem(
                                        icon = Icons.Rounded.Air,
                                        label = "Wind",
                                        value = "${weatherState.windSpeed} km/h"
                                    )
                                    WeatherDetailItem(
                                        icon = Icons.Rounded.Speed,
                                        label = "Pressure",
                                        value = "${weatherState.pressure} hPa"
                                    )
                                }

                                LocalSensorDisplay(weatherState = weatherState)
                            }
                        }
                    }

                    if (!isLoading && !isRefreshing) {
                        Button(
                            onClick = {
                                shareWeatherInfo(
                                    context,
                                    locationState,
                                    weatherState
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Share Weather Info")
                        }
                    }

                    if (!isLoading && !isRefreshing) {
                        Text(
                            text = "Saved Locations",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                }

                items(savedLocations) { location ->
                    SavedLocationItem(
                        location = location,
                        onClick = { viewModel.setCurrentLocation(location) },
                        onDelete = { locationToDelete ->
                            viewModel.deleteLocation(locationToDelete)
                        }
                    )
                }
            }
        }
    }
}

// Loading animation composable.
@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Searching location...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

//Displays environmental sensor data if available on the device.
@Composable
fun LocalSensorDisplay(weatherState: WeatherState) {
    val sensorData = weatherState.localSensorData

    if (sensorData.isAvailable) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            // Sensor data content
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Environmental Data",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (sensorData.hasPressure) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Local Pressure")
                            Text("${sensorData.pressure.roundToInt()} hPa")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pressure Trend")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(getPressureTrendText(sensorData.pressureTrend))
                                Spacer(modifier = Modifier.width(4.dp))
                                getPressureTrendIcon(sensorData.pressureTrend)
                            }
                        }

                        // Weather prediction based on pressure
                        Text(
                            text = getPressurePrediction(sensorData.pressure, sensorData.pressureTrend),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    thickness = 1.dp
                )

                Text(
                    text = "Sensor Last Updated: ${
                        if (sensorData.lastUpdated > 0)
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                .format(Date(sensorData.lastUpdated))
                        else
                            "Never"
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Returns the icon for pressure trend visualization.
@Composable
private fun getPressureTrendIcon(trend: PressureTrend) {
    val icon = when (trend) {
        PressureTrend.FALLING_FAST -> Icons.Rounded.ArrowDownward
        PressureTrend.FALLING -> Icons.Rounded.KeyboardArrowDown
        PressureTrend.STABLE -> Icons.Rounded.Remove
        PressureTrend.RISING -> Icons.Rounded.KeyboardArrowUp
        PressureTrend.RISING_FAST -> Icons.Rounded.ArrowUpward
    }

    val tint = when (trend) {
        PressureTrend.FALLING_FAST -> MaterialTheme.colorScheme.error
        PressureTrend.FALLING -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        PressureTrend.STABLE -> MaterialTheme.colorScheme.onSurfaceVariant
        PressureTrend.RISING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        PressureTrend.RISING_FAST -> MaterialTheme.colorScheme.primary
    }

    Icon(
        imageVector = icon,
        contentDescription = "Pressure trend ${trend.name}",
        tint = tint,
        modifier = Modifier.size(20.dp)
    )
}

// Converts pressure trend to human-readable text.
private fun getPressureTrendText(trend: PressureTrend): String {
    return when (trend) {
        PressureTrend.FALLING_FAST -> "Rapidly Falling"
        PressureTrend.FALLING -> "Falling"
        PressureTrend.STABLE -> "Stable"
        PressureTrend.RISING -> "Rising"
        PressureTrend.RISING_FAST -> "Rapidly Rising"
    }
}

// Provides weather prediction based on pressure and trend.

private fun getPressurePrediction(pressure: Float, trend: PressureTrend): String {
    return when {
        pressure < 1000 && trend == PressureTrend.FALLING_FAST ->
            "⚠️ Stormy conditions likely"
        pressure < 1000 && trend == PressureTrend.FALLING ->
            "Rain or unsettled weather possible"
        pressure > 1020 && trend == PressureTrend.RISING ->
            "Fair weather likely"
        pressure > 1020 && trend == PressureTrend.STABLE ->
            "Continued fair weather"
        trend == PressureTrend.STABLE ->
            "No significant weather changes expected"
        else -> "Weather conditions may be changing"
    }
}

// Reusable composable for displaying weather detail items.
// like for showing humidity, wind speed, and pressure information.
@Composable
fun WeatherDetailItem(icon: ImageVector, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

// Displays a saved location item with click handling and delete functionality.
@Composable
fun SavedLocationItem(location: LocationState, onClick: () -> Unit, onDelete: (LocationState) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${location.cityName}, ${location.country}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            IconButton(onClick = { onDelete(location) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

//Content displayed when permissions are required.
// Shows different UI based on whether permissions are denied or need to be requested.
@Composable
fun PermissionRequiredContent(
    isPermissionDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (isPermissionDenied) {
        PermissionRationale(onRequestPermission)
    } else {
        PermissionDenied(onOpenSettings)
    }
}

// Display rationale for requesting location permission.
@Composable
fun PermissionRationale(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Location permission is needed to show weather information for your current location.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Grant Permission")
        }
    }
}

// Display UI when permissions are permanently denied.
@Composable
fun PermissionDenied(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Location permissions are permanently denied. Please enable them in settings.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = onOpenSettings,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Open Settings")
        }
    }
}

// Shares current weather information using system share intent.
@SuppressLint("QueryPermissionsNeeded")
fun shareWeatherInfo(
    context: Context,
    location: LocationState,
    weather: WeatherState
) {
    val locationText = if (weather.locationName.isNotEmpty()) {
        "${weather.locationName}, ${weather.country}"
    } else {
        "(${location.latitude}, ${location.longitude})"
    }

    val weatherInfo = """
        Current Weather:
        Location: $locationText
        Temperature: ${weather.temperature}°C
        Humidity: ${weather.humidity}%
        Wind Speed: ${weather.windSpeed} km/h
        Pressure: ${weather.pressure} hPa
    """.trimIndent()

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, weatherInfo)
        type = "text/plain"
    }

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

// Maps weather codes to corresponding drawable resources.
@DrawableRes
fun getWeatherIcon(weatherCode: Int): Int {
    return when (weatherCode) {
        // Clear conditions
        1000, 1100 -> R.drawable.ic_sunny  // Clear, Mostly Clear

        // Cloudy conditions
        1001, 1101, 1102 -> R.drawable.ic_cloudy  // Cloudy, Partly Cloudy

        // Rain conditions
        1002, 1192, 1195, 1201, 1180, 1181, 1186, 1189, 1187, 1183, 1198 -> R.drawable.ic_rainy  // Light/Moderate/Heavy Rain

        // Drizzle conditions
        1003, 1150, 1153, 1168, 1171 -> R.drawable.ic_drizzle  // Light/Moderate/Heavy Drizzle

        // Thunderstorm conditions
        1004, 1087, 1273, 1276, 1279, 1282 -> R.drawable.ic_thunderstorm  // Thunderstorm, Thunder

        // Snow conditions
        1005, 1210, 1213, 1216, 1219, 1222, 1225, 1255, 1258 -> R.drawable.ic_snow  // Light/Moderate/Heavy Snow

        // Mist/Fog conditions
        1006, 1030, 1135, 1147 -> R.drawable.ic_mist  // Mist, Fog, Freezing Fog

        // Default for unknown conditions
        else -> R.drawable.ic_unknown
    }
}

// Factory for creating HomeViewModel instances with application context.
class HomeViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}