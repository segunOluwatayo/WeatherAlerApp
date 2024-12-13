package com.example.weatheralertapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.weatheralertapp.alerts.AlertsScreen
import com.example.weatheralertapp.home.HomeScreen
import com.example.weatheralertapp.settings.SettingsScreen
import com.example.weatheralertapp.settings.SettingsViewModel

@Composable
fun NavigationGraph(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }
        composable(Screen.Alerts.route) {
            AlertsScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = settingsViewModel)
        }
    }
}