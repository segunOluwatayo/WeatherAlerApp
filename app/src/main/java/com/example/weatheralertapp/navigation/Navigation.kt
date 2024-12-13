package com.example.weatheralertapp.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Alerts : Screen("alerts")
    object Settings : Screen("settings")
}