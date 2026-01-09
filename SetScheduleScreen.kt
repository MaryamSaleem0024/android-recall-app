package com.example.selftalker.screens

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.selftalker.data.local.ScheduleDatabase
import com.example.selftalker.data.local.entity.ScheduleEntity
import com.example.selftalker.navigation.Screens
import com.example.selftalker.model.ScheduleViewModel
import com.example.selftalker.model.ScheduleViewModelFactory
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.Locale

fun willOverlap(newSchedule: ScheduleEntity, existingSchedules: List<ScheduleEntity>): Boolean {
    for (existing in existingSchedules) {
        if (existing.id == newSchedule.id) continue  // Skip self if editing

        if (existing.repeatIntervalMillis == newSchedule.repeatIntervalMillis) {
            continue // ✅ Skip same repeat intervals (already handled by onConflict)
        }

        val gcd = gcd(existing.repeatIntervalMillis, newSchedule.repeatIntervalMillis)
        val diff = kotlin.math.abs(existing.timeMillis - newSchedule.timeMillis)

        if (gcd > 0 && diff % gcd == 0L) {
            return true // ⛔ Conflict found
        }
    }
    return false
}

fun gcd(a: Long, b: Long): Long {
    return if (b == 0L) a else gcd(b, a % b)
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SetScheduleScreen(navController: NavController) {
    val context = LocalContext.current
    val gradientBrush = Brush.verticalGradient(
        listOf(Color(0xFF43AAA7), Color(0xFF29909B))
    )

    val audioDir = remember {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "SelfTalker"
        ).apply {
            if (!exists()) mkdirs()
        }
    }

    var currentFolder by remember { mutableStateOf(audioDir) }
    var subFiles by remember { mutableStateOf(listOf<File>()) }
    val selectedFiles = remember { mutableStateListOf<File>() }
    var showFileExplorer by remember { mutableStateOf(false) }
    var showOverlapDialog by remember { mutableStateOf(false) }
    var scheduleToSave by remember { mutableStateOf<ScheduleEntity?>(null) }


    fun updateSubFiles() {
        subFiles = currentFolder.listFiles()?.filter {
            it.isDirectory || it.extension.lowercase() in listOf("mp3", "wav", "m4a")
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
    }

    LaunchedEffect(currentFolder) {
        updateSubFiles()
    }

    val calendar = remember { Calendar.getInstance() }
    var hour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

    val formattedHour = String.format(Locale.getDefault(), "%02d", hour)
    val formattedMinute = String.format(Locale.getDefault(), "%02d", minute)
    val amPm = if (hour >= 12) "PM" else "AM"

    val scheduleViewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModelFactory(ScheduleDatabase.getInstance(context).scheduleDao())
    )

    var repeatMode by remember { mutableStateOf("Minutes") }
    var repeatMinutes by remember { mutableStateOf("1") }
    var repeatHours by remember { mutableStateOf("0") }


    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    var isHourSelectedManually by remember { mutableStateOf(false) }

    var showReplaceDialog by remember { mutableStateOf(false) }
    var conflictSchedule by remember { mutableStateOf<ScheduleEntity?>(null) }
    var onReplaceConfirmed: (() -> Unit)? by remember { mutableStateOf(null) }


    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF333333),
                    contentColor = Color.White,
                )
            }
        }
    ) { _ ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, shape = RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF05655F)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Set Schedule", color = Color.White, fontSize = 22.sp)
            }

            Spacer(Modifier.height(16.dp))
            Text("Select Sound Bite", color = Color(0xFF014D4D), fontSize = 20.sp)

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { showFileExplorer = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D6D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (selectedFiles.isNotEmpty())
                        "${selectedFiles.size} file(s) selected"
                    else "Select Sound Bites"
                )
            }

            if (selectedFiles.isNotEmpty())
            {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Repeat Daily", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = repeatMode == "Daily",
                        onCheckedChange = { isChecked ->
                            repeatMode = if (isChecked) "Daily" else "Minutes"
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF05655F)
                        )
                    )
                }

                if (repeatMode == "Daily") {
                    Spacer(Modifier.height(32.dp))
                    Text("Set Time", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val timeDisplay = "$formattedHour:$formattedMinute $amPm"
                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, selectedHour, selectedMinute ->
                                    hour = selectedHour
                                    minute = selectedMinute
                                },
                                hour, minute, false
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("Time: $timeDisplay", color = Color(0xFF05655F))
                    }
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Repeat Every", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    val hourItems = (0..23).map { it.toString().padStart(2, '0') }
                    val minuteItems = remember(repeatHours) {
                        if (repeatHours == "00") {
                            (1..59).map { it.toString().padStart(2, '0') }
                        } else {
                            (0..59).map { it.toString().padStart(2, '0') }
                        }
                    }


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hours", color = Color(0xFFFFD54F), fontSize = 16.sp)
                            SpinnerPicker(
                                items = hourItems,
                                selectedItem = repeatHours.padStart(2, '0'),
                                onItemSelected = {
                                    repeatHours = it
                                    isHourSelectedManually = true
                                    if (it == "00" && repeatMinutes == "00") {
                                        repeatMinutes = "01"
                                    }
                                }

                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Minutes", color = Color(0xFF80D8FF), fontSize = 16.sp)
                            SpinnerPicker(
                                items = minuteItems,
                                selectedItem = repeatMinutes.padStart(2, '0'),
                                onItemSelected = {
                                    repeatMinutes = it
                                },
                                isEnabled = isHourSelectedManually
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (selectedFiles.isNotEmpty())
                    {
                        val triggerCalendar = Calendar.getInstance()

                        if (triggerCalendar.get(Calendar.SECOND) > 0 || triggerCalendar.get(Calendar.MILLISECOND) > 0) {
                            triggerCalendar.add(Calendar.MINUTE, 1)
                        }
                        triggerCalendar.set(Calendar.SECOND, 0)
                        triggerCalendar.set(Calendar.MILLISECOND, 0)

                        val firstTriggerMillis = triggerCalendar.timeInMillis

                        val repeatTotalMinutes = (repeatHours.toInt() * 60) + repeatMinutes.toInt()
                        val repeatMillis = if (repeatMode == "Daily")
                            24 * 60 * 60 * 1000L // exactly 1 day
                        else
                            repeatTotalMinutes * 60 * 1000L

                        val filePaths = selectedFiles.map { it.absolutePath }
                        val fileNames = selectedFiles.joinToString(" | ") { it.name }

                        Log.d("ScheduleDebug", "Saving schedule:")
                        Log.d("ScheduleDebug", "fileName: $fileNames")
                        Log.d("ScheduleDebug", "filePath: $filePaths")


                        val newSchedule = ScheduleEntity(
                            timeMillis = firstTriggerMillis,
                            repeatIntervalMillis = repeatMillis
                        )

                        val existing = scheduleViewModel.schedules.value.map { it.schedule }

                        if (willOverlap(newSchedule, existing)) {
                            scheduleToSave = newSchedule
                            showOverlapDialog = true
                        } else {
                            scheduleViewModel.addScheduleWithConflictCheck(
                                context = context,
                                schedule = newSchedule,
                                audioFiles = selectedFiles,
                                onConflict = { existingSchedule, onConfirm ->
                                    // Conflict found, show dialog and set confirmation
                                    conflictSchedule = existingSchedule
                                    onReplaceConfirmed = {
                                        onConfirm()
                                        navController.currentBackStackEntry?.savedStateHandle?.set("show_card", true)
                                        navController.navigate(Screens.ScheduleSummary.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    showReplaceDialog = true
                                },
                                // ✅ No conflict — safe to navigate immediately
                                onSuccess = {
                                    navController.currentBackStackEntry?.savedStateHandle?.set("show_card", true)
                                    navController.navigate(Screens.ScheduleSummary.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )

                        }

                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF05655F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedFiles.isNotEmpty() && (repeatMode == "Daily" || isHourSelectedManually)
            ) {
                Text("Set Schedule", color = Color.White)
            }


        }
    }
    if (showReplaceDialog && conflictSchedule != null) {
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = {
                Text(
                    "Replace Existing Schedule?",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFBF360C)
                )
            },
            text = {
                Text(
                    "A schedule already exists with the same repeat interval of ${
                        conflictSchedule!!.repeatIntervalMillis / 60000
                    } minutes. Do you want to replace it?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showReplaceDialog = false  // ✅ dismiss first
                    onReplaceConfirmed?.invoke()  // ✅ now execute callback
                }) {
                    Text("Replace", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    if (showOverlapDialog) {
        AlertDialog(
            onDismissRequest = { showOverlapDialog = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️ Warning", style = MaterialTheme.typography.titleLarge, color = Color.Red)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Schedule Conflict", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = { Text("This schedule may overlap with an existing one. Do you want to proceed anyway?") },
            confirmButton = {
                TextButton(onClick = {
                    showOverlapDialog = false
                    scheduleToSave?.let { schedule ->
                        scope.launch {
                            scheduleViewModel.addScheduleWithAudioFiles(context, schedule, selectedFiles)
                            navController.currentBackStackEntry?.savedStateHandle?.set("show_card", true)
                            navController.navigate(Screens.ScheduleSummary.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlapDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    if (showFileExplorer) {
        AlertDialog(
            onDismissRequest = { showFileExplorer = false },
            confirmButton = {
                TextButton(
                    onClick = { showFileExplorer = false }
                ) {
                    Text("Done", color = Color(0xFF05655F))
                }
            },
            title = {
                Column {
                    Text("Browse Audio")
                    if (selectedFiles.isNotEmpty()) {
                        Text(
                            "${selectedFiles.size} file(s) selected",
                            fontSize = 14.sp,
                            color = Color(0xFF05655F),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column {
                    if (currentFolder != audioDir) {
                        Text(
                            "⬅ Back to ${currentFolder.parentFile?.name}",
                            modifier = Modifier
                                .clickable {
                                    currentFolder = currentFolder.parentFile ?: audioDir
                                }
                                .padding(bottom = 8.dp),
                            color = Color(0xFF2E8B7A)
                        )
                    }
                    LazyColumn {
                        items(subFiles) { file ->
                            val isSelected = selectedFiles.contains(file)
                            Row(
                                modifier = Modifier
                                    .heightIn(max = 400.dp)
                                    .fillMaxWidth()
                                    .background(
                                        if (!file.isDirectory && isSelected) Color(0xFFB2EBF2) else Color.Transparent
                                    )
                                    .clickable {
                                        if (file.isDirectory) {
                                            currentFolder = file
                                        } else {
                                            if (isSelected) selectedFiles.remove(file)
                                            else selectedFiles.add(file)
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.AudioFile,
                                    contentDescription = null,
                                    tint = if (file.isDirectory) Color(0xFF2E8B7A) else Color.Gray
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(file.name)
                            }
                        }
                    }
                }
            }
        )
    }

}

@Composable
fun SpinnerPicker(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    isEnabled: Boolean = true
) {
    if (!isEnabled) {
        // ✅ Show placeholder (disabled UI only)
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(120.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "--",
                fontSize = 18.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    )

    val coroutineScope = rememberCoroutineScope()
    var isUserScrolling by remember { mutableStateOf(false) }

    // Detect when user stops scrolling, then snap to nearest item
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && isUserScrolling) {
            isUserScrolling = false
            val centerIndex = listState.firstVisibleItemIndex +
                    if (listState.firstVisibleItemScrollOffset > 50) 1 else 0

            val validIndex = centerIndex.coerceIn(0, items.lastIndex)
            onItemSelected(items[validIndex])

            coroutineScope.launch {
                listState.animateScrollToItem(validIndex)
            }
        } else if (listState.isScrollInProgress) {
            isUserScrolling = true
        }
    }

    Box(
        modifier = Modifier
            .width(80.dp)
            .height(120.dp)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // Highlight center region
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(32.dp)
                .background(Color(0x22000000))
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 44.dp)
        ) {
            items(items) { item ->
                val isSelected = item == selectedItem
                Text(
                    text = item,
                    fontSize = if (isSelected) 22.sp else 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.Black else Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}
