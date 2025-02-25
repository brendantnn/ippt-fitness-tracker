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
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
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

    var isTracking by remember { mutableStateOf(false) }


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { grantedPermissions ->
            if (grantedPermissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                viewModel.startTracking()
            }
        }
    )

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        )

    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Run Tracker", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Map View
        val singapore = LatLng(locationState.latitude, locationState.longitude)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(singapore, 15f)
        }

        //display google map with marker showing user location
        GoogleMap(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = MarkerState(position = singapore),
                title = "You"
            )        }

        Spacer(modifier = Modifier.height(16.dp))

        // Running Stats
        Text(text = "Distance: ${String.format("%.2f", distance)} km")
        Text(text = "Time: ${timeElapsed / 60}:${timeElapsed % 60}")
        Text(text = "Pace: ${if (pace > 0) String.format("%.2f", pace) else "--"} min/km")
        Text(text = "Steps: $steps")

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(onClick = {
                if (isTracking) {
                    viewModel.pauseTracking()
                } else {
                    viewModel.startTracking()
                }
                isTracking = !isTracking
            }) {
                Text(if (isTracking) "Pause" else "Start")
            }
            Spacer(modifier = Modifier.width(8.dp))
            //stop tracking and navigate to running summary
            Button(onClick = {
                viewModel.stopTracking()
                isTracking = false
                navController.navigate("run_summary") // Navigate to summary screen
            })
            {
                Text("Finish")
            }
        }
    }
}