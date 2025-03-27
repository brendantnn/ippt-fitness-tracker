package com.example.ippt.screens
import com.example.ippt.data.IPPTDatabase
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailScreen(navController: NavController, runId: String) {
    val context = LocalContext.current
    val db = IPPTDatabase.getDatabase(context)
    val coroutineScope = rememberCoroutineScope()

    var summary by remember { mutableStateOf("Loading...") }
    var pathPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    LaunchedEffect(runId) {
        coroutineScope.launch {
            val idInt = runId.removePrefix("run_").toIntOrNull()
            if (idInt != null) {
                val run = db.exerciseDao().getExercisesByType("run").find { it.id == idInt }
                summary = run?.summary ?: "No summary available"
                val points = db.runPathDao().getPathPoints(idInt)
                pathPoints = points.map { LatLng(it.latitude, it.longitude) }
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(pathPoints) {
        if (pathPoints.isNotEmpty()) {
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(pathPoints.last(), 15f)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Run Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = summary, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))

            if (pathPoints.isNotEmpty()) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(state = MarkerState(position = pathPoints.first()), title = "Start")
                    Marker(state = MarkerState(position = pathPoints.last()), title = "End")
                    Polyline(points = pathPoints, color = androidx.compose.ui.graphics.Color.Red, width = 5f)
                }
            }
        }
    }
}
