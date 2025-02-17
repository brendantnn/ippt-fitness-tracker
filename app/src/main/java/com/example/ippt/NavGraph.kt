package com.example.ippt

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ippt.screens.HomeScreen
import com.example.ippt.screens.PushupScreen
import com.example.ippt.screens.RunScreen




@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("pushup") { PushupScreen(navController) }
        composable("run") { RunScreen(navController) }
    }
}
