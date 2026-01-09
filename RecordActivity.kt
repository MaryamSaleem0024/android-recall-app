package com.example.selftalker

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.selftalker.screens.RecordCompleteScreen
import com.example.selftalker.screens.RecordReadyScreen
import com.example.selftalker.screens.RecordingScreen
import com.example.selftalker.ui.theme.SelfTalkerTheme
import java.io.File

class RecordActivity : ComponentActivity() {

    private var recorder: MediaRecorder? = null
    private lateinit var outputFile: File
    private var permissionGranted by mutableStateOf(false)

    private lateinit var requestPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            permissionGranted = isGranted
            if (isGranted) {
                Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Microphone permission is required to record", Toast.LENGTH_LONG).show()
            }
        }

        setContent {
            SelfTalkerTheme {
                var recordingState by remember { mutableStateOf(RecordingState.READY) }
                var timer by remember { mutableStateOf<CountDownTimer?>(null) }

                val stopRecording = {
                    timer?.cancel()
                    recorder?.apply {
                        try {
                            stop()
                            release()
                        } catch (e: Exception) {
                            release()
                        }
                    }
                    recorder = null
                    recordingState = RecordingState.COMPLETE
                }

                val startRecording = {
                    if (!permissionGranted) {
                        checkAndRequestPermission()
                    } else {
                        try {
                            recorder?.apply {
                                try {
                                    stop()
                                    release()
                                } catch (e: Exception) {
                                    Log.e("Recorder", "Stop failed", e)
                                    release()
                                }
                            }
                            recorder = null

                            outputFile = File(cacheDir, "recorded_audio.m4a")

                            recorder = MediaRecorder().apply {
                                setAudioSource(MediaRecorder.AudioSource.MIC)
                                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                setOutputFile(outputFile.absolutePath)
                                prepare()
                                start()
                            }

                            recordingState = RecordingState.RECORDING

                            timer = object : CountDownTimer(60000, 1000) {
                                override fun onTick(millisUntilFinished: Long) {}
                                override fun onFinish() {
                                    stopRecording()
                                }
                            }.start()
                        } catch (e: Exception) {
                            recorder?.release()
                            recorder = null
                            Toast.makeText(this@RecordActivity, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (recordingState) {
                        RecordingState.READY -> RecordReadyScreen(
                            onBack = { finish() },
                            onStartRecording = startRecording
                        )

                        RecordingState.RECORDING -> RecordingScreen(
                            onBack = { finish() },
                            onStopRecording = stopRecording
                        )

                        RecordingState.COMPLETE -> RecordCompleteScreen(
                            onBack = {
                                recordingState = RecordingState.READY
                            },
                            onSave = { title, playlistName ->
                                val saved = saveRecordingToSelectedPlaylist(title, playlistName)
                                if (saved) {
                                    Toast.makeText(this@RecordActivity, "Audio saved", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@RecordActivity, "Failed to save audio", Toast.LENGTH_SHORT).show()
                                }
                                finish()
                            },
                            onDiscard = {
                                deleteAudioFile()
                                Toast.makeText(this@RecordActivity, "Recording discarded", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        )
                    }
                }
            }
        }

        // Immediately check permission on launch
        checkAndRequestPermission()
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                permissionGranted = true
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun saveRecordingToSelectedPlaylist(title: String, playlistName: String): Boolean {
        return try {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val recordingsDir = File(musicDir, "SelfTalker")
            val playlistDir = File(recordingsDir, playlistName)
            if (!playlistDir.exists()) playlistDir.mkdirs()

            val newFile = File(playlistDir, "$title.m4a")
            outputFile.copyTo(newFile, overwrite = true)
            true
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun deleteAudioFile() {
        val file = File(cacheDir, "recorded_audio.m4a")
        if (file.exists()) file.delete()
    }
}

enum class RecordingState {
    READY, RECORDING, COMPLETE
}