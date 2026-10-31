package com.mkr.schedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mkr.schedule.util.LogCollector

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subject = intent.getStringExtra("subject") ?: return
        val subjectFull = intent.getStringExtra("subjectFull") ?: subject
        val room = intent.getStringExtra("room") ?: "?"
        val teacher = intent.getStringExtra("teacher") ?: "N/A"
        val startTime = intent.getStringExtra("startTime") ?: ""
        val endTime = intent.getStringExtra("endTime") ?: ""
        val lessonDate = intent.getStringExtra("lessonDate") ?: ""
        val lessonNumber = intent.getIntExtra("lessonNumber", 0)
        val lessonType = intent.getStringExtra("lessonType") ?: "Пара"
        val minutesBefore = intent.getIntExtra("minutesBefore", 15)

        LogCollector.log("AlarmReceiver", "Received alarm trigger for $subject ($minutesBefore mins before)")

        val notificationManager = LessonNotificationManager(context)

        if (minutesBefore == 0) {
            val serviceIntent = Intent(context, com.mkr.schedule.services.OngoingLessonService::class.java).apply {
                putExtra("lesson_name", subjectFull)
                putExtra("lesson_number", lessonNumber)
                putExtra("lesson_type", lessonType)
                putExtra("room", room)
                putExtra("teacher", teacher)
                putExtra("start_time", startTime)
                putExtra("end_time", endTime)
            }
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                LogCollector.log("AlarmReceiver", "Failed to start ongoing service: ${e.message}")
            }

            notificationManager.showLessonStarted(
                subjectName = subjectFull,
                teacher = teacher,
                classroom = room,
                startTime = startTime,
                endTime = endTime,
                lessonNumber = lessonNumber,
                lessonType = lessonType,
                lessonDate = lessonDate
            )
            return
        }

        notificationManager.showLessonReminder(
            subjectName = subjectFull,
            subjectShortName = subject,
            teacher = teacher,
            classroom = room,
            startTime = startTime,
            endTime = endTime,
            lessonNumber = lessonNumber,
            lessonType = lessonType,
            reminderMinutes = minutesBefore,
            lessonDate = lessonDate
        )
    }
}
