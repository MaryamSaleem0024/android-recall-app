package com.example.selftalker.screens

import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.selftalker.model.AudioFile
import com.example.selftalker.screens.common.AudioListScreen
import com.example.selftalker.utils.*
import com.example.selftalker.model.ScheduleViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SoundLibraryScreen(
    viewModel: ScheduleViewModel = viewModel(),
    initialFolder: File? = null
) {
    val context = LocalContext.current
    LaunchedEffect(viewModel.schedules) {
        val scheduleList = viewModel.schedules.value
        scheduleList.forEach {
            val allMissing = it.audios.all { audio -> !File(audio.filePath).exists() }
            if (allMissing) {
                viewModel.deleteScheduleAndCancelAlarm(context, it.schedule)
            }
        }
    }

    var audioFiles by remember { mutableStateOf(emptyList<AudioFile>()) }

    val rootAudioDir = remember {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "SelfTalker")
    }
    var currentDir by remember { mutableStateOf(initialFolder ?: rootAudioDir) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingFile by remember { mutableStateOf<AudioFile?>(null) }

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("New Playlist") }

    var isPlaying by remember { mutableStateOf(false) }
    var availableFolders by remember { mutableStateOf(emptyList<File>()) }

    fun stopCurrentPlayback() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        playingFile = null
        isPlaying = false
    }

    fun refreshAvailableFolders() {
        availableFolders = rootAudioDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.apply {
                stop()
                release()
            }
            mediaPlayer = null
            playingFile = null
        }
    }
    fun isValidPlaylistName(name: String): Boolean {
        val invalidChars = Regex("""[\\/:*?"<>|]""")
        return name.isNotBlank() &&
                name != "Favourites" &&
                !invalidChars.containsMatchIn(name.trim())
    }

    fun refreshAudioFiles() {
        val files = currentDir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()

        val favouritePaths = getFavouritePaths(context)
        audioFiles = files.map {
            AudioFile(
                file = it,
                isFavourite = favouritePaths.contains(it.absolutePath) || isInFavouritesFolder(it),
                isPlaying = it.absolutePath == playingFile?.file?.absolutePath
            )
        }

    }


    LaunchedEffect(Unit) {
        refreshAudioFiles()
        refreshAvailableFolders()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        var importedCount = 0

        fun generateUniqueFile(baseDir: File, originalName: String): File {
            val name = originalName.substringBeforeLast('.')
            val ext = originalName.substringAfterLast('.', "")
            var count = 1
            var candidate = File(baseDir, originalName)

            while (candidate.exists()) {
                Log.d("ImportDebug", "File exists: ${candidate.absolutePath}")
                val newName = "$name ($count).$ext"
                candidate = File(baseDir, newName)
                count++
            }
            return candidate
        }

        uris.forEach { uri ->
            try {
                Log.d("ImportDebug", "Selected URI: $uri")

                // Test DocumentFile read access
                val docFile = DocumentFile.fromSingleUri(context, uri)
                Log.d("ImportDebug", "DocumentFile - Can Read: ${docFile?.canRead()} | Name: ${docFile?.name}")

                val fileName = getFileName(context, uri)
                Log.d("ImportDebug", "Extracted File Name: $fileName")

                if (fileName == null) {
                    Log.e("ImportDebug", "File name is null, skipping URI: $uri")
                    return@forEach
                }

                val destFile = generateUniqueFile(currentDir, fileName)

                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e("ImportDebug", "InputStream is null for URI: $uri")
                    return@forEach
                }

                FileOutputStream(destFile, false).use { output ->
                    inputStream.copyTo(output)
                }

                if (destFile.exists()) {
                    importedCount++
                    Log.d("ImportDebug", "File saved to: ${destFile.absolutePath}")
                }

            } catch (e: Exception) {
                Log.e("ImportDebug", "Error importing file: ${e.message}", e)
            }
        }


        refreshAudioFiles()

        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val message = if (importedCount > 0)
                "✅ $importedCount Audio File(s) Imported"
            else
                "⚠️ No new files were imported"
            snackbarHostState.showSnackbar(message)
        }
    }



    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF666666),
                    contentColor = Color.White,
                )
            }
        }
    ) { _ ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            val showPlaylistButton = currentDir == rootAudioDir

            Box(
                modifier = Modifier.fillMaxHeight()
            ) {
                if (currentDir != rootAudioDir) {
                    Text(
                        text = "Path: ${currentDir.relativeTo(rootAudioDir).path}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                AudioListScreen(
                    title = currentDir.name.uppercase(),
                    audioFiles = audioFiles,
                    showAddButton = true,
                    isSubfolder = currentDir != rootAudioDir,
                    onBackClick = {
                        currentDir = currentDir.parentFile ?: rootAudioDir
                        refreshAudioFiles()
                    },
                    onAddClick = {
                        println("Importing to: ${currentDir.absolutePath}")
                        importLauncher.launch(arrayOf("audio/*"))
                    },
                    onAddPlaylistClick = if (showPlaylistButton) {
                        { showPlaylistDialog = true }
                    } else null,
                    onDeleteClick = { audioFile ->
                        val file = audioFile.file
                        if (file.exists()) {
                            val deleted = if (file.isDirectory) {
                                file.deleteRecursively()
                            } else {
                                file.delete()
                            }

                            if (deleted) {
                                // 🛑 Stop playback if this was the currently playing file
                                if (file.absolutePath == playingFile?.file?.absolutePath) {
                                    stopCurrentPlayback()
                                }


                                viewModel.deleteSchedulesByFilePath(file.absolutePath, context)
                                removeFromFavourites(context, file)
                                refreshAudioFiles()

                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar("❌ Deleted Successfully")
                                }
                            }
                        }
                    },
                    onRenameClick = { oldFile, newFile ->
                        updateFavouritePath(context, oldFile, newFile)
                        refreshAudioFiles()
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("✏️ Renamed Successfully")
                        }
                    },
                    onRemoveFavouriteClick = { audioFile ->
                        val file = audioFile.file

                        if (audioFile.isFolder && file.name == "Favourites") {
                            // Move all files out of the Favourites folder
                            val filesInFavourites = file.listFiles()?.filter { it.isFile } ?: emptyList()
                            val defaultDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "SelfTalker")

                            var movedCount = 0
                            filesInFavourites.forEach { favFile ->
                                val destFile = File(defaultDir, favFile.name)
                                if (favFile.renameTo(destFile)) {
                                    updateFavouritePath(context, favFile, destFile)
                                    movedCount++
                                }
                            }

                            refreshAudioFiles()
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar("✅ Removed $movedCount file(s) from Favourites")
                            }
                        } else {
                            // Individual file removal
                            removeFromFavourites(context, file)
                            refreshAudioFiles()
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar("💔 Removed from Favourites")
                            }
                        }
                    },
                    onFavouriteClick = { audioFile ->
                        saveToFavourites(context, audioFile.file)
                        refreshAudioFiles()
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("❤ Marked as Favourite")
                        }
                    },
                    currentlyPlayingFile = playingFile,
                    onPlayPauseClick = { audioFile ->
                        if (audioFile.file.absolutePath == playingFile?.file?.absolutePath) {
                            stopCurrentPlayback()
                        } else {
                            try {
                                mediaPlayer?.apply {
                                    stop()
                                    release()
                                }
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(audioFile.file.absolutePath)
                                    prepare()
                                    start()
                                    setOnCompletionListener {
                                        playingFile = null
                                        isPlaying = false
                                        refreshAudioFiles()
                                    }
                                }
                                playingFile = audioFile
                                isPlaying = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                isPlaying = false
                            }
                        }

                        refreshAudioFiles()
                    },
                    onFolderClick = { folder ->
                        currentDir = folder.file
                        refreshAudioFiles()
                    },
                    availableFolders = availableFolders,
                    onFilesMoved = {
                        refreshAudioFiles()
                        refreshAvailableFolders()
                    }
                )
            }

            if (showPlaylistDialog) {
                var showNameError by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = {
                        showPlaylistDialog = false
                        showNameError = false
                        newPlaylistName = ""
                    },
                    title = { Text("Create New Playlist") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = {
                                    newPlaylistName = it
                                    showNameError = !isValidPlaylistName(it)
                                },
                                label = { Text("Playlist Name") },
                                isError = showNameError,
                                singleLine = true
                            )

                            if (showNameError) {
                                Text(
                                    text = "❌ Invalid name (no slashes, empty, or 'Favourites')",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {

                                val trimmedName = newPlaylistName.trim()
                                if (isValidPlaylistName(trimmedName)) {
                                    val newDir = File(rootAudioDir, trimmedName)
                                    if (!newDir.exists()) {
                                        newDir.mkdirs()
                                        currentDir = newDir
                                        refreshAudioFiles()
                                        coroutineScope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar("🎵 Playlist Created")
                                        }
                                    }
                                    showPlaylistDialog = false
                                    newPlaylistName = ""
                                    showNameError = false
                                } else {
                                    showNameError = true
                                }
                            },
                            enabled = isValidPlaylistName(newPlaylistName)
                        ) {
                            Text("Create")
                        }

                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showPlaylistDialog = false
                            newPlaylistName = ""
                            showNameError = false
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
