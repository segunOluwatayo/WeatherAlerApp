package com.example.weatheralertapp.data

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatheralertapp.theme.AppTheme
import com.example.weatheralertapp.navigation.NavigationGraph
import com.example.weatheralertapp.navigation.Screen
import com.example.weatheralertapp.settings.SettingsViewModel
import com.example.weatheralertapp.worker.WeatherAlertWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
//    private fun handleThemeChange() {
//        // This will recreate the activity smoothly
//        recreate()
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("MainActivity: Starting weather alert worker")
        lifecycleScope.launch(Dispatchers.Default) {
            WeatherAlertWorker.startImmediateCheck(this@MainActivity)
            WeatherAlertWorker.startPeriodicChecks(this@MainActivity)
        }

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            AppTheme(viewModel = settingsViewModel) {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(navController = navController)
                    }
                ) { innerPadding ->
                    NavigationGraph(
                        navController = navController,
                        settingsViewModel = settingsViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

        @Composable
        fun BottomNavigationBar(navController: NavHostController) {
            val items = listOf(
                Screen.Home to Icons.Default.Home,
                Screen.Alerts to Icons.Default.Warning,
                Screen.Settings to Icons.Default.Settings
            )
            NavigationBar {
                val currentRoute = currentRoute(navController)

                items.forEach { (screen, icon) ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = screen.route
                            )
                        },
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
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }

    @Composable
    fun currentRoute(navController: NavHostController): String? {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        return navBackStackEntry?.destination?.route
    }
}