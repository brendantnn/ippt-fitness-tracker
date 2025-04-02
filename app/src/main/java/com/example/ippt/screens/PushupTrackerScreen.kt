package com.example.ippt.screens

import com.example.ippt.data.ExerciseEntry
import com.example.ippt.data.IPPTDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.atan2


@Composable
fun PushupTrackerScreen(navController: NavController) {
    val context = LocalContext.current
    // val sharedPreferences removed (replaced with DAO)
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

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isCameraOn by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var pushUpCount by remember { mutableIntStateOf(0) }
    var elbowAngle by remember { mutableDoubleStateOf(0.0) }
    val legAngle by remember { mutableDoubleStateOf(0.0) }
    var hipAngle by remember { mutableDoubleStateOf(0.0) }
    var keyPoints by remember { mutableStateOf(emptyList<Pair<Float, Float>>()) }
    var isBackStraight by remember { mutableStateOf(true) }


    // New state for 10s countdown timer
    var countdownTimer by remember { mutableIntStateOf(3) }
    var isCountingDown by remember { mutableStateOf(true) }

    // For 60 sec timer
    var timeLeft by remember { mutableIntStateOf(60) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary

    // Function to save data
    
fun savePushupData(context: Context, count: Int) {
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    val db = IPPTDatabase.getDatabase(context)
    val entry = ExerciseEntry(type = "pushup", timestamp = timestamp, count = count)
    CoroutineScope(Dispatchers.IO).launch {
        db.exerciseDao().insertExercise(entry)
    }
}
    

    // Request camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            isCameraOn = true
        }
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
        playPositionSound(context)

        // Start the 10-second countdown
        isCountingDown = true
        for (i in 15 downTo 1) {
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
            isCameraOn = false // Turn off the camera
            showSaveDialog = true // Show save/discard options when time runs out
        }
    }

    // Main UI
    Box(modifier = Modifier.fillMaxSize()) {
        // Start camera preview if permission is granted
        if (hasCameraPermission  && isCameraOn) {
                val lifecycleOwner = LocalLifecycleOwner.current
                CameraPreview(context, lifecycleOwner, { isCountingDown }, isTimerRunning = { isTimerRunning }, cameraExecutor) { newCount, newElbowAngle, newHipAngle, newKeyPoints, backStraight ->
                    pushUpCount = newCount
                    elbowAngle = newElbowAngle
                    hipAngle = newHipAngle
                    keyPoints = newKeyPoints
                    isBackStraight = backStraight
                }


                // UI during 10sec timer
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

                        // Show Push-Up Outline overlay
                        PushUpOutline(
                            modifier = Modifier
                                .align(Alignment.Center)
                        )

                        // Additional Instructions
                        Text(
                            text = "Keep your back and knees straight.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(32.dp)
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


                // UI after 10 second timer
                if (!isCountingDown) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                     
                        Box(
                            modifier = Modifier
                                .weight(0.3f)
                                .height(80.dp)
                                .background(Color.DarkGray.copy(alpha = 0.8f))
                                .padding(5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(   modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$pushUpCount",
                                    style = MaterialTheme.typography.displayLarge,
                                    color =  primaryColor,
                                    modifier = Modifier.weight(0.5f)
                                )
                                Text(
                                    text = "PUSH UPS",
                                    style = MaterialTheme.typography.bodySmall,
                                    color =  primaryColor,
                                    modifier = Modifier.weight(0.1f)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.6f)
                                .height(80.dp)
                                .background(Color.DarkGray.copy(alpha = 0.8f))
                                .padding(10.dp)
                            ,    contentAlignment = Alignment.Center

                        ) {
                            DrawSkeletonLines(
                                keyPoints = keyPoints,
                                backStraight = isBackStraight,
                                legStraight = legAngle in 160.0..200.0,
                                boxSize = 200f
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.3f)
                                .height(80.dp)
                                .background(Color.DarkGray.copy(alpha = 0.8f))
                                .padding(5.dp),
                            contentAlignment = Alignment.Center

                        ) {

                            Column(   modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                Text(
                                    text = "$timeLeft",
                                    style = MaterialTheme.typography.displayLarge,
                                    color =  primaryColor,
                                    modifier = Modifier.weight(0.5f)
                                )
                                Text(
                                    text = "SECONDS LEFT",
                                    style = MaterialTheme.typography.bodySmall,
                                    color =  primaryColor,
                                    modifier = Modifier.weight(0.1f)
                                )

                            }

                        }


                    }

                    // Warning if back or legs aren't straight
                    if (!isBackStraight) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                                .padding(16.dp)

                        ) {
                            Text(
                                text = "Straighten Back",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                }

            }

        }
        // Show permission required message
        else {

            Text("Camera permission is required to use this feature.", modifier = Modifier.align(Alignment.Center))
        }
        }


    // Show save/discard dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Session Over") },
            text = { Text("You completed $pushUpCount push-ups. Do you want to save the session?") },
            confirmButton = {
                Button(onClick = {
                    savePushupData(context, pushUpCount)
                    pushUpCount = 0
                    navController.navigate("profile")
                }) {
                    Text("Finish Set: ($pushUpCount pushups)")
                }
            },
            dismissButton = {
                Button(onClick = {
                    pushUpCount = 0  // Reset push-up count
                    isCameraOn = true // Restart camera when returning
                    showSaveDialog = false
                    navController.popBackStack()  // Navigate back
                }) {
                    Text("Discard")
                }
            }
        )
    }
}
var mediaPlayerPosition: MediaPlayer? = null

