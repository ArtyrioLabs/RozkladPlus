package com.mkr.schedule.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mkr.schedule.MainActivity
import com.mkr.schedule.R
import com.mkr.schedule.services.OngoingLessonService
import com.mkr.schedule.util.LogCollector
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class LessonNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "lesson_reminders_v2"
        const val ONGOING_CHANNEL_ID = "ongoing_lesson_live_v3"

        fun formatClassroom(rawRoom: String): String {
            val trimmed = rawRoom.trim()
            if (trimmed.isEmpty() || trimmed == "?") return "Ауд. не вказана"
            if (trimmed.contains("ONLINE", ignoreCase = true) ||
                trimmed.contains("ON-LINE", ignoreCase = true) ||
                trimmed.contains("ON LINE", ignoreCase = true) ||
                trimmed.contains("ДИСТАНЦ", ignoreCase = true)
            ) {
                return "Онлайн"
            }
            val cleaned = trimmed
                .replace(Regex("^(аудиторія|ауд\\.?|каб\\.?)\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
            return if (cleaned.isNotEmpty()) "Ауд. $cleaned" else trimmed
        }

        fun resolveLessonTypeLabel(typeCode: Int?, shortName: String, fullName: String): String {
            val source = "$shortName $fullName".lowercase(Locale.getDefault())
            if (source.contains("екз") || source.contains("іспит") || source.contains("залік")) return "Екзамен"
            if (source.contains("лк") || source.contains("лекц")) return "Лекція"
            if (source.contains("пз") || source.contains("практ") || source.contains("сем")) return "Практика"
            if (source.contains("лб") || source.contains("лаб")) return "Лабораторна"
            return when (typeCode) {
                1 -> "Лекція"
                2 -> "Практика"
                3 -> "Лабораторна"
                4 -> "Семінар"
                5 -> "Екзамен"
                else -> "Заняття"
            }
        }

        fun parseTimeTodayMillis(timeStr: String): Long {
            return try {
                val clean = timeStr.take(5)
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = sdf.parse(clean) ?: return 0L
                val calendar = Calendar.getInstance()
                val timeCal = Calendar.getInstance().apply { time = date }
                calendar.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            } catch (_: Exception) {
                0L
            }
        }
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_ID,
                "Нагадування про пари",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Сповіщення про початок та відлік часу до кінця пари"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(reminderChannel)

            // IMPORTANCE_DEFAULT without sound keeps the notification in the active shade
            // and enables status bar countdown chip on modern Android versions (12 to 16).
            val ongoingChannel = NotificationChannel(
                ONGOING_CHANNEL_ID,
                "Таймер поточної пари",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Зворотний відлік часу до кінця поточної пари в статус-барі"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(ongoingChannel)
        }
    }

    fun showLessonReminder(
        subjectName: String,
        subjectShortName: String,
        teacher: String,
        classroom: String,
        startTime: String,
        endTime: String = "",
        lessonNumber: Int = 0,
        lessonType: String = "Пара",
        reminderMinutes: Int,
        lessonDate: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pairBadge = if (lessonNumber > 0) "$lessonNumber пара" else ""
        val cleanRoom = formatClassroom(classroom)
        val startClean = startTime.take(5)
        val endClean = endTime.take(5).ifEmpty {
            try {
                LocalTime.parse(startClean, DateTimeFormatter.ofPattern("HH:mm"))
                    .plusMinutes(80)
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
            } catch (_: Exception) {
                ""
            }
        }

        val subText = listOf(pairBadge, lessonType, if (endClean.isNotEmpty()) "до $endClean" else "")
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        val displaySubject = subjectName.takeIf { it.isNotBlank() } ?: subjectShortName
        val title = when {
            reminderMinutes > 0 && pairBadge.isNotEmpty() ->
                "Через $reminderMinutes хв • $pairBadge: $subjectShortName"
            reminderMinutes > 0 ->
                "Через $reminderMinutes хв • $subjectShortName"
            pairBadge.isNotEmpty() ->
                "$pairBadge • $subjectShortName"
            else ->
                "Зараз триває • $subjectShortName"
        }

        val timeSpan = if (endClean.isNotEmpty()) "$startClean–$endClean" else startClean
        val cleanTeacher = teacher.takeIf { it.isNotBlank() && it != "N/A" }
        val subtitle = listOfNotNull(
            timeSpan.takeIf { it.isNotBlank() },
            cleanRoom,
            cleanTeacher
        ).joinToString(" • ")

        val countdownTargetMillis = if (reminderMinutes > 0) {
            val parsedStart = parseTimeTodayMillis(startClean)
            if (parsedStart > System.currentTimeMillis()) parsedStart else System.currentTimeMillis() + reminderMinutes * 60_000L
        } else {
            val parsedEnd = parseTimeTodayMillis(endClean)
            if (parsedEnd > System.currentTimeMillis()) parsedEnd else System.currentTimeMillis() + 80 * 60_000L
        }

        val notificationId = buildNotificationId(
            lessonDate = lessonDate,
            startTime = startClean,
            subjectShortName = subjectShortName,
            reminderMinutes = reminderMinutes
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mkr)
            .setSubText(subText)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(subText)
                    .bigText("$displaySubject\n$subtitle")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setShowWhen(true)
            .setWhen(countdownTargetMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setColor(0xFF104A32.toInt())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            LogCollector.log("LessonNotification", "Skipping notification: POST_NOTIFICATIONS not granted")
            return
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            LogCollector.log("LessonNotification", "Failed to show notification: ${e.message}")
        }
    }

    fun showLessonStarted(
        subjectName: String,
        teacher: String,
        classroom: String,
        startTime: String,
        endTime: String = "",
        lessonNumber: Int = 0,
        lessonType: String = "Пара",
        lessonDate: String
    ) {
        showLessonReminder(
            subjectName = subjectName,
            subjectShortName = subjectName,
            teacher = teacher,
            classroom = classroom,
            startTime = startTime,
            endTime = endTime,
            lessonNumber = lessonNumber,
            lessonType = lessonType,
            reminderMinutes = 0,
            lessonDate = lessonDate
        )
    }

    private fun buildNotificationId(
        lessonDate: String,
        startTime: String,
        subjectShortName: String,
        reminderMinutes: Int
    ): Int {
        val rawKey = "$lessonDate|$startTime|$subjectShortName|$reminderMinutes"
        return rawKey.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
    }

    fun buildOngoingNotification(
        subjectName: String,
        remainingTime: String,
        progress: Int,
        endTimeMillis: Long,
        lessonNumber: Int = 0,
        lessonType: String = "Пара",
        classroom: String = "",
        teacher: String = "",
        startTime: String = "",
        endTime: String = ""
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pairLabel = if (lessonNumber > 0) "$lessonNumber пара" else "Поточна пара"
        val endClean = endTime.take(5)
        val startClean = startTime.take(5)
        val subText = listOfNotNull(
            pairLabel,
            lessonType.takeIf { it.isNotBlank() },
            endClean.takeIf { it.isNotEmpty() }?.let { "до $it" }
        ).joinToString(" • ")

        val title = subjectName
        val cleanRoom = if (classroom.isNotBlank()) formatClassroom(classroom) else null
        val cleanTeacher = teacher.takeIf { it.isNotBlank() && it != "N/A" }
        val timeRange = if (startClean.isNotEmpty() && endClean.isNotEmpty()) "$startClean–$endClean" else null

        val detailsLine = listOfNotNull(
            cleanRoom,
            cleanTeacher,
            timeRange
        ).joinToString(" • ")

        val extras = Bundle().apply {
            putBoolean("android.requestPromotedOngoing", true)
        }

        val builder = NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mkr)
            .setSubText(subText)
            .setContentTitle(title)
            .setContentText(detailsLine)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setWhen(endTimeMillis)
            .setChronometerCountDown(true)
            .addExtras(extras)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF104A32.toInt())

        return builder.build()
    }

    fun showTestNotification() {
        val now = LocalTime.now()
        val startStr = now.minusMinutes(15).format(DateTimeFormatter.ofPattern("HH:mm"))
        val endStr = now.plusMinutes(65).format(DateTimeFormatter.ofPattern("HH:mm"))

        // Start ongoing countdown service so user sees the live countdown timer to the end of the lesson + status bar chip
        val serviceIntent = Intent(context, OngoingLessonService::class.java).apply {
            putExtra("lesson_name", "Основи національного спротиву")
            putExtra("lesson_number", 2)
            putExtra("lesson_type", "Лекція")
            putExtra("room", "ON-LINE")
            putExtra("teacher", "Захаров Р.Г.")
            putExtra("start_time", startStr)
            putExtra("end_time", endStr)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            LogCollector.log("LessonNotification", "Failed to start test ongoing service: ${e.message}")
        }

        showLessonReminder(
            subjectName = "Мобільні інформаційні технології",
            subjectShortName = "МІТ",
            teacher = "Іваненко О.П.",
            classroom = "205",
            startTime = now.plusMinutes(5).format(DateTimeFormatter.ofPattern("HH:mm")),
            endTime = now.plusMinutes(85).format(DateTimeFormatter.ofPattern("HH:mm")),
            lessonNumber = 3,
            lessonType = "Лекція",
            reminderMinutes = 5,
            lessonDate = LocalDate.now().toString()
        )
    }
}
