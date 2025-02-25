package com.example.ippt.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.atan2

@Composable
fun PushupTrackerScreen(navController: NavController) {
    val context = LocalContext.current
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
    var pushUpCount by remember { mutableStateOf(0) }
    var elbowAngle by remember { mutableStateOf(0.0) }
    var backAngle by remember { mutableStateOf(0.0) }
    var legAngle by remember { mutableStateOf(0.0) }
    var hipAngle by remember { mutableStateOf(0.0) }
    var keyPoints by remember { mutableStateOf(emptyList<Pair<Float, Float>>()) }
    var isUserReady by remember { mutableStateOf(false) }
    var isBackStraight by remember { mutableStateOf(true) }

    // New state for countdown timer
    var countdownTimer by remember { mutableStateOf(3) }
    var isCountingDown by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // ✅ Request camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            isCameraOn = true
        }
    }

    LaunchedEffect(Unit) {
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
    }

    // ✅ Function to start countdown
    fun startCountdown() {
        isCountingDown = true
        coroutineScope.launch {
            for (i in 5 downTo 1) {
                countdownTimer = i
                delay(1000L)  // 1-second delay
            }
            countdownTimer = 0
            isCountingDown = false
        }
    }



    // ✅ Main UI
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            if (isCameraOn) {
                val lifecycleOwner = LocalLifecycleOwner.current

                // ✅ Camera Preview
                CameraPreview(context, lifecycleOwner, cameraExecutor) { newCount, newElbowAngle, newHipAngle, newKeyPoints, backStraight ->
                    if (isUserReady) {
                        pushUpCount = newCount
                        elbowAngle = newElbowAngle
                        hipAngle = newHipAngle
                        keyPoints = newKeyPoints
                        isBackStraight = backStraight
                    } else {
                        // ✅ Detect push-up position
                        if (newElbowAngle in 160.0..200.0 && newHipAngle in 160.0..200.0) {
                            isUserReady = true
                            startCountdown()  // Start countdown once aligned
                        }
                    }
                }

                // UI for before pushup position
                if (!isUserReady) {
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

                        // ✅ Show Push-Up Outline overlay
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
                }

                // UI for before pushup position
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

                        // ✅ Show Push-Up Outline overlay
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



                // Start counting pushups after timer
                if (isUserReady && countdownTimer == 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.3f)
                                .height(80.dp)
                                .background(Color.Green.copy(alpha = 0.8f))
                                //.align(Alignment.TopCenter)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Push-ups: $pushUpCount",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.7f)
                                .height(80.dp)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                                //.align(Alignment.TopCenter)
                                .padding(10.dp)
                        ) {
                            DrawSkeletonLines(
                                keyPoints = keyPoints,
                                backStraight = isBackStraight,
                                legStraight = legAngle in 160.0..200.0,
                                boxSize = 200f
                            )
                        }


                    }

                    // ✅ Warning if back or legs aren't straight
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


        } else {
            // ✅ Show permission required message
            Text("Camera permission is required to use this feature.", modifier = Modifier.align(Alignment.Center))
        }


        }

    }
}


