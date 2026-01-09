    package com.example.selftalker.utils

    import android.content.Context
    import android.content.Intent
    import android.media.MediaPlayer
    import android.os.Build
    import android.util.Log
    import com.example.selftalker.receiver.AudioPlaybackService
    import java.io.File
    import java.util.concurrent.ConcurrentLinkedQueue

    object PlaybackQueueManager {

        private val playbackQueue = ConcurrentLinkedQueue<List<String>>()
        private var mediaPlayer: MediaPlayer? = null
        private var isPlaying = false
        private lateinit var appContext: Context

        fun init(context: Context) {
            appContext = context.applicationContext
        }

        fun enqueue(context: Context, audioPaths: List<String>) {
            Log.d("PlaybackQueue", "📥 Enqueue called with: $audioPaths")
            playbackQueue.add(audioPaths)

            if (!isPlaying) {
                playNextBatch()
            }
        }

        private fun playNextBatch() {
            val nextBatch = playbackQueue.poll()
            if (nextBatch.isNullOrEmpty()) {
                Log.d("PlaybackQueue", "⛔ No more items in queue.")
                isPlaying = false
                return
            }

            Log.d("PlaybackQueue", "▶️ Playing batch of ${nextBatch.size} files")
            isPlaying = true

            // Start foreground service
            val serviceIntent = Intent(appContext, AudioPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(serviceIntent)
            }

            playSequentially(nextBatch.iterator())
        }

        private fun playSequentially(iterator: Iterator<String>) {
            if (!iterator.hasNext()) {
                notifyPlaybackCompleted()
                return
            }

            val path = iterator.next()
            Log.d("AudioPlaybackService", "🎵 Playing: $path")

            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                setOnPreparedListener {
                    Log.d("AudioPlaybackService", "🔊 MediaPlayer prepared, starting playback")
                    start()
                }
                setOnCompletionListener {
                    Log.d("AudioPlaybackService", "✅ Completed playing: $path")
                    reset()
                    release()
                    mediaPlayer = null
                    playSequentially(iterator)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlaybackService", "❌ MediaPlayer error what=$what extra=$extra")
                    reset()
                    release()
                    mediaPlayer = null
                    playSequentially(iterator)
                    true
                }
                prepareAsync()
            }
        }

        fun notifyPlaybackCompleted() {
            Log.d("PlaybackQueue", "✅ notifyPlaybackCompleted called")
            isPlaying = false
            playNextBatch()
        }

        fun stopPlayback() {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            playbackQueue.clear()
            isPlaying = false
        }
    }
