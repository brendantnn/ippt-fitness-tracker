package com.example.ippt.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun PushupARView(navController: NavController) {
    // UI layout for displaying a button to launch the AR model viewer
    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            launchSceneViewer(navController.context)
        }) {
            Text("View 3D Model in AR")
        }
    }
}

/**
 * Function to launch Google Scene Viewer with a 3D model.
 * If Scene Viewer is not installed, redirects the user to the Play Store.
 */
fun launchSceneViewer(context: Context) {
    val modelUrl = "https://drive.google.com/uc?export=download&id=1d_DG6_on4Remq22-BW0sqvy54ooV09Kj" // URL to the 3D model

    val intentUri = Uri.parse("https://arvr.google.com/scene-viewer/1.0")
        .buildUpon()
        .appendQueryParameter("file", modelUrl) // Model file location
        .appendQueryParameter("mode", "ar_preferred") // Prefer AR mode
        .build()

    Log.d("ARView", "Scene Viewer Intent URI: $intentUri")

    val sceneViewerIntent = Intent(Intent.ACTION_VIEW).apply {
        data = intentUri
        setPackage("com.google.android.googlequicksearchbox") // Package name for Scene Viewer
    }

    try {
        // Attempt to start Scene Viewer
        context.startActivity(sceneViewerIntent)
        Log.d("ARView", "Scene Viewer launched successfully")
    } catch (e: ActivityNotFoundException) {
        // If Scene Viewer is not found, notify user and redirect to Play Store
        Log.e("ARView", "Scene Viewer not found on device", e)
        Toast.makeText(context, "Google Scene Viewer not installed", Toast.LENGTH_SHORT).show()
        redirectToPlayStore(context)
    }
}

/**
 * Redirects the user to the Play Store to install Google Scene Viewer.
 */
fun redirectToPlayStore(context: Context) {
    val playStoreUri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox")
    val playStoreIntent = Intent(Intent.ACTION_VIEW, playStoreUri)
    try {
        context.startActivity(playStoreIntent)
    } catch (e: ActivityNotFoundException) {
        // If Play Store cannot be opened, notify the user
        Toast.makeText(context, "Unable to open Play Store", Toast.LENGTH_SHORT).show()
    }
}
