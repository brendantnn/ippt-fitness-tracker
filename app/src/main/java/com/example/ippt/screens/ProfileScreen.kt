package com.example.ippt.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("IPPT_DATA", Context.MODE_PRIVATE)

    val historyList = remember { mutableStateListOf<Pair<String, String>>() }

    LaunchedEffect(Unit) {
        sharedPreferences.all.forEach { (key, value) ->
            historyList.add(Pair(key, value.toString()))
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Exercise History", style = MaterialTheme.typography.headlineMedium)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(historyList) { (key, value) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp).clickable {
                        // Placeholder for future detailed view
                    },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = key.replace("_", " ").capitalize(), style = MaterialTheme.typography.bodyLarge)
                        Text(text = value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }
}
