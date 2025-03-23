package com.example.ippt.screens

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
import com.example.ippt.data.IPPTDatabase
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current

    val runHistory = remember { mutableStateListOf<RunHistoryEntry>() }
    val pushupHistory = remember { mutableStateListOf<Pair<String, String>>() }
    val situpHistory = remember { mutableStateListOf<Pair<String, String>>() }

    val db = IPPTDatabase.getDatabase(context)

    LaunchedEffect(Unit) {
        val runEntries = db.exerciseDao().getExercisesByType("run")
        val pushupEntries = db.exerciseDao().getExercisesByType("pushup")
        val situpEntries = db.exerciseDao().getExercisesByType("situp")

        val runPathDao = db.runPathDao()
        val fullRunData = runEntries.map { run ->
            val pathPoints = runPathDao.getPathPoints(run.id).map {
                LatLng(it.latitude, it.longitude)
            }
            RunHistoryEntry(
                runId = "run_${run.id}",
                summary = run.summary ?: "",
                pathPoints = pathPoints
            )
        }

        runHistory.addAll(fullRunData)
        pushupHistory.addAll(pushupEntries.map { it.timestamp to "${it.count ?: 0}" })
        situpHistory.addAll(situpEntries.map { it.timestamp to "${it.count ?: 0}" })
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Exercise History", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Runs", style = MaterialTheme.typography.titleMedium)
        }

        items(runHistory) { run ->
            RunHistoryCard(run = run, navController = navController)
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Push-ups", style = MaterialTheme.typography.titleMedium)
        }

        items(pushupHistory) { (key, value) ->
            HistoryCard(key, value)
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Sit-ups", style = MaterialTheme.typography.titleMedium)
        }

        items(situpHistory) { (key, value) ->
            HistoryCard(key, value)
        }
    }
}

@Composable
fun RunHistoryCard(run: RunHistoryEntry, navController: NavController) {
    val cameraPositionState = rememberCameraPositionState {
        val lastLocation = run.pathPoints.lastOrNull() ?: LatLng(1.3521, 103.8198)
        position = CameraPosition.fromLatLngZoom(lastLocation, 14f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { navController.navigate("runDetail/${run.runId}") },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(run.summary, style = MaterialTheme.typography.bodyLarge)

            if (run.pathPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(state = MarkerState(position = run.pathPoints.first()), title = "Start")
                    Marker(state = MarkerState(position = run.pathPoints.last()), title = "End")
                    Polyline(points = run.pathPoints, color = androidx.compose.ui.graphics.Color.Red, width = 5f)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(key: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = key.replace("_", " ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge)
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

data class RunHistoryEntry(
    val runId: String,
    val summary: String,
    val pathPoints: List<LatLng>
)
