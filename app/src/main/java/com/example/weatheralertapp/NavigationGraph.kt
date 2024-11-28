package com.example.weatheralertapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavigationGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    val settingsViewModel: SettingsViewModel = viewModel()
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
            SettingsScreen( viewModel = settingsViewModel)
        }
    }
}