package com.example.ippt.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Home Screen", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("pushup") }) {
            Text("Go to Pushup Screen")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { navController.navigate("run") }) {
            Text("Go to Run Screen")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { navController.navigate("AR") }) {
            Text("Go to AR Screen")
        }
    }
}