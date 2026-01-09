package com.example.selftalker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.selftalker.datastore.AlarmPreferences
import com.example.selftalker.data.local.ScheduleDatabase
import com.example.selftalker.utils.ScheduleManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val backgroundColor = Color(0xFF05655F)
    val coroutineScope = rememberCoroutineScope()

    val alarmsEnabled by AlarmPreferences.alarmsEnabledFlow(context)
        .collectAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF43AAA7), Color(0xFF29909B))
                )
            )
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, RoundedCornerShape(20.dp))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = backgroundColor
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Settings", fontSize = 22.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Alarm Toggle
        SettingsItem(
            title = "All Schedule Alarms",
            icon = Icons.Default.Notifications,
            checked = alarmsEnabled,
            onCheckedChange = { enabled ->
                coroutineScope.launch {
                    AlarmPreferences.setAlarmsEnabled(context, enabled)

                    if (enabled) {
                        val dao = ScheduleDatabase.getInstance(context).scheduleDao()
                        ScheduleManager.rescheduleAllAlarms(context, dao)
                    } else {
                        val dao = ScheduleDatabase.getInstance(context).scheduleDao()
                        ScheduleManager.cancelAllAlarms(context, dao)
                    }
                }
            },
            color = backgroundColor
        )

        // About section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = backgroundColor)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("About App", fontSize = 18.sp, color = Color.Black)
                    Text("Version 1.0.0", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontSize = 18.sp, color = Color.Black, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = color
                )
            )
        }
    }
}
