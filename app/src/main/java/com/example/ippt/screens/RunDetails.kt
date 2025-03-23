package com.example.ippt.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailScreen(navController: NavController, runId: String) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("IPPT_DATA", Context.MODE_PRIVATE)

    val summary = sharedPreferences.getString("${runId}_summary", "No summary available") ?: ""
    val pathRaw = sharedPreferences.getString("${runId}_path", "") ?: ""
    val pathPoints = deserializePathPoints(pathRaw)

    val cameraPositionState = rememberCameraPositionState {
        val lastLocation = pathPoints.lastOrNull() ?: LatLng(1.3521, 103.8198)
        position = CameraPosition.fromLatLngZoom(lastLocation, 15f)
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

fun serializePathPoints(pathPoints: List<LatLng>): String {
    return pathPoints.joinToString(";") { "${it.latitude},${it.longitude}" }
}

fun deserializePathPoints(data: String): List<LatLng> {
    return data.split(";").mapNotNull {
        val latLng = it.split(",")
        if (latLng.size == 2) {
            val lat = latLng[0].toDoubleOrNull()
            val lng = latLng[1].toDoubleOrNull()
            if (lat != null && lng != null) LatLng(lat, lng) else null
        } else null
    }
}