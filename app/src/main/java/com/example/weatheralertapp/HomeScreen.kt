package com.example.weatheralertapp

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context.applicationContext as Application)
    )

    var permissionsGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.all { it.value }
        if (permissionsGranted) {
            viewModel.fetchLocation(context)
        }
    }

    val hasFineLocationPermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasCoarseLocationPermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

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
                WeatherContent(viewModel, Modifier.padding(padding))
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

@Composable
fun WeatherContent(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    HomeContent(viewModel, LocalContext.current, modifier)
}

@Composable
fun HomeContent(
    viewModel: HomeViewModel,
    context: Context,
    modifier: Modifier = Modifier
) {
    val locationState by viewModel.locationState.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val savedLocations by viewModel.savedLocations.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                        .padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { viewModel.searchLocation(searchQuery) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
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
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Location")
                    }
                }

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

                Text(
                    text = "Saved Locations",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            items(savedLocations) { location ->
                SavedLocationItem(location) {
                    viewModel.setCurrentLocation(location)
                }
            }
        }
    }
}

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

private fun getPressureTrendText(trend: PressureTrend): String {
    return when (trend) {
        PressureTrend.FALLING_FAST -> "Rapidly Falling"
        PressureTrend.FALLING -> "Falling"
        PressureTrend.STABLE -> "Stable"
        PressureTrend.RISING -> "Rising"
        PressureTrend.RISING_FAST -> "Rapidly Rising"}
}

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

@Composable
fun SavedLocationItem(location: LocationState, onClick: () -> Unit) {
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    }
}

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

@DrawableRes
fun getWeatherIcon(weatherCode: Int): Int {
    return when (weatherCode) {
        1000 -> R.drawable.ic_sunny
        1001 -> R.drawable.ic_cloudy
        1002 -> R.drawable.ic_rainy
        1003 -> R.drawable.ic_drizzle
        1004 -> R.drawable.ic_thunderstorm
        1005 -> R.drawable.ic_snow
        1006 -> R.drawable.ic_mist
        else -> R.drawable.ic_unknown
    }
}

class HomeViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
