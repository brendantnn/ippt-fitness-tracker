package com.example.ippt.screens

import android.content.Context
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("IPPT_DATA", Context.MODE_PRIVATE)

    val runHistory = remember { mutableStateListOf<RunHistoryEntry>() }
    val pushupHistory = remember { mutableStateListOf<Pair<String, String>>() }
    val situpHistory = remember { mutableStateListOf<Pair<String, String>>() }

    LaunchedEffect(Unit) {
        val tempRunHistory = mutableListOf<RunHistoryEntry>()
        val tempPushupHistory = mutableListOf<Pair<String, String>>()
        val tempSitupHistory = mutableListOf<Pair<String, String>>()

        sharedPreferences.all.forEach { (key, value) ->
            when {
                key.endsWith("_summary") -> {
                    val runId = key.removeSuffix("_summary")
                    val summary = value.toString()
                    val pathRaw = sharedPreferences.getString("${runId}_path", "") ?: ""
                    val pathPoints = deserializePathPoints(pathRaw)
                    tempRunHistory.add(RunHistoryEntry(runId, summary, pathPoints))
                }
                key.contains("pushup", ignoreCase = true) -> tempPushupHistory.add(Pair(key, value.toString()))
                key.contains("situp", ignoreCase = true) -> tempSitupHistory.add(Pair(key, value.toString()))
            }
        }

        runHistory.clear()
        runHistory.addAll(tempRunHistory.sortedByDescending { extractDateFromKeyOrId(it.summary) })

        pushupHistory.clear()
        pushupHistory.addAll(tempPushupHistory.sortedByDescending { extractDateFromKeyOrId(it.first) })

        situpHistory.clear()
        situpHistory.addAll(tempSitupHistory.sortedByDescending { extractDateFromKeyOrId(it.first) })
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = key.replace("_", " ").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge
            )
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

data class RunHistoryEntry(
    val runId: String,
    val summary: String,
    val pathPoints: List<LatLng>
)

fun extractDateFromKeyOrId(key: String): Date {
    val datePatterns = listOf(
        "yyyy-MM-dd HH:mm:ss", // for pushup/situp like: pushup_2025-03-24 18:47:13
        "yyyyMMdd",            // fallback pattern if using compact format
        "yyyy-MM-dd"           // another fallback
    )

    val match = Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}").find(key)?.value
        ?: Regex("\\d{8}").find(key)?.value
        ?: Regex("\\d{4}-\\d{2}-\\d{2}").find(key)?.value
        ?: return Date(0)

    for (pattern in datePatterns) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            return sdf.parse(match) ?: Date(0)
        } catch (_: Exception) {}
    }

    return Date(0) // fallback if parsing fails
}
