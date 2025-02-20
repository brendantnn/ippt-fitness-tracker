package com.example.ippt.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.atan2

@Composable
fun PushupTrackerScreen(navController: NavController) {
    val context = LocalContext.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var isCameraOn by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var pushUpCount by remember { mutableStateOf(0) }
    var elbowAngle by remember { mutableStateOf(0.0) }
    var hipAngle by remember { mutableStateOf(0.0) }
    var keyPoints by remember { mutableStateOf(emptyList<Pair<Float, Float>>()) }

    // Request camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            isCameraOn = true  // Add this line
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
            isCameraOn = true

        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            if (isCameraOn) {
                val lifecycleOwner = LocalLifecycleOwner.current
                CameraPreview(context, lifecycleOwner, cameraExecutor) { newCount, newElbowAngle, newHipAngle, newKeyPoints ->
                    pushUpCount = newCount
                    elbowAngle = newElbowAngle
                    hipAngle = newHipAngle
                    keyPoints = newKeyPoints
                }
            }
        } else {
            Text("Camera permission is required to use this feature.", modifier = Modifier.align(Alignment.Center))
        }

        // ✅ Display Push-Up Count
        Text(
            text = "Push-ups: $pushUpCount",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp)
        )

        // ✅ Display Elbow and Hip Angles
        Column(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Text(text = "Elbow Angle: ${elbowAngle.toInt()}°", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Hip Angle: ${hipAngle.toInt()}°", style = MaterialTheme.typography.bodyLarge)
        }

        // ✅ Draw Keypoints (Joints)
        keyPoints.forEach { (x, y) ->
            Box(
                modifier = Modifier
                    .offset(x.dp, y.dp)
                    .size(10.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }

        DrawSkeletonLines(keyPoints, lineColor = MaterialTheme.colorScheme.secondary)


    }
}

@Composable
fun DrawSkeletonLines(keyPoints: List<Pair<Float, Float>>, lineColor: androidx.compose.ui.graphics.Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        keyPoints.chunked(2).forEach { (start, end) ->
            drawLine(
                color = lineColor,
                start = Offset(start.first, start.second),
                end = Offset(end.first, end.second),
                strokeWidth = 5f
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
    onDataUpdate: (Int, Double, Double, List<Pair<Float, Float>>) -> Unit  // ✅ Pass keypoints
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
    onDataUpdate: (Int, Double, Double, List<Pair<Float, Float>>) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
            it.setAnalyzer(cameraExecutor) { imageProxy ->
                processPushUpDetection(imageProxy, context, onDataUpdate)
            }
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)  // ✅ Switch to front camera
            .build()

        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
    }, context.mainExecutor)
}



private var pushUpCount = 0  // ✅ Persist push-up count across frames
private var isGoingDown = false  // ✅ Track downward movement
private var wasGoingDown = false  // ✅ Prevent multiple counts

private fun processPushUpDetection(
    imageProxy: ImageProxy,
    context: Context,
    onDataUpdate: (Int, Double, Double, List<Pair<Float, Float>>) -> Unit  // ✅ Send keypoints
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

                    // ✅ Extract keypoint positions for skeleton overlay
                    val keyPoints = listOf(
                        PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
                        PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
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

                    // ✅ Improved Push-Up Counting Logic
                    if (elbowAngle < 90 && hipAngle > 160) {
                        isGoingDown = true  // ✅ Detect downward motion
                        wasGoingDown = true
                    }

                    // ✅ Count Only When Going Back Up After Fully Going Down
                    if (wasGoingDown && isGoingDown && elbowAngle > 160 && hipAngle > 160) {
                        pushUpCount++  // ✅ Increment only after a full rep
                        isGoingDown = false  // ✅ Reset for next push-up
                        wasGoingDown = false  // ✅ Prevent multiple counts
                        Log.d("PushupTracker", "Push-up Count Increased: $pushUpCount")  // ✅ Debugging Log
                    }

                    // ✅ Send push-up count, angles, and skeleton keypoints to UI
                    onDataUpdate(pushUpCount, elbowAngle, hipAngle, keyPoints)
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