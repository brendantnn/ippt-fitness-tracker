package com.example.ippt.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ippt.screens.RunViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*


@Composable
fun RunScreen(navController: NavController, viewModel: RunViewModel = viewModel()) {


    val locationState by viewModel.locationState.collectAsState()
    val steps by viewModel.steps.collectAsState()
    val distance by viewModel.distance.collectAsState()
    val pace by viewModel.pace.collectAsState()
    val timeElapsed by viewModel.timeElapsed.collectAsState()
    val pathPoints by viewModel.pathPoints.collectAsState()

    val cameraPositionState = rememberCameraPositionState()

    var isTracking by remember { mutableStateOf(false) }


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { grantedPermissions ->
            if (grantedPermissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                grantedPermissions[Manifest.permission.ACTIVITY_RECOGNITION] == true) {
                // Permissions granted, but do NOT start tracking yet.
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.resetRunData() // Clear previous run data when screen loads
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        )

    }

    LaunchedEffect(locationState) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(
            LatLng(locationState.latitude, locationState.longitude), 15f
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Running Tracker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E88E5)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Map View with Route Tracking
        //val cameraPositionState = rememberCameraPositionState {
        //    position = CameraPosition.fromLatLngZoom(
          //      LatLng(locationState.latitude, locationState.longitude), 15f
            //)
        //}
        Card(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ){
            //display google map with marker showing user location
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(position = LatLng(locationState.latitude, locationState.longitude)),
                    title = "Your Location"
                )
                Polyline(points = pathPoints, color = Color.Red, width = 5f)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF42A5F5))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Run Stats", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Running Stats
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        SummaryItem(title = "Distance", value = "${String.format("%.2f", distance)} km")
                        SummaryItem(title = "Time", value = formatTime(timeElapsed))
                        SummaryItem(title = "Pace", value = if (pace > 0) "${String.format("%.2f", pace)} min/km" else "--")
                        SummaryItem(title = "Steps", value = "$steps")

                    }
                }
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        // Control Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(
                onClick = {
                    if (isTracking) {
                        viewModel.pauseTracking()
                    } else {
                        viewModel.startTracking()
                    }
                    isTracking = !isTracking
                },
                modifier = Modifier.weight(1f).padding(8.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Green)
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isTracking) "Pause" else "Start",
                    tint = Color.White
                )
            }
            //stop button
            Button(
                onClick = {
                    viewModel.stopTracking()
                    navController.navigate("run_summary")
                },
                modifier = Modifier.weight(1f).padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Stop, contentDescription = "Finish", tint = Color.White)
            }
        }
    }
}