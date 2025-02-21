package com.example.ippt.screens

import android.app.Application
import android.content.Context
import android.hardware.*
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.ippt.screens.RunViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.hardware.*
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch


data class LocationState(val latitude: Double = 0.0, val longitude: Double = 0.0, val speed: Float = 0.0f)

class RunViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val locationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = 2000
        smallestDisplacement = 5.0f
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.lastOrNull()?.let { location ->
                _locationState.value = LocationState(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speed = location.speed
                )
            }
        }
    }

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun startTracking() {
        if (ActivityCompat.checkSelfPermission(
                getApplication(), android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }

        // Register Step Counter
        stepSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> _steps.value = it.values[0].toInt()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}