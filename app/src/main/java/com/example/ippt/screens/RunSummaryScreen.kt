package com.example.ippt.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSummaryScreen(navController: NavController, viewModel: RunViewModel) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Run Summary") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryItem(title = "Distance", value = "${String.format("%.2f", distance)} km")
                SummaryItem(title = "Avg Pace", value = if (averagePace > 0) "${String.format("%.2f", averagePace)} min/km" else "--")
                SummaryItem(title = "Elevation", value = "${String.format("%.1f", elevationGain)} m")
                SummaryItem(title = "Steps", value = "$steps")
            }

            Button(
                onClick = { viewModel.resetRunData(); navController.navigate("home") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
            ) {
                Text(text = "View Analysis", color = Color.White)
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
