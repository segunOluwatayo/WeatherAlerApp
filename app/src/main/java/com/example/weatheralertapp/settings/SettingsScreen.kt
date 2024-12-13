package com.example.weatheralertapp.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import androidx.compose.foundation.clickable
import com.example.weatheralertapp.theme.ThemeMode
import com.example.weatheralertapp.database.UserPreferences

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }

) {
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()

    LaunchedEffect(saveStatus) {
        when (saveStatus) {
            is SaveStatus.Error -> {
                snackbarHostState.showSnackbar(
                    message = (saveStatus as SaveStatus.Error).message,
                    duration = SnackbarDuration.Short
                )
            }
            is SaveStatus.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Preferences saved successfully",
                    duration = SnackbarDuration.Short
                )
            }
            else -> { /* no-op */ }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        SettingsContent(
            preferences = preferences,
            viewModel = viewModel,
            context = context,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun SettingsContent(
    preferences: UserPreferences,
    viewModel: SettingsViewModel,
    context: Context,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar with Settings title
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(16.dp)
        )

        // Wrap LazyColumn in Box with weight
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Alert Types Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Select Alerts to Receive",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            val alertTypes = listOf(
                                "Fires" to preferences.fires,
                                "Wind" to preferences.wind,
                                "Winter" to preferences.winter,
                                "Thunderstorms" to preferences.thunderstorms,
                                "Floods" to preferences.floods,
                                "Temperature" to preferences.temperature,
                                "Tropical" to preferences.tropical,
                                "Marine" to preferences.marine,
                                "Fog" to preferences.fog,
                                "Tornado" to preferences.tornado
                            )

                            alertTypes.forEach { (label, checked) ->
                                CheckboxItem(
                                    label = label,
                                    checked = checked,
                                    onCheckedChange = {
                                        when (label) {
                                            "Fires" -> viewModel.updatePreferences(fires = it)
                                            "Wind" -> viewModel.updatePreferences(wind = it)
                                            "Winter" -> viewModel.updatePreferences(winter = it)
                                            "Thunderstorms" -> viewModel.updatePreferences(thunderstorms = it)
                                            "Floods" -> viewModel.updatePreferences(floods = it)
                                            "Temperature" -> viewModel.updatePreferences(temperature = it)
                                            "Tropical" -> viewModel.updatePreferences(tropical = it)
                                            "Marine" -> viewModel.updatePreferences(marine = it)
                                            "Fog" -> viewModel.updatePreferences(fog = it)
                                            "Tornado" -> viewModel.updatePreferences(tornado = it)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Notifications Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Notifications",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            SwitchItem(
                                checked = preferences.notificationsEnabled,
                                onCheckedChange = { viewModel.updatePreferences(notificationsEnabled = it) }
                            )
                        }
                    }
                }

                // Theme Selection Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Theme",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            val themeOptions = listOf(
                                ThemeMode.SYSTEM to "System Default",
                                ThemeMode.LIGHT to "Light",
                                ThemeMode.DARK to "Dark"
                            )
                            themeOptions.forEach { (mode, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.updatePreferences(themeMode = mode) }
                                        .padding(vertical = 8.dp)
                                ) {
                                    RadioButton(
                                        selected = preferences.themeMode == mode,
                                        onClick = { viewModel.updatePreferences(themeMode = mode) }
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckboxItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
fun SwitchItem(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val context = LocalContext.current

    // Check system notification permission
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enable Location-Based Notifications",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = notificationPermissionState,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        // Show status message
        if (!notificationPermissionState) {
            Text(
                text = "Please enable notifications in system settings",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else if (checked) {
            Text(
                text = "Notifications are enabled",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Text(
                text = "Notifications are disabled",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}