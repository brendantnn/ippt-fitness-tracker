package com.example.ippt

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ippt.screens.HomeScreen
import com.example.ippt.screens.PushupScreen
import com.example.ippt.screens.PushupTrackerScreen
import com.example.ippt.screens.RunScreen
import com.example.ippt.screens.RunSummaryScreen
import com.example.ippt.screens.RunViewModel


@Composable

fun NavGraph() {
    val navController = rememberNavController()
    val runViewModel: RunViewModel = viewModel() // Shared ViewModel


    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("pushup") { PushupScreen(navController) }
        composable("pushup_tracker") { PushupTrackerScreen(navController) }
        composable("run") { RunScreen(navController, runViewModel) }
        composable("run_summary") { RunSummaryScreen(navController, runViewModel) }

    }
}
