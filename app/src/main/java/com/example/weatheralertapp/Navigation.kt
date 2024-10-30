package com.example.weatheralertapp

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Alerts : Screen("alerts")
    object Settings : Screen("settings")
}