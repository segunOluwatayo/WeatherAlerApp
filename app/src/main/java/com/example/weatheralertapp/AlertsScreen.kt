package com.example.weatheralertapp
import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatheralertapp.com.example.weatheralertapp.AlertItem
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.util.Locale

// The main screen for displaying weather alerts
@Composable
fun AlertsScreen() {
    val context = LocalContext.current
    val viewModel: AlertsViewModel = viewModel(
        // Using a ViewModel factory for creating the AlertsViewModel
        factory = AlertsViewModelFactory(context.applicationContext as Application)
    )
    val uiState by viewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Automatically fetches the current location when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.fetchCurrentLocation(context)
    }

    // Main layout for the screen
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Search and location management section at the top
        Column(
            modifier = Modifier
                .fillMaxWidth() // Use full width
                .padding(16.dp) // Apply padding around the content
        ) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Location") },
                modifier = Modifier.fillMaxWidth()
            )


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { viewModel.searchLocation(searchQuery) }
                ) {
                    Text("Search")
                }

                Button(
                    onClick = { viewModel.saveCurrentLocation() }
                ) {
                    Text("Save Location")
                }
            }

            // Display the current location
            Text(
                text = "Current Location: ${uiState.currentLocation.cityName.ifEmpty {
                    "${uiState.currentLocation.latitude}, ${uiState.currentLocation.longitude}"
                }}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Scrollable content section for displaying alerts and saved locations
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            // Display saved locations if available
            if (uiState.savedLocations.isNotEmpty()) {
                item {
                    Text(
                        text = "Saved Locations",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(uiState.savedLocations) { location ->
                    Text(
                        text = location.cityName.ifEmpty {
                            "${location.latitude}, ${location.longitude}"
                        },
                        modifier = Modifier
                            .clickable {
                                viewModel.fetchAlertsForLocation(
                                    location.latitude,
                                    location.longitude
                                )
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            // Display the active weather alerts section
            item {
                Text(
                    text = "Active Weather Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Display different content based on the UI state
            when {
                uiState.isLoading -> {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.Center)
                        )
                    }
                }
                uiState.error != null -> {
                    item {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                uiState.alerts.isEmpty() -> {
                    // Show a message if there are no alerts
                    item {
                        Text("No active alerts for this location")
                    }
                }
                else -> {
                    // Display the alerts
                    items(uiState.alerts) { alert ->
                        AlertCard(alert = alert)
                    }
                }
            }
        }
    }
}

// A composable for displaying individual weather alerts
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertCard(alert: AlertItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alert.event,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Show less" else "Show more",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Text(
                text = "From: ${formatTimestamp(alert.start)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )

            // Display alert tags
            if (alert.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    alert.tags.forEach { tag ->
                        AlertTag(tag = tag)
                    }
                }
            }

            // Expandable content with additional details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Until: ${formatTimestamp(alert.end)}", // End time
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )

                    if (alert.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = alert.description, // Alert description
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

// A composable for displaying alert tags with specific styles
@Composable
private fun AlertTag(tag: String) {
    val (backgroundColor, textColor) = when (tag.lowercase(Locale.getDefault())) {
        // Map different tag types to colors and labels
        "minor" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "moderate" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "severe" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "expected" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "immediate" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "likely" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "possible" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

// Factory for creating the ViewModel with an Application context
class AlertsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlertsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Utility function for formatting timestamps into human-readable dates
private fun formatTimestamp(timestamp: Long): String {
    return java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        .format(java.util.Date(timestamp))
}
