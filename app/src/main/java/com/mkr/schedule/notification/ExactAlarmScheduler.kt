package com.mkr.schedule.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mkr.schedule.data.model.DaySchedule
import com.mkr.schedule.util.LogCollector
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ExactAlarmScheduler {

    fun scheduleAllAlarms(context: Context) {
        cancelAllAlarms(context)
        val scheduledIds = mutableSetOf<String>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canUseExactAlarms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (!canUseExactAlarms) {
            LogCollector.log("ExactAlarmScheduler", "Exact alarm permission denied. Using inexact fallback alarms.")
        }

        val schedule = getScheduleFromPreferences(context)
        if (schedule.isEmpty()) return

        val apiLinksPrefs = context.getSharedPreferences("api_lesson_links", Context.MODE_PRIVATE)
        val nowDateTime = LocalDateTime.now(ZoneId.systemDefault())
        val today = nowDateTime.toLocalDate()

        val reminderOffsets = listOf(15, 5, 0)

        schedule.forEach { daySchedule ->
            val cleanDateStr = daySchedule.date.take(10)
            val lessonDate = try {
                LocalDate.parse(cleanDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                return@forEach
            }

            if (lessonDate.isBefore(today) || lessonDate.isAfter(today.plusDays(14))) {
                return@forEach
            }

            daySchedule.lessons.forEach { lesson ->
                val pairNum = lesson.numberPair?.toIntOrNull() ?: 0
                lesson.periods.forEach periodLoop@{ period ->
                    val startTimeStr = period.timeStart?.take(5) ?: return@periodLoop
                    val startTime = try {
                        LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
                    } catch (e: Exception) {
                        return@periodLoop
                    }

                    val endTimeStr = period.timeEnd?.take(5)
                        ?: startTime.plusMinutes(80).format(DateTimeFormatter.ofPattern("HH:mm"))
                    val endTime = try {
                        LocalTime.parse(endTimeStr, DateTimeFormatter.ofPattern("HH:mm"))
                    } catch (_: Exception) {
                        startTime.plusMinutes(80)
                    }

                    val lessonStart = LocalDateTime.of(lessonDate, startTime)
                    val lessonEnd = LocalDateTime.of(lessonDate, endTime)
                    val shortName = period.disciplineShortName?.takeIf { it.isNotBlank() }
                        ?: period.disciplineFullName ?: "Пара"
                    val fullName = period.disciplineFullName?.takeIf { it.isNotBlank() } ?: shortName
                    val room = period.classroom ?: "?"
                    val teacher = period.teachersNameFull ?: period.teachersName ?: "N/A"
                    val lessonType = LessonNotificationManager.resolveLessonTypeLabel(
                        period.type,
                        shortName,
                        fullName,
                        period.typeStr
                    )
                    val conferenceLink: String? = (period.r1?.let { apiLinksPrefs.getString("r1_${it}_$cleanDateStr", null) })
                        ?: (period.disciplineId?.let { apiLinksPrefs.getString("disc_$it", null) })
                        ?: extractFirstUrl(period.classroom)
                        ?: extractFirstUrl(period.disciplineFullName)

                    // If this lesson is currently ongoing right now today, start the live countdown service immediately!
                    if (lessonDate == today && nowDateTime.isAfter(lessonStart) && nowDateTime.isBefore(lessonEnd)) {
                        val serviceIntent = Intent(context, com.mkr.schedule.services.OngoingLessonService::class.java).apply {
                            putExtra("lesson_name", fullName)
                            putExtra("lesson_number", pairNum)
                            putExtra("lesson_type", lessonType)
                            putExtra("room", room)
                            putExtra("teacher", teacher)
                            putExtra("start_time", startTimeStr)
                            putExtra("end_time", endTimeStr)
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
                            LogCollector.log("ExactAlarmScheduler", "Failed to start ongoing service: ${e.message}")
                        }
                    }

                    reminderOffsets.forEach { minutesBefore ->
                        val alarmTime = lessonStart.minusMinutes(minutesBefore.toLong())
                        if (alarmTime.isAfter(nowDateTime)) {
                            val intent = Intent(context, AlarmReceiver::class.java).apply {
                                putExtra("subject", shortName)
                                putExtra("subjectFull", fullName)
                                putExtra("room", room)
                                putExtra("teacher", teacher)
                                putExtra("startTime", startTimeStr)
                                putExtra("endTime", endTimeStr)
                                putExtra("lessonDate", cleanDateStr)
                                putExtra("lessonNumber", pairNum)
                                putExtra("lessonType", lessonType)
                                putExtra("minutesBefore", minutesBefore)
                                putExtra("r1", period.r1 ?: -1)
                                putExtra("disciplineId", period.disciplineId ?: -1)
                                if (!conferenceLink.isNullOrBlank()) {
                                    putExtra("conferenceLink", conferenceLink)
                                }
                            }
                            val requestCode = "${cleanDateStr}_${startTimeStr}_${shortName}_$minutesBefore".hashCode()

                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                requestCode,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            scheduledIds.add(requestCode.toString())
                            val triggerAtMillis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            try {
                                if (canUseExactAlarms) {
                                    alarmManager.setExactAndAllowWhileIdle(
                                        AlarmManager.RTC_WAKEUP,
                                        triggerAtMillis,
                                        pendingIntent
                                    )
                                } else {
                                    alarmManager.setAndAllowWhileIdle(
                                        AlarmManager.RTC_WAKEUP,
                                        triggerAtMillis,
                                        pendingIntent
                                    )
                                }
                            } catch (se: SecurityException) {
                                alarmManager.setAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    triggerAtMillis,
                                    pendingIntent
                                )
                            }
                            LogCollector.log("ExactAlarmScheduler", "Scheduled $minutesBefore min alarm for $shortName at $alarmTime")
                        }
                    }
                }
            }
        }
        context.getSharedPreferences("alarm_registry", Context.MODE_PRIVATE).edit()
            .putStringSet("ids", scheduledIds).apply()
    }

    fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val registry = context.getSharedPreferences("alarm_registry", Context.MODE_PRIVATE)
        val storedIds = registry.getStringSet("ids", emptySet())?.toSet().orEmpty()
        storedIds.forEach { id ->
            val code = id.toIntOrNull() ?: return@forEach
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        registry.edit().remove("ids").apply()

        val schedule = getScheduleFromPreferences(context)
        if (schedule.isEmpty()) return

        val reminderOffsets = listOf(15, 5, 0)

        schedule.forEach { daySchedule ->
            val cleanDateStr = daySchedule.date.take(10)
            daySchedule.lessons.forEach { lesson ->
                lesson.periods.forEach periodLoop@{ period ->
                    val startTimeStr = period.timeStart?.take(5) ?: return@periodLoop
                    val disciplineName = period.disciplineShortName?.takeIf { it.isNotEmpty() }
                        ?: period.disciplineFullName ?: "Пара"

                    reminderOffsets.forEach { minutesBefore ->
                        val intent = Intent(context, AlarmReceiver::class.java)
                        val requestCode = "${cleanDateStr}_${startTimeStr}_${disciplineName}_$minutesBefore".hashCode()

                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                        )
                        if (pendingIntent != null) {
                            alarmManager.cancel(pendingIntent)
                            pendingIntent.cancel()
                        }
                    }
                }
            }
        }
        LogCollector.log("ExactAlarmScheduler", "Cancelled all alarms")
    }

    private fun getScheduleFromPreferences(context: Context): List<DaySchedule> {
        return try {
            val prefs = context.getSharedPreferences("schedule_cache", Context.MODE_PRIVATE)
            val scheduleJson = prefs.getString("cached_schedule", null)
            if (scheduleJson.isNullOrEmpty()) return emptyList()

            val type = object : TypeToken<List<DaySchedule>>() {}.type
            Gson().fromJson<List<DaySchedule>>(scheduleJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractFirstUrl(rawText: String?): String? {
        if (rawText.isNullOrBlank()) return null
        val match = Regex("""https?://[^\s"'<>);,]+""", RegexOption.IGNORE_CASE).find(rawText)
        return match?.value?.trimEnd('.', ',', ';', ')')
    }
}
