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
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


@Composable
fun ARView(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            val modelUri = copyModelToFiles(navController.context, "cube.glb")
            if (modelUri != null) {
                launchSceneViewer(navController.context, modelUri) // Pass modelUri directly
            } else {
                Toast.makeText(navController.context, "Model not found", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("View 3D Model in AR")
        }
    }
}

fun copyModelToFiles(context: Context, fileName: String): Uri? {
    val filesDir = File(context.filesDir, fileName)

    if (filesDir.exists()) filesDir.delete() // Ensure a fresh copy
    filesDir.parentFile?.mkdirs()

    return try {
        context.assets.open(fileName).use { inputStream ->
            FileOutputStream(filesDir).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        Log.d("ARView", "Model copied to files: ${filesDir.absolutePath}")

        // Use FileProvider to create a content:// URI
        val modelUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Make sure this is in your AndroidManifest
            filesDir
        )
        modelUri
    } catch (e: IOException) {
        Log.e("ARView", "Error copying model: ${e.message}", e)
        null
    }
}

fun launchSceneViewer(context: Context, modelUri: Uri?) {
    if (modelUri == null) {
        Log.e("ARView", "Model URI is null")
        Toast.makeText(context, "3D Model not found", Toast.LENGTH_SHORT).show()
        return
    }

    val encodedUri = Uri.encode(modelUri.toString())
    val intentUri = Uri.parse("https://arvr.google.com/scene-viewer/1.0")
        .buildUpon()
        .appendQueryParameter("file", encodedUri)
        .appendQueryParameter("mode", "ar_preferred")
        .build()


    Log.d("ARView", "Encoded Scene Viewer URI: $encodedUri")
    Log.d("ARView", "Scene Viewer Intent URI: $intentUri")

    val sceneViewerIntent = Intent(Intent.ACTION_VIEW).apply {
        data = intentUri
        setPackage("com.google.android.googlequicksearchbox") // Scene Viewer package
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Allow reading the file
    }

    try {
        context.startActivity(sceneViewerIntent)
        Log.d("ARView", "Scene Viewer launched successfully")
    } catch (e: ActivityNotFoundException) {
        Log.e("ARView", "Scene Viewer not found on device", e)
        Toast.makeText(context, "Google Scene Viewer not installed", Toast.LENGTH_SHORT).show()
    }
}
