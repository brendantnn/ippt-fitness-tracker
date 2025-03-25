package com.example.ippt.screens
import com.example.ippt.data.ExerciseEntry
import com.example.ippt.data.RunPathPoint
import com.example.ippt.data.IPPTDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSummaryScreen(navController: NavController, viewModel: RunViewModel) {
    val context = LocalContext.current

    val distance by viewModel.distance.collectAsState()
    val timeElapsed by viewModel.timeElapsed.collectAsState()
    val elevationGain by viewModel.elevationGain.collectAsState()
    val pathPoints by viewModel.pathPoints.collectAsState()
    val steps by viewModel.steps.collectAsState()

    val averagePace = viewModel.calculateAveragePace()
    val lastLocation = pathPoints.lastOrNull() ?: LatLng(1.3521, 103.8198)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(lastLocation, 15f)
    }

    // Function to save data
    fun saveRunData(context: Context, pathPoints: List<LatLng>, distance: Double, timeElapsed: Int, elevationGain: Double, steps: Int) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val db = IPPTDatabase.getDatabase(context)

        val runEntry = ExerciseEntry(
            type = "run",
            timestamp = timestamp,
            summary = "Date: $timestamp | Distance: ${String.format("%.2f", distance)} km | Time: ${formatTime(timeElapsed)} | Elevation: ${String.format("%.1f", elevationGain)} m | Steps: $steps"
        )

        CoroutineScope(Dispatchers.IO).launch {
            val runId = db.exerciseDao().insertExercise(runEntry).toInt()
            val pathPointsDb = pathPoints.map {
                RunPathPoint(runId = runId, latitude = it.latitude, longitude = it.longitude)
            }
            db.runPathDao().insertPathPoints(pathPointsDb)
        }

        viewModel.resetRunData()
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Run Summary") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Run Summary", fontSize = 20.sp)

            // ✅ **Add Google Map Here**
            Card(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    if (pathPoints.isNotEmpty()) {
                        Marker(
                            state = MarkerState(position = pathPoints.first()),
                            title = "Start"
                        )
                        Marker(
                            state = MarkerState(position = pathPoints.last()),
                            title = "End"
                        )
                        Polyline(points = pathPoints, color = Color.Red, width = 5f)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryItem(title = "Distance", value = "${String.format("%.2f", distance)} km")
                SummaryItem(title = "Avg Pace", value = if (averagePace > 0) "${String.format("%.2f", averagePace)} min/km" else "--")
                SummaryItem(title = "Elevation", value = "${String.format("%.1f", elevationGain)} m")
                SummaryItem(title = "Steps", value = "$steps")
            }

            Button(
                onClick = {
                    saveRunData(context, pathPoints, distance, timeElapsed, elevationGain, steps)
                    navController.navigate("profile")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
            ) {
                Text(text = "Save", color = Color.White)
            }
        }
    }
}
@Composable
fun SummaryItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 14.sp, color = Color.Gray)
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}