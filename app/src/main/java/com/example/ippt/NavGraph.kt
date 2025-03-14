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

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        topBar = { if (currentRoute !in listOf("sit-up", "pushup_tracker")) TopNavBar() },
        bottomBar = { if (currentRoute !in listOf("sit-up", "pushup_tracker")) BottomNavBar(navController) }
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
            composable("sit-up"){SitupTrackerScreen(navController)}
        }
    }
}

