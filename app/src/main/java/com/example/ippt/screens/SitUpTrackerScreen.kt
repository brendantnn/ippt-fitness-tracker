package com.example.ippt.screens

import android.R.attr.text
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.ippt.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.atan2

@Composable
fun SitupTrackerScreen(navController: NavController){
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("IPPT_DATA", Context.MODE_PRIVATE)

    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    // Set landscape orientation when the screen is started and reset when leaving
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    //For Sit UP
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isCameraOn by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var sitUpCount by remember { mutableStateOf(0) }
    var elbowAngle by remember { mutableStateOf(0.0) }
    var backAngle by remember { mutableStateOf(0.0) }
    var legAngle by remember { mutableStateOf(0.0) }
    var hipAngle by remember { mutableStateOf(0.0) }
    var keyPoints by remember { mutableStateOf(emptyList<Pair<Float, Float>>()) }
    var isUserReady by remember { mutableStateOf(false) }
    var isBackStraight by remember { mutableStateOf(true) }
    var firstPushUpDone by remember { mutableStateOf(false) }

    // New state for 10s countdown timer
    var countdownTimer by remember { mutableStateOf(3) }
    var isCountingDown by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // For 60 sec timer
    var timeLeft by remember { mutableStateOf(60) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary

    // Request camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            isCameraOn = true
        }
    }

    fun saveSitupData(count: Int) {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        editor.putString("situp_$timestamp", count.toString())
        editor.apply()
    }

    // Run once when enter recomposition and never reexecutes
    LaunchedEffect(Unit) {
        // Check if camera permission is granted
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            hasCameraPermission = true
            isCameraOn = true
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
        //playPositionSound(context)

        // Start the 10-second countdown
        isCountingDown = true
        for (i in 10 downTo 1) {
            countdownTimer = i
            delay(1000L)
        }
        countdownTimer = 0
        isCountingDown = false
    }

    // Start 60 second timer when 10 second timer ends
    LaunchedEffect(isCountingDown) {
        if (!isCountingDown) {
            isTimerRunning = true
            timeLeft = 60  // Reset timer when it starts
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft -= 1
            }
            isTimerRunning = false
            showSaveDialog = true // Show save/discard options when time runs out
        }
    }

    //Main UI
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // Start camera preview if permission is granted
        if (hasCameraPermission) {
            val lifecycleOwner = LocalLifecycleOwner.current
            CameraPreview2(context, lifecycleOwner, { isCountingDown }, cameraExecutor) {
                    newCount, newBackAngle, newKeyPoints, isUp ->
                sitUpCount = newCount
                backAngle = newBackAngle
                keyPoints = newKeyPoints
            }

            if (isCountingDown) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "Align your body with the outline",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 32.dp)
                    )

                    SitUpOutline(modifier = Modifier
                        .align(Alignment.Center))

                    Text(
                        text = "Keep your back and knees straight.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$countdownTimer",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                }
            }

            if (!isCountingDown) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.3f)
                            .height(80.dp)
                            .background(Color.DarkGray.copy(alpha = 0.8f))
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$sitUpCount",
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White,
                                modifier = Modifier.weight(0.5f))
                            Text(
                                text = "SIT-UPS",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.weight(0.1f))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.7f)
                            .height(80.dp)
                            .background(Color.DarkGray.copy(alpha = 0.8f))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DrawSkeletonLines(keyPoints, isBackStraight, hipAngle in 160.0..200.0, boxSize = 200f)
                    }

                    Box(
                        modifier = Modifier.weight(0.3f).height(80.dp).background(Color.DarkGray.copy(alpha = 0.8f)).padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$timeLeft",
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White,
                                modifier = Modifier.weight(0.5f))
                            Text(text = "SECONDS LEFT",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.weight(0.1f))
                        }
                    }
                }
            }
        } else {
            Text("Camera permission is required to use this feature.", modifier = Modifier.align(Alignment.Center), color = Color.White)
        }
    }

    // Show save/discard dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Session Over") },
            text = { Text("You completed $sitUpCount push-ups. Do you want to save the session?") },
            confirmButton = {
                Button(onClick = {
                    saveSitupData(sitUpCount)
                    sitUpCount = 0
                    navController.navigate("profile")
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = {
                    sitUpCount = 0  // Reset push-up count
                    showSaveDialog = false
                    navController.popBackStack()  // Navigate back
                }) {
                    Text("Discard")
                }
            }
        )
    }


}
@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview2(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    isCountingDown: () -> Boolean,
    cameraExecutor: ExecutorService,
    onDataUpdate: (Int, Double, List<Pair<Float, Float>>, Boolean) -> Unit
) {
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        startCameras(previewView, context, lifecycleOwner, cameraExecutor, isCountingDown, onDataUpdate)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { previewView }
    )
}


