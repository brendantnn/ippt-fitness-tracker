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
import android.hardware.*
import com.google.android.gms.location.*
import android.hardware.*
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.Manifest
import android.content.pm.PackageManager



//data class representing users location state
data class LocationState(val latitude: Double = 0.0, val longitude: Double = 0.0, val speed: Float = 0.0f)

class RunViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    // stored variables.
    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState

    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    private val _distance = MutableStateFlow(0.0)
    val distance: StateFlow<Double> = _distance

    private val _pace = MutableStateFlow(0.0)
    val pace: StateFlow<Double> = _pace

    private val _timeElapsed = MutableStateFlow(0)
    val timeElapsed: StateFlow<Int> = _timeElapsed

    //GPS location tracking
    //fusedLocationClient: Google API to fetch real-time location updates
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private var lastLocation: Location? = null
    private var isTracking = false

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 2000
    ).apply {
        setMinUpdateDistanceMeters(5.0f) // Equivalent to smallestDisplacement
        setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
    }.build()


    //updates user location and computes distance covered
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.lastOrNull()?.let { location ->
                val prevLocation = lastLocation
                lastLocation = location

                _locationState.value = LocationState(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speed = location.speed
                )

                if (prevLocation != null && isTracking) {
                    val distanceIncrement = prevLocation.distanceTo(location) / 1000 // convert to km
                    _distance.value += distanceIncrement

                    // Calculate pace (min/km) -> time esplased / distance
                    if (_distance.value > 0) {
                        _pace.value = (_timeElapsed.value / 60.0) / _distance.value
                    }
                }
            }
        }
    }

    //registers the step senor to track step count
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    //starts tracking time and distance when user begins running
    //Request location updates from google api
    fun startTracking() {
        val applicationContext = getApplication<Application>().applicationContext

        // Check for location permissions before requesting updates
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            // Handle case where permission is not granted (return early)
            return
        }

        isTracking = true
        viewModelScope.launch {
            while (isTracking) {
                delay(1000)
                _timeElapsed.value += 1
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        // Register Step Counter Sensor
        stepSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    //paste without resetting data
    fun pauseTracking() {
        isTracking = false
    }

    //Stops tracking completely and clears location updates.
    fun stopTracking() {
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                _steps.value = it.values[0].toInt()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}