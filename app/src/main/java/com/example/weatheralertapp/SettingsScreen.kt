package com.example.weatheralertapp

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

// Composable function representing the Settings screen where users can manage their preferences
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current

    // Observes changes in user preferences from the ViewModel
    val preferences by viewModel.preferences.collectAsState(initial = UserPreferences())

    Scaffold { padding ->
        SettingsContent(preferences, viewModel, context, Modifier.padding(padding))
    }
}

// Composable function to display settings content, allowing users to select weather alerts and toggle notifications
@Composable
fun SettingsContent(
    preferences: UserPreferences,
    viewModel: SettingsViewModel,
    context: Context,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Select Alerts to Receive:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Individual checkboxes for different alert types, allowing users to enable or disable each alert
        CheckboxItem(
            label = "Storms",
            checked = preferences.storms,
            onCheckedChange = { viewModel.updatePreferences(storms = it) }
        )
        CheckboxItem(
            label = "Heatwaves",
            checked = preferences.heatwaves,
            onCheckedChange = { viewModel.updatePreferences(heatwaves = it) }
        )
        CheckboxItem(
            label = "Floods",
            checked = preferences.floods,
            onCheckedChange = { viewModel.updatePreferences(floods = it) }
        )
        CheckboxItem(
            label = "Cold Snaps",
            checked = preferences.coldSnaps,
            onCheckedChange = { viewModel.updatePreferences(coldSnaps = it) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Enable Location-Based Notifications:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Switch item to toggle location-based notifications
        SwitchItem(
            checked = preferences.notificationsEnabled,
            onCheckedChange = { viewModel.updatePreferences(notificationsEnabled = it) }
        )
    }
}

// Composable function to display a labeled checkbox item for each type of alert
@Composable
fun CheckboxItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Checkbox component for selecting alert preferences
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// Composable function to display a switch for enabling or disabling notifications
@Composable
fun SwitchItem(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Notifications", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