@Composable
fun DrawSkeletonLines(
    keyPoints: List<Pair<Float, Float>>,
    backStraight: Boolean,
    legStraight: Boolean,
    boxSize: Float // Controls the overall size of the skeleton
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (keyPoints.isEmpty()) return@Canvas

        // Find min and max points for scaling
        val minX = keyPoints.minOf { it.first }
        val maxX = keyPoints.maxOf { it.first }
        val minY = keyPoints.minOf { it.second }
        val maxY = keyPoints.maxOf { it.second }

        val widthRange = maxX - minX
        val heightRange = maxY - minY

        // Increase scale factor to enlarge the skeleton
        val scaleMultiplier = 1.5f  // Increase this value for a bigger skeleton
        val scale = scaleMultiplier * if (widthRange > heightRange) {
            boxSize / widthRange
        } else {
            boxSize / heightRange
        }

        // Center offsets to position the skeleton
        val centerX = size.width / 2
        val centerY = size.height / 2

        // Normalize and scale keypoints
        val normalizedKeyPoints = keyPoints.map { (x, y) ->
            val scaledX = ((x - minX) * scale) - (widthRange * scale / 2)
            val scaledY = ((y - minY) * scale) - (heightRange * scale / 2)
            Pair(centerX + scaledX, centerY + scaledY)
        }

        // Draw skeleton lines with conditional coloring
        normalizedKeyPoints.chunked(2).forEachIndexed { index, (start, end) ->
            val lineColor = when (index) {
                2 -> if (!backStraight) Color.Red else Color.Gray  // 🔴 Red for back issues
                3 -> if (!backStraight) Color.Red else Color.Gray   // 🔴 Red for leg issues
                else -> Color.Gray  // ⚪ Default color for other joints
            }

            drawLine(
                color = lineColor,
                start = Offset(start.first, start.second),
                end = Offset(end.first, end.second),
                strokeWidth = 6f  // Thicker lines for better visibility
            )
        }
    }
}







@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    onDataUpdate: (Int, Double, Double, List<Pair<Float, Float>>, Boolean) -> Unit  // ✅ Pass keypoints
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            startCamera(previewView, context, lifecycleOwner, cameraExecutor, onDataUpdate)
            previewView
        }
    )
}



private fun startCamera(
    previewView: PreviewView,
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    onDataUpdate: (Int, Double, Double, List<Pair<Float, Float>>, Boolean) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9).  // ✅ Force landscape aspect ratio
        build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
            it.setAnalyzer(cameraExecutor) { imageProxy ->
                processPushUpDetection(imageProxy, context, onDataUpdate)
            }
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
    }, context.mainExecutor)
}



private var pushUpCount = 0  // ✅ Persist push-up count across frames
private var isGoingDown = false  // ✅ Track downward movement
private var wasGoingDown = false  // ✅ Prevent multiple counts
private var backStraight = false

private fun processPushUpDetection(
    imageProxy: ImageProxy,
    context: Context,
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

                    if (backAngle in 160.0..200.0 && legAngle in 160.0..200.0) {
                        backStraight = true
                    } else {
                        backStraight = false
                    }

                    // ✅ Improved Push-Up Counting Logic
                    if (elbowAngle < 90 && hipAngle > 160) {
                        isGoingDown = true  // ✅ Detect downward motion
                        wasGoingDown = true
                    }

                    // ✅ Count Only When Going Back Up After Fully Going Down
                    if (wasGoingDown && isGoingDown && elbowAngle > 160 && hipAngle > 160 && backStraight) {
                        pushUpCount++  // ✅ Increment only after a full rep
                        isGoingDown = false  // ✅ Reset for next push-up
                        wasGoingDown = false  // ✅ Prevent multiple counts
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






//Get angle between 3 joints
private fun getAngle(landmarks: List<PoseLandmark>, p1: Int, p2: Int, p3: Int): Double {
    val point1 = landmarks[p1].position
    val point2 = landmarks[p2].position
    val point3 = landmarks[p3].position

    val angle = Math.toDegrees(
        atan2((point3.y - point2.y).toDouble(), (point3.x - point2.x).toDouble()) -
                atan2((point1.y - point2.y).toDouble(), (point1.x - point2.x).toDouble())
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
            center = Offset(size.width * 0.90f, size.height * 0.35f) // Head slightly above shoulders
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




// Capture image from camera, converts it into InputImage format, then use ML kit's Pose Detection API to process frames,
private fun processImageProxy(imageProxy: ImageProxy, context: Context) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        val poseDetector = PoseDetection.getClient(options)

        poseDetector.process(image)
            .addOnSuccessListener { pose ->
                // Implement push-up counting logic
                Log.d("PushupScreen", "Pose Detected: ${pose.allPoseLandmarks}")
            }
            .addOnFailureListener { e ->
                Log.e("PushupScreen", "Pose Detection Failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}

fun setScreenOrientation(activity: Activity?, orientation: Int) {
    activity?.requestedOrientation = orientation
}