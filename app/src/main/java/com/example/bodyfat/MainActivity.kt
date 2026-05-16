package com.example.bodyfat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bodyfat.ui.screens.ChartScreen
import com.example.bodyfat.ui.screens.HomeScreen
import com.example.bodyfat.ui.screens.SettingsScreen
import com.example.bodyfat.ui.theme.BodyFatTheme
import com.example.bodyfat.viewmodel.BodyFatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BodyFatTheme {
                val navController = rememberNavController()
                val viewModel: BodyFatViewModel = viewModel()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onNavigateToChart = { navController.navigate("chart") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            viewModel = viewModel
                        )
                    }
                    composable("chart") {
                        ChartScreen(
                            onNavigateBack = { navController.popBackStack() },
                            viewModel = viewModel
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