fun playPositionSound(context: Context) {
    if (mediaPlayerPosition == null) {
        mediaPlayerPosition = MediaPlayer.create(context, R.raw.position) // Ensure position.mp3 is in res/raw
    }
    mediaPlayerPosition?.start()
}

@Composable
fun DrawSkeletonLines(
    keyPoints: List<Pair<Float, Float>>,
    backStraight: Boolean,
    legStraight: Boolean,
    boxSize: Float // Controls the overall size of the skeleton
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (keyPoints.isEmpty()) return@Canvas

        // Find min and max points for scaling
        val minX = keyPoints.minOf { it.first }
        val maxX = keyPoints.maxOf { it.first }
        val minY = keyPoints.minOf { it.second }
        val maxY = keyPoints.maxOf { it.second }

        val widthRange = maxX - minX
        val heightRange = maxY - minY

        val scaleMultiplier = 2f  // Increase this value for a bigger skeleton
        val scale = scaleMultiplier * if (widthRange > heightRange) {
            boxSize / widthRange
        } else {
            boxSize / heightRange
        }

        val centerY = size.height / 2

        // **Fix: Mirror X-coordinates for Front Camera**
        val normalizedKeyPoints = keyPoints.map { (x, y) ->
            val flippedX = size.width - ((x - minX) * scale)  // Flip horizontally
            val scaledY = ((y - minY) * scale) - (heightRange * scale / 2)
            Pair(flippedX, centerY + scaledY)
        }

        // Draw skeleton lines with conditional coloring
        normalizedKeyPoints.chunked(2).forEachIndexed { index, (start, end) ->
            val lineColor = when (index) {
                2, 3 -> if (!backStraight) Color.Red else primaryColor
                else -> primaryColor
            }

            drawLine(
                color = lineColor,
                start = Offset(start.first, start.second),
                end = Offset(end.first, end.second),
                strokeWidth = 6f
            )
        }
    }
}


@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    isCountingDown: () -> Boolean,
    isTimerRunning: () -> Boolean,
    cameraExecutor: ExecutorService,
    onDataUpdate: (Int, Double, Double, List<Pair<Float, Float>>, Boolean) -> Unit
) {
    val previewView = remember { PreviewView(context) }


    // Runs startCamera once
    LaunchedEffect(Unit) {
        startCamera(previewView, context, lifecycleOwner, cameraExecutor, isCountingDown, isTimerRunning, onDataUpdate)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { previewView }
    )
}

private fun startCamera(
    previewView: PreviewView,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    isCountingDown: () -> Boolean,
    isTimerRunning: () -> Boolean,
    onDataUpdate: (Int, Double, Double, List<Pair<Float, Float>>, Boolean) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        try {
            // Unbind all use cases before binding again
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
                        processPushUpDetection(imageProxy, context, isCountingDown, isTimerRunning) { newCount, newElbowAngle, newHipAngle, newKeyPoints, backStraight ->
                            // Always update state, but only count push-ups after countdown ends
                            onDataUpdate(newCount, newElbowAngle, newHipAngle, newKeyPoints, backStraight)
                        }
                    }
                }


            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            // ✅ Bind the new use cases safely
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)

        } catch (e: Exception) {
            Log.e("CameraDebug", "Failed to start camera: ${e.message}")
        }

    }, ContextCompat.getMainExecutor(context))
}



private var pushUpCount = 0  // ✅ Persist push-up count across frames
private var isGoingDown = false  // ✅ Track downward movement
private var wasGoingDown = false  // ✅ Prevent multiple counts
private var backStraight = false
private var mediaPlayer: MediaPlayer? = null
private var mediaPlayerBack: MediaPlayer? = null
private var lastBackWarningTime = 0L  // To prevent frequent playback


