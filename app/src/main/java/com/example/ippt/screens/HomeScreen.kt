package com.example.ippt.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ippt.R

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Greeting Text at the top
        Text(
            text = "Welcome to IPPT Fitness Tracker!",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 32.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Push up
        Spacer(modifier = Modifier.weight(1f))
        GradientButton(
            imageRes = R.drawable.ic_push_upv2,
            text = "Push-Up Tracker",
            onClick = { navController.navigate("pushup") }
        )
        //Sit up
        Spacer(modifier = Modifier.weight(1f))
        GradientButton(
            imageRes = R.drawable.ic_sit_up, // Replace with the actual sit-up image resource
            text = "Sit-Up Tracker",
            onClick = { navController.navigate("sit-up") }
        )
        //Running
        Spacer(modifier = Modifier.weight(1f))
        GradientButton(
            imageRes = R.drawable.ic_run,
            text = "Run Tracker",
            onClick = { navController.navigate("run") }
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavBar() {
    CenterAlignedTopAppBar(
        title = { Text("IPPT Fitness App") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

//Gradient Button Design
@Composable
fun GradientButton(imageRes: Int, text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(10.dp, shape = RoundedCornerShape(12.dp)) //shadow for depth
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

//  Bottom Navigation Bar
@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        NavItem("Home", "home", Icons.Filled.Home),
        NavItem("Pushups", "pushup", null), // Pushups now uses a custom image
        NavItem("Situps", "sit-up", null),  // Situps also uses a custom image
        NavItem("Run", "run", Icons.Filled.DirectionsRun)
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.primary) {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    when (item.title) {
                        "Pushups" -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_pushup_logo), // Custom pushup icon
                                contentDescription = item.title,
                                modifier = Modifier.size(25.dp), // Adjust size for Pushup
                                tint = LocalContentColor.current
                            )
                        }
                        "Situps" -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_situp_logo), // Custom situp icon
                                contentDescription = item.title,
                                modifier = Modifier.size(25.dp), // Adjust size for Situp
                                tint = LocalContentColor.current
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = item.icon!!,
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp) // Default size for other icons
                            )
                        }
                    }
                },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = { navController.navigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}


// Update NavItem to support both ImageVector and Painter
data class NavItem(
    val title: String,
    val route: String,
    val icon: ImageVector? // Allow nullable type
)




//data class NavItem(val title: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}
