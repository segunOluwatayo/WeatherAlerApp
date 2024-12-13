package com.example.weatheralertapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.weatheralertapp.settings.SettingsViewModel
import com.example.weatheralertapp.ui.theme.DarkColorScheme
import com.example.weatheralertapp.ui.theme.LightColorScheme
import com.example.weatheralertapp.ui.theme.Typography

@Composable
fun AppTheme(
    viewModel: SettingsViewModel,
    content: @Composable () -> Unit
) {
    val preferences by viewModel.preferences.collectAsState()
    val context = LocalContext.current
    val themeMode = preferences.themeMode

    val shouldUseDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> {
            val isDark = isSystemInDarkTheme()
            println("System theme is dark: $isDark")  // Debug log
            isDark
        }
        ThemeMode.DARK -> {
            println("Using dark theme")  // Debug log
            true
        }
        ThemeMode.LIGHT -> {
            println("Using light theme")  // Debug log
            false
        }
    }

    val dynamicColor = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

    val colorScheme = when {
        dynamicColor && shouldUseDarkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && !shouldUseDarkTheme -> dynamicLightColorScheme(context)
        shouldUseDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}