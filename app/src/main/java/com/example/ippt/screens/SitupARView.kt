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
fun SitupARView(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            launchSitupViewer(navController.context)
        }) {
            Text("View 3D Model in AR")
        }
    }
}

fun launchSitupViewer(context: Context) {
    val modelUrl = "https://drive.usercontent.google.com/u/0/uc?id=1bOUNNEVAgPsRSPbK8qzA7gFjaC36u3xU&export=download" // Replace with your actual file ID

    val intentUri = Uri.parse("https://arvr.google.com/scene-viewer/1.0")
        .buildUpon()
        .appendQueryParameter("file", modelUrl)
        .appendQueryParameter("mode", "ar_preferred")
        .build()

    Log.d("ARView", "Scene Viewer Intent URI: $intentUri")

    val sceneViewerIntent = Intent(Intent.ACTION_VIEW).apply {
        data = intentUri
        setPackage("com.google.android.googlequicksearchbox") // Scene Viewer package
    }

    try {
        context.startActivity(sceneViewerIntent)
        Log.d("ARView", "Scene Viewer launched successfully")
    } catch (e: ActivityNotFoundException) {
        Log.e("ARView", "Scene Viewer not found on device", e)
        Toast.makeText(context, "Google Scene Viewer not installed", Toast.LENGTH_SHORT).show()
    }
}
