package com.example.bodyfat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bodyfat.ui.screens.ChartScreen
import com.example.bodyfat.ui.screens.HomeScreen
import com.example.bodyfat.ui.screens.ProfileSetupScreen
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
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    startDestination = if (viewModel.hasProfile()) "home" else "profile"
                }

                if (startDestination == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    NavHost(navController = navController, startDestination = startDestination!!) {
                        composable("profile") {
                            ProfileSetupScreen(
                                onProfileSaved = {
                                    navController.navigate("home") {
                                        popUpTo("profile") { inclusive = true }
                                    }
                                },
                                viewModel = viewModel
                            )
                        }
                        composable("home") {
                            HomeScreen(
                                onNavigateToChart = { navController.navigate("chart") },
                                onNavigateToProfile = { navController.navigate("profile") },
                                viewModel = viewModel
                            )
                        }
                        composable("chart") {
                            ChartScreen(
                                onNavigateBack = { navController.popBackStack() },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
