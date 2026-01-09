package com.example.selftalker.screens

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onOpenPlaylist: (File) -> Unit = {},
    onBackToLibrary: () -> Unit = {}
) {
    val rootDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
        "SelfTalker"
    )
    val playlists = remember { mutableStateListOf<File>() }

    LaunchedEffect(Unit) {
        playlists.clear()
        playlists.addAll(
            rootDir.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedBy { it.name }
                ?: emptyList()
        )
    }

    val primaryTeal = Color(0xFF2E8B7A)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Playlists", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackToLibrary) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = primaryTeal
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No playlists found", color = primaryTeal, fontSize = 18.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(playlists) { folder ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPlaylist(folder) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = "Playlist", tint = primaryTeal)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(folder.name, color = primaryTeal, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
