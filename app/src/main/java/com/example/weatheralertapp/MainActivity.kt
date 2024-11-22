package com.example.weatheralertapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.weatheralertapp.ui.theme.WeatherAlertAppTheme
import java.util.Locale

// The MainActivity class which serves as the entry point for the app
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the weather alert worker
        println("MainActivity: Starting weather alert worker")
        WeatherAlertWorker.startImmediateCheck(this)
        WeatherAlertWorker.startPeriodicChecks(this)

        // The set content id setting the content view with the custom theme and navigation setup
        setContent {
            WeatherAlertAppTheme {
                // The controller for navigation
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        // A set up for bottom navigation bar to show the 3 layouts of home, alerts and settings
                        BottomNavigationBar(navController = navController)
                    }
                ) { innerPadding ->
                    // Passes the navController and padding to the navigation graph
                    NavigationGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }


    // Composable function for setting up the Bottom Navigation Bar
    @Composable
    fun BottomNavigationBar(navController: NavHostController) {
        // List of options available in the navigation bar
        val items = listOf(
            Screen.Home,
            Screen.Alerts,
            Screen.Settings
        )
        NavigationBar {
            // Retrieval of the current route
            val currentRoute = currentRoute(navController)

            // Loops through each options in items and create a navigation bar item for each
            items.forEach { screen ->
                NavigationBarItem(
                    icon = {
                        // Set the icon for each item NB: (this currently uses the same icon for all for milestone 1)
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = screen.route
                        )
                    }, // Note to self: I have to replace with the right icons for milestone 2
                    label = {
                        Text(screen.route.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(
                                Locale.ROOT
                            ) else it.toString()
                        })
                    },
                    selected = currentRoute == screen.route,
                    onClick = {
                        navController.navigate(screen.route) {
                            // Pop up to the start destination of the graph
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid duplicate navigation to the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    }

    // A helper function to get the current route in the navigation stack
    @Composable
    fun currentRoute(navController: NavHostController): String? {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        return navBackStackEntry?.destination?.route
    }
}
