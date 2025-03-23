package com.example.ippt.screens

import android.app.Application
import android.content.Context
import android.hardware.*
import android.location.Location
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LocationState(val latitude: Double = 0.0, val longitude: Double = 0.0, val speed: Float = 0.0f)

class RunViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

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

    private val _pathPoints = MutableStateFlow<List<LatLng>>(emptyList())
    val pathPoints: StateFlow<List<LatLng>> = _pathPoints

    private val _elevationGain = MutableStateFlow(0.0)
    val elevationGain: StateFlow<Double> = _elevationGain

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private var initialSteps = 0
    private var lastLocation: Location? = null

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 2000
    ).apply {
        setMinUpdateDistanceMeters(5.0f)
        setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
    }.build()

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

                if (prevLocation != null && _isTracking.value) {
                    val distanceIncrement = prevLocation.distanceTo(location) / 1000
                    _distance.value += distanceIncrement

                    if (_distance.value > 0) {
                        _pace.value = (_timeElapsed.value / 60.0) / _distance.value
                    }

                    _pathPoints.value += LatLng(location.latitude, location.longitude)

                    val elevationDiff = location.altitude - prevLocation.altitude
                    if (elevationDiff > 0) {
                        _elevationGain.value += elevationDiff
                    }
                }
            }
        }
    }

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun startTracking() {
        val context = getApplication<Application>().applicationContext
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                _locationState.value = LocationState(latitude = it.latitude, longitude = it.longitude, speed = it.speed)
            }
        }

        _isTracking.value = true

        viewModelScope.launch {
            while (_isTracking.value) {
                delay(1000)
                _timeElapsed.value += 1
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun pauseTracking() {
        _isTracking.value = false
    }

    fun stopTracking() {
        _isTracking.value = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val currentSteps = it.values[0].toInt()
                if (initialSteps == 0 && _isTracking.value) {
                    initialSteps = currentSteps
                }
                _steps.value = (currentSteps - initialSteps).coerceAtLeast(0)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun resetRunData() {
        _distance.value = 0.0
        _pace.value = 0.0
        _steps.value = 0
        _timeElapsed.value = 0
        _pathPoints.value = emptyList()
        _elevationGain.value = 0.0
        lastLocation = null
        initialSteps = 0
    }

    fun calculateAveragePace(): Double {
        return if (_distance.value > 0 && _timeElapsed.value > 0) {
            (_timeElapsed.value / 60.0) / _distance.value
        } else 0.0
    }
}
