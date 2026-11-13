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
        val r1 = intent.getIntExtra("r1", -1)
        val disciplineId = intent.getIntExtra("disciplineId", -1)

        // Если ссылка не пришла в Intent (например, подгрузилась позже), берём из кэша
        var conferenceLink = intent.getStringExtra("conferenceLink")?.takeIf { it.isNotBlank() }
        if (conferenceLink == null && (r1 > 0 || disciplineId > 0)) {
            val apiLinksPrefs = context.getSharedPreferences("api_lesson_links", Context.MODE_PRIVATE)
            conferenceLink = (if (r1 > 0) apiLinksPrefs.getString("r1_${r1}_$lessonDate", null) else null)
                ?: (if (disciplineId > 0) apiLinksPrefs.getString("disc_$disciplineId", null) else null)
        }

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
                if (!conferenceLink.isNullOrBlank()) {
                    putExtra("conference_link", conferenceLink)
                }
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
                lessonDate = lessonDate,
                conferenceLink = conferenceLink
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
            lessonDate = lessonDate,
            conferenceLink = conferenceLink
        )
    }
}
