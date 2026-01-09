@file:Suppress("DEPRECATION", "DEPRECATION")

package com.example.selftalker.screens

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordCompleteScreen(
    onBack: () -> Unit,
    onSave: (String, String) -> Unit,
    onDiscard: () -> Unit
) {
    val rootDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "SelfTalker")
    var currentDir by remember { mutableStateOf(rootDir) }
    val folders by remember(currentDir) {
        mutableStateOf(currentDir.listFiles()?.filter { it.isDirectory } ?: emptyList())
    }

    var titleText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf(rootDir.name) }

    val primaryTeal = Color(0xFF2E8B7A)
    val lightTeal = Color(0xFF6AB8A8)
    val whiteTransparent = Color(0xFFF5FFFE)
    val cardBackground = Color(0xFFE8F5F3)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF43AAA7), Color(0xFF29909B))
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "App Icon",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SELF TALK",
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = whiteTransparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Recording Complete!",
                        style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = primaryTeal, letterSpacing = 0.5.sp),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Your audio has been successfully recorded",
                        style = TextStyle(fontSize = 16.sp, color = primaryTeal.copy(alpha = 0.7f), fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(brush = Brush.radialGradient(listOf(lightTeal, primaryTeal)), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Recording Complete", modifier = Modifier.size(60.dp), tint = Color.White)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Add a title for your recording",
                            style = TextStyle(fontSize = 18.sp, color = primaryTeal, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            BasicTextField(
                                value = titleText,
                                onValueChange = { titleText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                textStyle = TextStyle(fontSize = 16.sp, color = primaryTeal, fontWeight = FontWeight.Medium),
                                decorationBox = { innerTextField ->
                                    if (titleText.isEmpty()) {
                                        Text(
                                            text = "Enter a descriptive title...",
                                            style = TextStyle(fontSize = 16.sp, color = primaryTeal.copy(alpha = 0.5f))
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Select Playlist",
                            style = TextStyle(fontSize = 18.sp, color = primaryTeal, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryTeal)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedPlaylist.isNotEmpty()) selectedPlaylist else "Select Playlist",
                                        fontSize = 16.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = primaryTeal
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                if (currentDir != rootDir) {
                                    DropdownMenuItem(
                                        text = { Text("⬅️ Back") },
                                        onClick = {
                                            currentDir = currentDir.parentFile ?: rootDir
                                            expanded = false
                                        }
                                    )
                                }
                                folders.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text(folder.name) },
                                        onClick = {
                                            if (folder.listFiles()?.any { it.isDirectory } == true) {
                                                currentDir = folder
                                            } else {
                                                selectedPlaylist = folder.name
                                            }
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (titleText.isEmpty() || selectedPlaylist.isEmpty()) {
                            Text(
                                text = "Title and playlist are required to save your recording",
                                style = TextStyle(fontSize = 12.sp, color = primaryTeal.copy(alpha = 0.6f)),
                                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDiscard,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryTeal),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(primaryTeal, primaryTeal))
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Discard", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (titleText.isNotEmpty() && selectedPlaylist.isNotEmpty()) {
                                    onSave(titleText, selectedPlaylist)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (titleText.isNotEmpty() && selectedPlaylist.isNotEmpty()) primaryTeal else primaryTeal.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            enabled = titleText.isNotEmpty() && selectedPlaylist.isNotEmpty()
                        ) {
                            Text("Save", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
