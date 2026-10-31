package com.mkr.schedule.services

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.mkr.schedule.notification.LessonNotificationManager
import com.mkr.schedule.util.LogCollector
import java.util.Locale

class OngoingLessonService : Service() {

    private lateinit var notificationManager: LessonNotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private var lessonName: String = ""
    private var lessonNumber: Int = 0
    private var lessonType: String = "Пара"
    private var classroom: String = ""
    private var teacher: String = ""
    private var startTimeStr: String = ""
    private var endTimeStr: String = ""
    private var startTimeMillis: Long = 0
    private var endTimeMillis: Long = 0

    // Android's native Chronometer (setUsesChronometer + setChronometerCountDown + setWhen)
    // ticks every second automatically in SystemUI & status bar chip.
    // We only update the progress bar every 30 seconds so we never reset the chronometer tick.
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lessonName = intent?.getStringExtra("lesson_name") ?: "Пара"
        lessonNumber = intent?.getIntExtra("lesson_number", 0) ?: 0
        lessonType = intent?.getStringExtra("lesson_type") ?: "Пара"
        classroom = intent?.getStringExtra("room") ?: ""
        teacher = intent?.getStringExtra("teacher") ?: ""
        startTimeStr = intent?.getStringExtra("start_time") ?: ""
        endTimeStr = intent?.getStringExtra("end_time") ?: ""

        if (startTimeStr.isEmpty() || endTimeStr.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startTimeMillis = LessonNotificationManager.parseTimeTodayMillis(startTimeStr)
        endTimeMillis = LessonNotificationManager.parseTimeTodayMillis(endTimeStr)

        if (endTimeMillis <= System.currentTimeMillis()) {
            stopSelf()
            return START_NOT_STICKY
        }

        notificationManager = LessonNotificationManager(this)
        handler.removeCallbacks(updateRunnable)

        val initialNotification = notificationManager.buildOngoingNotification(
            subjectName = lessonName,
            remainingTime = calculateRemainingTime(),
            progress = calculateProgress(),
            endTimeMillis = endTimeMillis,
            lessonNumber = lessonNumber,
            lessonType = lessonType,
            classroom = classroom,
            teacher = teacher,
            startTime = startTimeStr,
            endTime = endTimeStr
        )

        startForeground(NOTIFICATION_ID, initialNotification)
        handler.postDelayed(updateRunnable, 30_000L)

        return START_STICKY
    }

    private fun updateNotification() {
        val now = System.currentTimeMillis()
        if (now >= endTimeMillis) {
            LogCollector.log("OngoingService", "Lesson ended, stopping service")
            stopSelf()
            return
        }

        val notification = notificationManager.buildOngoingNotification(
            subjectName = lessonName,
            remainingTime = calculateRemainingTime(),
            progress = calculateProgress(),
            endTimeMillis = endTimeMillis,
            lessonNumber = lessonNumber,
            lessonType = lessonType,
            classroom = classroom,
            teacher = teacher,
            startTime = startTimeStr,
            endTime = endTimeStr
        )

        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun calculateProgress(): Int {
        val now = System.currentTimeMillis()
        val total = endTimeMillis - startTimeMillis
        if (total <= 0) return 100
        val elapsed = now - startTimeMillis
        return ((elapsed.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
    }

    private fun calculateRemainingTime(): String {
        val now = System.currentTimeMillis()
        val remainingMillis = endTimeMillis - now
        if (remainingMillis <= 0) return "00:00"
        val totalSeconds = remainingMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2024
    }
}
