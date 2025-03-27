package com.example.ippt.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun RunScreen(navController: NavController, viewModel: RunViewModel = viewModel()) {
    val locationState by viewModel.locationState.collectAsState()
    val steps by viewModel.steps.collectAsState()
    val distance by viewModel.distance.collectAsState()
    val pace by viewModel.pace.collectAsState()
    val timeElapsed by viewModel.timeElapsed.collectAsState()
    val pathPoints by viewModel.pathPoints.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        //contract = ActivityResultContracts.RequestPermission()
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ){ permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION]== true
        val activityRecognitionGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true

        if(!fineLocationGranted){
            Toast.makeText(context, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
        if(!activityRecognitionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Toast.makeText(context, "Activity recognition permission is required to track steps", Toast.LENGTH_SHORT).show()
        }

    }
    // { isGranted ->
       // if (!isGranted) {
         //   Toast.makeText(context, "Location permission is required for map features", Toast.LENGTH_SHORT).show()
        //}
    //}
    val cameraPositionState = rememberCameraPositionState()
    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        cameraPositionState.position = CameraPosition.fromLatLngZoom(
            LatLng(locationState.latitude, locationState.longitude), 15f
        )
    }
    /*
    LaunchedEffect(locationState) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        cameraPositionState.position = CameraPosition.fromLatLngZoom(
            LatLng(locationState.latitude, locationState.longitude), 15f
        )
    }*/

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Running Tracker",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(position = LatLng(locationState.latitude, locationState.longitude)),
                    title = "Your Location"
                )
                Polyline(points = pathPoints, color = Color.Red, width = 5f)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Run Stats",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem(title = "Distance", value = "${String.format("%.2f", distance)} km")
                    SummaryItem(title = "Time", value = formatTime(timeElapsed))
                    SummaryItem(title = "Pace", value = if (pace > 0) "${String.format("%.2f", pace)} min/km" else "--")
                    SummaryItem(title = "Steps", value = "$steps")
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                onClick = {
                    if (isTracking) viewModel.pauseTracking()
                    else {
                        viewModel.resetRunData()
                        viewModel.startTracking()
                    }
                },
                modifier = Modifier.size(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isTracking) "Pause" else "Start",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            Button(
                onClick = {
                    viewModel.stopTracking()
                    navController.navigate("run_summary")
                },
                modifier = Modifier.size(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Finish",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
