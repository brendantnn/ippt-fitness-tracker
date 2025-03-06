package com.example.ippt.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

//Refined Orange/Yellow Sports Theme
val OrangePrimary = Color(0xFFFF9800) // Vibrant Orange
val GoldenYellow = Color(0xFFFFC107) // Accent Yellow
val DeepOrange = Color(0xFFF57C00) // Darker Orange
val DarkText = Color(0xFF5D4037) // Dark Brown text
val LightBackground = Color(0xFFFFF8E1) //Softer Off-White Background
val WhiteText = Color.White

//Dark Mode Adjustments
val DarkBackground = Color(0xFF212121) // Dark Gray
val DarkSurface = Color(0xFF424242) // Slightly lighter gray

// Light Mode Theme
private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    secondary = GoldenYellow,
    background = Color.White,
    surface = Color.White,
    onPrimary = WhiteText,
    onSecondary = WhiteText,
    onBackground = DarkText,
    onSurface = DarkText
)


//Dark Mode Theme
private val DarkColorScheme = darkColorScheme(
    primary = DeepOrange,
    secondary = OrangePrimary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = WhiteText,
    onSecondary = WhiteText,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun IPPTTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, //Disable dynamic colors
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
