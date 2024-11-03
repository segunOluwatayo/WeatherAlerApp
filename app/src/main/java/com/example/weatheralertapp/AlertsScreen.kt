package com.example.weatheralertapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Composable function that represents the Alerts screen, displaying a list of weather alerts
@Composable
fun AlertsScreen() {
    // Static list of sample alerts; will be replaced with dynamic data in the either milestone 1 or 2
    val alerts = listOf(
        AlertItem(
            title = "Storm Warning",
            details = "Severe storm expected from 3 PM to 6 PM."
        ),
        AlertItem(
            title = "Heatwave Alert",
            details = "High temperatures expected for the next 48 hours."
        ),
        AlertItem(
            title = "Flood Warning",
            details = "Flooding possible in low-lying areas due to heavy rains."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Active Weather Alerts",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Iterates through each alert and displays it in an AlertCard composable
        alerts.forEach { alert ->
            AlertCard(alert = alert)
        }
    }
}

// Composable function to display a single alert card with title and details
@Composable
fun AlertCard(alert: AlertItem) {
    // Card configuration with background color, shape, and clickable behavior
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xffd84e5d)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                // Handle click to show more details Should be done in milestone 2
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Displays detailed information about the alert
            Text(
                text = alert.details,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Data class to represent an alert item with title and details
data class AlertItem(
    val title: String,
    val details: String
)
