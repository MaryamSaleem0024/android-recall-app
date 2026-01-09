package com.example.selftalker.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.selftalker.R
import com.example.selftalker.RecordActivity
import com.example.selftalker.navigation.Screens

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current

    val gradientStart = Color(0xFF43AAA7)
    val gradientEnd = Color(0xFF29909B)


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(gradientStart, gradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Logo
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(240.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Welcome Text
            Text(
                text = "Welcome to\nSelf-Talk",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons
            HomeButton(
                icon = Icons.Default.Mic,
                text = "Record Sound Bite",
                onClick = {
                    context.startActivity(Intent(context, RecordActivity::class.java))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeButton(
                icon = Icons.Default.Folder,
                text = "My Sound Library",
                onClick = {
                    navController.navigate(Screens.Library.route)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeButton(
                icon = Icons.Default.AccessTime,
                text = "Set Playback Timings",
                onClick = {
                    navController.navigate("${Screens.ScheduleSummary.route}?new_schedule=null")
                }
            )
        }
    }
}

@Composable
fun HomeButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.15f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 18.sp, color = Color.White)
    }
}
