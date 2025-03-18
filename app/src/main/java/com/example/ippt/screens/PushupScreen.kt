    package com.example.ippt.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.ippt.R

@Composable
fun PushupScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title at the top
        Text(
            text = "Push-Up Tracker\n",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 32.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Evenly spaced buttons (matching HomeScreen)
        Spacer(modifier = Modifier.weight(1f))
        GradientButton(
            imageRes = R.drawable.ic_push_up,
            text = "Start Tracking",
            onClick = { navController.navigate("pushup_tracker") }
        )

        Spacer(modifier = Modifier.weight(1f))
        GradientButton(
            imageRes = R.drawable.ic_ar_pushup,
            text = "View in AR",
            onClick = { navController.navigate("ar_view") }
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun PushUpGradientButton(imageRes: Int, text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = text,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 22.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PushupScreenPreview() {
    PushupScreen(navController = rememberNavController())
}
