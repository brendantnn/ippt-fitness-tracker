package com.example.ippt.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.ippt.R

@Composable
fun SitupScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title at the top
        Text(
            text = "Sit-Up Tracker\n",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 32.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Evenly spaced buttons (matching HomeScreen)
        Spacer(modifier = Modifier.weight(1f))
        GradientButton(
            imageRes = R.drawable.ic_sit_up,
            text = "Start Tracking",
            onClick = { navController.navigate("situp_tracker") }
        )

        Spacer(modifier = Modifier.weight(1f))
        GradientButton(
            imageRes = R.drawable.ic_ar_situp,
            text = "View in AR",
            onClick = { navController.navigate("situp_ar_view") }
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
fun SitupScreenPreview() {
    SitupScreen(navController = rememberNavController())
}
