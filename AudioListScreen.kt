package com.example.selftalker.screens.common

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selftalker.components.AudioFileItem
import com.example.selftalker.model.AudioFile
import com.example.selftalker.utils.getFavourites
import com.example.selftalker.utils.moveFileOutOfFavourites
import com.example.selftalker.utils.moveFileToFavourites
import com.example.selftalker.utils.updateFavouritePath
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AudioListScreen(
    title: String,
    audioFiles: List<AudioFile>,
    availableFolders: List<File>,
    showAddButton: Boolean = false,
    onAddClick: (() -> Unit)? = null,
    onAddPlaylistClick: (() -> Unit)? = null,
    onDeleteClick: ((AudioFile) -> Unit)? = null,
    onRenameClick: ((oldFile: File, newFile: File) -> Unit)? = null,
    onFavouriteClick: ((AudioFile) -> Unit)? = null,
    onRemoveFavouriteClick: ((AudioFile) -> Unit)? = null,
    currentlyPlayingFile: AudioFile? = null,
    onPlayPauseClick: ((AudioFile) -> Unit)? = null,
    onFolderClick: ((AudioFile) -> Unit)? = null,
    isSubfolder: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onFilesMoved: (() -> Unit)? = null,

    ) {
    val context = LocalContext.current
    val primaryTeal = Color(0xFF2E8B7A)
    val whiteTransparent = Color(0xFFF5FFFE)

    var renameDialogFile by remember { mutableStateOf<File?>(null) }
    var newFileName by remember { mutableStateOf("") }

    val favourites = remember { mutableStateListOf<File>().apply { addAll(getFavourites(context)) } }

    val selectedFiles = remember { mutableStateListOf<AudioFile>() }
    var selectionMode by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }

    var showFolderPicker by remember { mutableStateOf(false) }

    var selectedFolder by remember { mutableStateOf<File?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val hasFolderSelected = selectedFiles.any { it.isFolder }

    val favouritesDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
        "SelfTalker/Favourites"
    )

    fun isInFavourites(file: File): Boolean {
        return file.absolutePath.startsWith(favouritesDir.absolutePath)
    }


    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF43AAA7), Color(0xFF29909B))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = isSubfolder) { onBackClick?.invoke() }
                ) {
                    if (isSubfolder) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Audio Files", fontSize = 18.sp, color = Color.White)
                }

                if (selectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${selectedFiles.size} Selected",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        IconButton(onClick = {
                            selectedFiles.clear()
                            selectionMode = false
                            showTopMenu = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection", tint = Color.White)
                        }

                        Box {
                            IconButton(onClick = { showTopMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                            }

                            DropdownMenu(
                                expanded = showTopMenu,
                                onDismissRequest = { showTopMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        selectedFiles.forEach { onDeleteClick?.invoke(it) }
                                        selectedFiles.clear()
                                        selectionMode = false
                                        showTopMenu = false
                                    }
                                )

                                if (!hasFolderSelected) {
                                    DropdownMenuItem(
                                        text = { Text("Move") },
                                        onClick = {
                                            showFolderPicker = true
                                            showTopMenu = false
                                        }
                                    )
                                }
                            }

                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = whiteTransparent)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn {
                        if (audioFiles.isEmpty()) {
                            item {
                                Text("No audio files found", color = primaryTeal)
                            }
                        } else {
                            items(audioFiles, key = { it.path }) { originalAudioFile ->
                                val audioFile = originalAudioFile.copy(
                                    isFavourite = isInFavourites(originalAudioFile.file)
                                )

                                val isSelected = selectedFiles.any { it.path == audioFile.path }

                                AudioFileItem(
                                    audioFile = audioFile,
                                    isSelected = isSelected,
                                    selectionMode = selectionMode,
                                    onPlay = { if (!audioFile.isFolder) onPlayPauseClick?.invoke(audioFile)},
                                    onClick = { if (audioFile.isFolder) onFolderClick?.invoke(audioFile) },
                                    onDelete = { onDeleteClick?.invoke(audioFile) },
                                    onRename = {
                                        renameDialogFile = audioFile.file
                                        newFileName = audioFile.name
                                    },
                                    onMove = {
                                        selectedFiles.clear()
                                        selectedFiles.add(audioFile)
                                        selectionMode = true
                                        showFolderPicker = true
                                    },
                                    onFavourite = {
                                        if (audioFile.isFolder) {
                                            val audioFilesInFolder = audioFile.file.listFiles()
                                                ?.filter { it.isFile && it.name.endsWith(".m4a", true) || it.name.endsWith(".wav", true)|| it.name.endsWith(".mp3", true) } ?: emptyList()

                                            audioFilesInFolder.forEach { file ->
                                                val movedFile = moveFileToFavourites(context, file)
                                                val movedAudioFile = audioFile.copy(file = movedFile)
                                                favourites.add(movedFile)
                                                onFavouriteClick?.invoke(movedAudioFile)
                                            }
                                        } else {
                                            val movedFile = moveFileToFavourites(context, audioFile.file)
                                            val movedAudioFile = audioFile.copy(file = movedFile)
                                            favourites.add(movedFile)
                                            onFavouriteClick?.invoke(movedAudioFile)
                                        }
                                    },
                                    onRemoveFavourite = {
                                        val movedFile = moveFileOutOfFavourites(context, audioFile.file)
                                        val movedAudioFile = audioFile.copy(file = movedFile)
                                        favourites.removeIf { it.absolutePath == audioFile.file.absolutePath }
                                        onRemoveFavouriteClick?.invoke(movedAudioFile)
                                    },
                                    onLongPress = {
                                        if (!selectedFiles.any { it.path == audioFile.path }) {
                                            selectionMode = true
                                            selectedFiles.add(audioFile)
                                        }
                                    },
                                    onSelectToggle = {
                                        if (selectedFiles.any { it.path == audioFile.path }) {
                                            selectedFiles.removeIf { it.path == audioFile.path }
                                            if (selectedFiles.isEmpty()) selectionMode = false
                                        } else {
                                            selectedFiles.add(audioFile)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showAddButton && onAddClick != null) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onAddClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A848B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Audio File", color = Color.White, maxLines = 1)
                    }

                    if (onAddPlaylistClick != null) {
                        Button(
                            onClick = onAddPlaylistClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A848B)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                            Spacer(Modifier.width(1.dp))
                            Text(text = "Playlist", color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
        }
        // Dialogs
        if (showFolderPicker) {
            AlertDialog(
                onDismissRequest = { showFolderPicker = false },
                title = { Text("Select Folder to Move") },
                text = {
                    Column {
                        availableFolders.forEach { folder ->
                            Text(
                                text = folder.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFolder = folder
                                    }
                                    .padding(8.dp),
                                color = if (selectedFolder == folder) Color.Blue else Color.Black
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            selectedFolder?.let { folder ->
                                selectedFiles.filter { !it.isFolder }.forEach { audioFile ->
                                    val newFile = File(folder, audioFile.file.name)
                                    if (audioFile.file.renameTo(newFile)) {
                                        updateFavouritePath(context, audioFile.file, newFile)
                                        // Optional: You can trigger callback to notify parent
                                    }
                                }

                                val movedCount = selectedFiles.count { !it.isFolder }

                                selectedFiles.clear()
                                selectionMode = false
                                selectedFolder = null
                                showFolderPicker = false

                                onFilesMoved?.invoke()

                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("✅ $movedCount file(s) moved to '${folder.name}'")
                                }
                            }
                            showFolderPicker = false

                        }
                    ) {
                        Text("Move")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFolderPicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (renameDialogFile != null) {
            AlertDialog(
                onDismissRequest = { renameDialogFile = null },
                title = { Text("Rename Audio File") },
                text = {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("New Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        renameDialogFile?.let { oldFile ->
                            val parent = oldFile.parentFile
                            val newFile = File(parent, "$newFileName.${oldFile.extension}")
                            if (oldFile.renameTo(newFile)) {
                                updateFavouritePath(context, oldFile, newFile)
                                onRenameClick?.invoke(oldFile, newFile)
                            }
                        }
                        renameDialogFile = null
                        newFileName = ""
                    }) {
                        Text("Rename")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        renameDialogFile = null
                        newFileName = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

    }
}