@OptIn(ExperimentalGetImage::class)
private fun processPushUpDetection(
    imageProxy: ImageProxy,
    context: Context,
    isCountingDown: () -> Boolean,
    isTimerRunning: () -> Boolean,
    onDataUpdate: (Int, Double, Double, List<Pair<Float, Float>>, Boolean) -> Unit  // ✅ Send keypoints
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        //Create instance of PoseDetector
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        val poseDetector = PoseDetection.getClient(options)

        poseDetector.process(image)
            .addOnSuccessListener { pose ->
                val landmarks = pose.allPoseLandmarks
                if (landmarks.isNotEmpty()) {
                    val elbowAngle = getAngle(landmarks, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
                    val hipAngle = getAngle(landmarks, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
                    val backAngle = getAngle(
                        landmarks, PoseLandmark.LEFT_SHOULDER,  // A
                        PoseLandmark.LEFT_HIP,       // B (Vertex)
                        PoseLandmark.LEFT_KNEE       // C
                    )

                    val legAngle = getAngle(
                        landmarks,
                        PoseLandmark.LEFT_HIP,       // A
                        PoseLandmark.LEFT_KNEE,      // B (Vertex)
                        PoseLandmark.LEFT_ANKLE      // C
                    )

                    // Extract keypoint positions for skeleton overlay
                    val keyPoints = listOf(
                        PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
                        PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
                        PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
                        PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
                        PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE
                        ).mapNotNull { (start, end) ->
                        val startPoint = landmarks.find { it.landmarkType == start }?.position
                        val endPoint = landmarks.find { it.landmarkType == end }?.position
                        if (startPoint != null && endPoint != null) {
                            listOf(Pair(startPoint.x, startPoint.y), Pair(endPoint.x, endPoint.y))
                        } else {
                            null
                        }
                    }.flatten()




                    // Check for back straightness
                    backStraight = backAngle in 140.0..200.0 && legAngle in 160.0..200.0

                    // ✅ Check dynamically if countdown is still running
                    if (!isCountingDown() && isTimerRunning()) {
                        if (elbowAngle < 100 && hipAngle > 140) {
                            isGoingDown = true
                            wasGoingDown = true
                        }

                        if (wasGoingDown && isGoingDown && elbowAngle > 145 && hipAngle > 140 && backStraight) {
                            pushUpCount++
                            isGoingDown = false
                            wasGoingDown = false
                            playCorrectSound(context)

                        }

                        // Play "Straighten Back" sound if back is NOT straight (limit repeats)
                        if (!backStraight && System.currentTimeMillis() - lastBackWarningTime > 3000) {
                            lastBackWarningTime = System.currentTimeMillis()
                            playBackWarningSound(context)
                        }
                    }



                    // ✅ Send push-up count, angles, and skeleton keypoints to UI
                    onDataUpdate(pushUpCount, elbowAngle, hipAngle, keyPoints, backStraight)
                }
            }
            .addOnFailureListener { e ->
                Log.e("PushupTracker", "Pose Detection Failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}

private fun playCorrectSound(context: Context) {
    if (mediaPlayer == null) {
        mediaPlayer = MediaPlayer.create(context, R.raw.correct) // Ensure correct.mp3 is in res/raw
    }
    mediaPlayer?.start()
}

private fun playBackWarningSound(context: Context) {
    if (mediaPlayerBack == null) {
        mediaPlayerBack = MediaPlayer.create(context, R.raw.back) // Ensure straighten_back.mp3 is in res/raw
    }
    mediaPlayerBack?.start()
}




//Get angle between 3 joints
private fun getAngle(landmarks: List<PoseLandmark>, p1: Int, p2: Int, p3: Int): Double {
    val point1 = landmarks.find { it.landmarkType == p1 }?.position ?: return 0.0
    val point2 = landmarks.find { it.landmarkType == p2 }?.position ?: return 0.0
    val point3 = landmarks.find { it.landmarkType == p3 }?.position ?: return 0.0

    // Adjust for Mirrored Camera
    val correctedX1 = -point1.x
    val correctedX2 = -point2.x
    val correctedX3 = -point3.x

    val angle = Math.toDegrees(
        atan2((point3.y - point2.y).toDouble(), (correctedX3 - correctedX2).toDouble()) -
                atan2((point1.y - point2.y).toDouble(), (correctedX1 - correctedX2).toDouble())
    )

    return if (angle < 0) angle + 360 else angle
}



@Composable
fun PushUpOutline(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)  // Add padding for better spacing
    ) {
        val strokeWidth = 8f
        val outlineColor = Color.White.copy(alpha = 0.7f)  // Higher visibility

        // Define body proportions
        val headRadius = 150f
        val bodyStart = Offset(size.width * 0.82f, size.height * 0.35f) // Shoulder
        val bodyEnd = Offset(size.width * 0.10f, size.height * 0.6f) // Hip

        // Draw Head (circle)
        drawCircle(
            color = outlineColor,
            radius = headRadius,
            center = Offset(
                size.width * 0.90f,
                size.height * 0.35f
            ) // Head slightly above shoulders
        )

        // Draw Body (shoulders to hips)
        drawLine(
            color = outlineColor,
            start = bodyStart,
            end = bodyEnd,
            strokeWidth = strokeWidth
        )

        // Draw Arms (shoulders to ground)
        val leftHand = Offset(size.width * 0.80f, size.height * 0.85f)
        drawLine(
            color = outlineColor,
            start = bodyStart, // Shoulder position
            end = leftHand,    // Hand on ground
            strokeWidth = strokeWidth
        )


    }
}