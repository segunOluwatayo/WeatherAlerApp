package com.example.weatheralertapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavigationGraph(navController: NavHostController, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
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
            SettingsScreen()
        }
    }
}