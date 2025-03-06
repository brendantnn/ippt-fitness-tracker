package com.example.ippt

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.ippt.screens.*

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val runViewModel: RunViewModel = viewModel()

    Scaffold(
        topBar = { TopNavBar() }, // Keeps the top bar consistent
        bottomBar = { BottomNavBar(navController) } //Only one bottom navigation bar
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { HomeScreen(navController) }
            composable("pushup") { PushupScreen(navController) }
            composable("pushup_tracker") { PushupTrackerScreen(navController) }
            composable("ar_view") { ARView(navController) }
            composable("run") { RunScreen(navController, runViewModel) }
        }
    }
}
