package com.example.selftalker.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.example.selftalker.receiver.AlarmReceiver
import com.example.selftalker.data.local.entity.ScheduleEntity
import com.example.selftalker.data.local.dao.ScheduleDao
import kotlinx.coroutines.*

object ScheduleManager {

    fun scheduleAudioPlayback(
        context: Context,
        schedule: ScheduleEntity,
        audioFiles: List<String>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "SCHEDULE_ALARM_${schedule.id}"
            putStringArrayListExtra("filePaths", ArrayList(audioFiles))
            putExtra("repeatIntervalMillis", schedule.repeatIntervalMillis)
            putExtra("scheduleId", schedule.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        schedule.timeMillis,
                        pendingIntent
                    )
                } else {
                    Toast.makeText(
                        context,
                        "Exact alarms not permitted. Please allow them in system settings.",
                        Toast.LENGTH_LONG
                    ).show()
                    val permissionIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    permissionIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(permissionIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    schedule.timeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to schedule alarm: permission denied", Toast.LENGTH_LONG).show()
        }
    }

    fun cancelScheduledAudio(context: Context, schedule: ScheduleEntity) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_PLAY_SCHEDULE // 🧠 MUST match scheduled action
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }




    suspend fun rescheduleAllAlarms(context: Context, dao: ScheduleDao) {
        withContext(Dispatchers.Default) {
            val allSchedules = dao.getAllSchedulesWithAudiosOnce()
            val now = System.currentTimeMillis()

            allSchedules.forEach { scheduleWithAudios ->
                val schedule = scheduleWithAudios.schedule
                val audioPaths = scheduleWithAudios.audios.map { it.filePath }

                val adjustedTime = if (schedule.timeMillis <= now) {
                    val intervalsPassed = ((now - schedule.timeMillis) / schedule.repeatIntervalMillis) + 1
                    schedule.timeMillis + intervalsPassed * schedule.repeatIntervalMillis
                } else {
                    schedule.timeMillis
                }

                val updatedSchedule = schedule.copy(timeMillis = adjustedTime)
                scheduleAudioPlayback(context, updatedSchedule, audioPaths)
            }
        }
    }


    fun cancelAllAlarms(context: Context, dao: ScheduleDao) {
        CoroutineScope(Dispatchers.Default).launch {
            val schedules = dao.getAllSchedulesOnce()
            schedules.forEach { schedule ->
                cancelScheduledAudio(context, schedule)
            }
        }
    }
}
