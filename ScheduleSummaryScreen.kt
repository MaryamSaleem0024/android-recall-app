package com.example.selftalker.screens

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import com.example.selftalker.data.local.entity.AudioEntity
import com.example.selftalker.data.local.entity.ScheduleEntity
import com.example.selftalker.navigation.Screens
import com.example.selftalker.utils.PermissionUtils
import com.example.selftalker.model.ScheduleViewModel
import com.example.selftalker.receiver.AudioPlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ScheduleSummaryScreen(
    viewModel: ScheduleViewModel,
    onEditClick: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val showCard = remember {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<Boolean>("show_card") == true
    }

    var visible by remember { mutableStateOf(showCard) }

    // Hide success card after 2.5s
    LaunchedEffect(showCard) {
        if (showCard) {
            delay(2500)
            visible = false
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("show_card", false)
        }
    }

    var showDeleteCard by remember { mutableStateOf(false) }
    val allSchedulesWithAudios by viewModel.schedules.collectAsState()


    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val gradientBrush = Brush.verticalGradient(
        listOf(Color(0xFF43AAA7), Color(0xFF29909B))
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    contentColor = Color.White,
                    snackbarData = data
                )
            }
        }
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    IconButton(
                        onClick = { navController.navigate(Screens.Home.route) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, shape = RoundedCornerShape(20.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF05655F)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Scheduled Audio",
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }

                when {
                    visible -> {
                        NewScheduleCard()
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    showDeleteCard -> {
                        DeletedScheduleCard()
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(allSchedulesWithAudios) { scheduleWithAudios ->
                        ScheduleCard(schedule = scheduleWithAudios.schedule,
                            audios = scheduleWithAudios.audios, onDelete = {
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()

                                val stopIntent = Intent(context, AudioPlaybackService::class.java).apply {
                                    action = AudioPlaybackService.ACTION_STOP
                                }
                                context.startService(stopIntent)

                                viewModel.deleteScheduleAndCancelAlarm(context, scheduleWithAudios.schedule)
                                showDeleteCard = true
                                delay(2500)
                                showDeleteCard = false
                            }
                        })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (!PermissionUtils.hasExactAlarmPermission(context)) {
                            PermissionUtils.requestExactAlarmPermission(context)
                        } else {
                            onEditClick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A848B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add New Schedule", color = Color.White)
                }
            }
        }
    }
}


@Composable
private fun ScheduleCard(
    schedule: ScheduleEntity,
    audios: List<AudioEntity>,
    onDelete: () -> Unit
) {
    val time = remember(schedule.timeMillis) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(schedule.timeMillis))
    }

    val repeatText = remember(schedule.repeatIntervalMillis) {
        when {
            schedule.repeatIntervalMillis == 24 * 60 * 60 * 1000L -> "Daily"
            schedule.repeatIntervalMillis % (60 * 60 * 1000) == 0L -> {
                val hours = schedule.repeatIntervalMillis / (60 * 60 * 1000)
                "Every ${hours}h"
            }
            else -> {
                val totalMinutes = schedule.repeatIntervalMillis / (60 * 1000)
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                if (hours > 0) "Every ${hours}h ${minutes}m" else "Every ${minutes}m"
            }
        }
    }

    val fileListText = audios.joinToString("\n• ", prefix = "• ") { it.fileName }
    val summary = "From: $time\nRepeat: $repeatText\n$fileListText"
    //
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
            title = { Text("Delete Schedule?") },
            text = { Text("Are you sure you want to delete this schedule? This action cannot be undone.") }
        )
    }
    //
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = summary,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp
            )

            IconButton(onClick = { confirmDelete = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }
    }
}


@Composable
private fun NewScheduleCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDFF5EB)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("✅ New Schedule Added Successfully", color = Color(0xFF05655F), fontSize = 16.sp)
        }
    }
}

@Composable
private fun DeletedScheduleCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E1)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("❌ Schedule Deleted Successfully", color = Color(0xFFD32F2F), fontSize = 16.sp)
        }
    }
}