private fun startCameras(
    previewView: PreviewView,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    isCountingDown: () -> Boolean,
    onDataUpdate: (Int, Double, List<Pair<Float, Float>>, Boolean) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        try {
            cameraProvider.unbindAll()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processSitUpDetection(imageProxy, context, isCountingDown) { newCount, newBackAngle, newKeyPoints, isUp ->
                            onDataUpdate(newCount, newBackAngle, newKeyPoints, isUp)
                        }
                    }
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
        } catch (e: Exception) {
            Log.e("CameraDebug", "Failed to start camera: ${e.message}")
        }
    }, ContextCompat.getMainExecutor(context))
}


//to track the sit up
fun getSitUpAngle(
    landmarks: List<PoseLandmark>,
    p1: Int,
    p2: Int,
    p3: Int
): Double {
    val point1 = landmarks.find { it.landmarkType == p1 }?.position
    val point2 = landmarks.find { it.landmarkType == p2 }?.position
    val point3 = landmarks.find { it.landmarkType == p3 }?.position

    if (point1 == null || point2 == null || point3 == null) return 0.0 // Prevent crashes if keypoints are missing

    val angle = Math.toDegrees(
        atan2((point3.y - point2.y).toDouble(), (point3.x - point2.x).toDouble()) -
                atan2((point1.y - point2.y).toDouble(), (point1.x - point2.x).toDouble())
    )

    return if (angle < 0) angle + 360 else angle
}



//for sit up detection
private var sitUpCount = 0 // Keep track of sit-ups
private var wasGoingUp = false // Track upward movement

private fun processSitUpDetection(
    imageProxy: ImageProxy,
    context: Context,
    isCountingDown: () -> Boolean,
    onDataUpdate: (Int, Double, List<Pair<Float, Float>>, Boolean) -> Unit
) {
    val mediaImage = imageProxy.image ?: return
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

    val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .build()
    val poseDetector = PoseDetection.getClient(options)

    poseDetector.process(image)
        .addOnSuccessListener { pose ->
            val landmarks = pose.allPoseLandmarks
            if (landmarks.isNotEmpty()) {
                val backToHipAngle = getSitUpAngle(
                    landmarks,
                    PoseLandmark.LEFT_SHOULDER,
                    PoseLandmark.LEFT_HIP,
                    PoseLandmark.LEFT_KNEE
                )

                Log.d("SitUpTracker", "Back-to-Hip Angle: $backToHipAngle")

                val keyPoints = listOf(
                    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
                    PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE
                ).mapNotNull { (start, end) ->
                    val startPoint = landmarks.find { it.landmarkType == start }?.position
                    val endPoint = landmarks.find { it.landmarkType == end }?.position
                    if (startPoint != null && endPoint != null) {
                        listOf(Pair(startPoint.x, startPoint.y), Pair(endPoint.x, endPoint.y))
                    } else {
                        null
                    }
                }.flatten()

                var isUp = false
                if (!isCountingDown()) {
                    if (backToHipAngle < 50) { // Sit-up "up" position
                        if (!wasGoingUp) {
                            sitUpCount++
                            wasGoingUp = true
                            Log.d("SitUpTracker", "Sit-Up Counted! Total: $sitUpCount")
                        }
                        isUp = true
                    } else if (backToHipAngle > 110) { // "Down" position
                        wasGoingUp = false
                        isUp = false
                    }
                }

                onDataUpdate(sitUpCount, backToHipAngle, keyPoints, isUp)
            } else {
                Log.e("SitUpTracker", "No Pose Detected!")
            }
        }
        .addOnFailureListener { e ->
            Log.e("SitUpTracker", "Pose Detection Failed: ${e.message}", e)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}



@Composable
fun SitUpOutline(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)  // Adjust spacing
    ) {
        val strokeWidth = 8f
        val outlineColor = Color.White.copy(alpha = 0.7f)  // Improve visibility

        // Define key points with the head on the right side
        val headRadius = 100f
        val headCenter = Offset(size.width * 0.65f, size.height * 0.7f) // Head on the right

        val shoulder = Offset(size.width * 0.60f, size.height * 0.7f) // Shoulders to the left of the head
        val hip = Offset(size.width * 0.45f, size.height * 0.7f) // Hips further left

        val knee = Offset(size.width * 0.35f, size.height * 0.50f)  // Knees bent upwards
        val foot = Offset(size.width * 0.25f, size.height * 0.70f)  // Feet planted on ground

        // Draw Head
        drawCircle(
            color = outlineColor,
            radius = headRadius,
            center = headCenter
        )

        // Draw Longer Torso (shoulders to hips)
        drawLine(
            color = outlineColor,
            start = shoulder,
            end = hip,
            strokeWidth = strokeWidth
        )

        // Draw Bent Leg (hip to knee to foot in inverted "V" shape)
        drawLine(
            color = outlineColor,
            start = hip,
            end = knee,
            strokeWidth = strokeWidth
        )
        drawLine(
            color = outlineColor,
            start = knee,
            end = foot,
            strokeWidth = strokeWidth
        )
    }
}


@Composable
fun SitUpTrackerUI(
    sitUpCount: Int,
    backAngle: Double
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Sit-Ups: $sitUpCount",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Back Angle: ${backAngle.toInt()}°",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

