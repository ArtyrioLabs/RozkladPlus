package com.mkr.schedule.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.mkr.schedule.MainActivity
import com.mkr.schedule.R
import com.mkr.schedule.data.api.MkrApiService
import com.mkr.schedule.data.local.PreferencesManager
import com.mkr.schedule.data.model.GroupTimeTableForm
import com.mkr.schedule.data.model.StudentTimeTableForm
import com.mkr.schedule.data.model.TimeTableDate
import com.mkr.schedule.data.model.University
import com.mkr.schedule.util.LogCollector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ScheduleChangeWorker @Inject constructor(
    @ApplicationContext private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val WORK_NAME = "schedule_change_check"
        private const val NOTIFICATION_CHANNEL_ID = "schedule_changes"
        private const val NOTIFICATION_ID = 2002

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<ScheduleChangeWorker>(
                6, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )

            LogCollector.log("ScheduleChangeWorker", "Schedule change checker scheduled")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            LogCollector.log("ScheduleChangeWorker", "Schedule change checker cancelled")
        }
    }

    override suspend fun doWork(): Result {
        LogCollector.log("ScheduleChangeWorker", "Starting schedule change check")

        try {
            val preferencesManager = PreferencesManager(context)
            val studentId = preferencesManager.selectedStudentId.first()?.toIntOrNull()
            val groupId = preferencesManager.selectedGroupId.first()?.toIntOrNull()

            if (studentId == null && groupId == null) {
                LogCollector.log("ScheduleChangeWorker", "No student or group selected, skipping")
                return Result.success()
            }

            val universityId = preferencesManager.selectedUniversityId.first()
            val university = University.MKR_UNIVERSITIES.firstOrNull { it.id == universityId } ?: return Result.success()

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStart = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            val dateEnd = dateFormat.format(calendar.time)

            var schedule: List<TimeTableDate>? = null
            for (url in university.getAlternativeUrls()) {
                try {
                    val retrofit = Retrofit.Builder()
                        .baseUrl(url)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val apiService = retrofit.create(MkrApiService::class.java)

                    val response = if (studentId != null) {
                        apiService.getStudentTimeTablePublic(
                            "",
                            StudentTimeTableForm(studentId, dateStart, dateEnd)
                        )
                    } else {
                        apiService.getGroupTimeTable(
                            "",
                            GroupTimeTableForm(groupId!!, dateStart, dateEnd)
                        )
                    }

                    if (response.isSuccessful && response.body() != null) {
                        schedule = response.body()
                        break
                    }
                } catch (_: Exception) {
                    continue
                }
            }

            if (schedule != null) {
                val prefs = context.getSharedPreferences("schedule_cache", Context.MODE_PRIVATE)
                val targetKey = studentId?.let { "s_$it" } ?: "g_$groupId"
                val prefix = "schedule_hash_${university.id}_${targetKey}_"
                val byDate = schedule.associateBy { it.date.take(10) }
                val editor = prefs.edit()
                var changed = false
                val day = Calendar.getInstance()
                repeat(8) {
                    val date = dateFormat.format(day.time)
                    val hash = calculateScheduleHash(listOfNotNull(byDate[date]))
                    val key = prefix + date
                    val previous = prefs.getString(key, null)
                    if (previous != null && previous != hash) changed = true
                    editor.putString(key, hash)
                    day.add(Calendar.DAY_OF_YEAR, 1)
                }
                editor.apply()
                if (changed && preferencesManager.getNotificationsEnabled()) {
                    sendScheduleChangeNotification()
                }
                return Result.success()
            } else {
                LogCollector.log("ScheduleChangeWorker", "Failed to load schedule from all URLs")
                return Result.retry()
            }
        } catch (e: Exception) {
            LogCollector.log("ScheduleChangeWorker", "Error: ${e.message}")
            return Result.retry()
        }
    }

    private fun calculateScheduleHash(schedule: List<TimeTableDate>): String =
        com.mkr.schedule.util.ScheduleFingerprint.calculate(schedule)

    private fun sendScheduleChangeNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            LogCollector.log("ScheduleChangeWorker", "Skipping schedule-change notification: POST_NOTIFICATIONS not granted")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Зміни розкладу",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Сповіщення про зміни в розкладі"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mkr)
            .setContentTitle("Розклад змінився")
            .setContentText("У вашому розкладі з'явилися зміни. Натисніть, щоб переглянути")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("У вашому розкладі з'явилися зміни.\n\nРекомендуємо перевірити актуальний розклад занять.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF2457E6.toInt())
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            LogCollector.log("ScheduleChangeWorker", "Failed to show schedule-change notification: ${e.message}")
        }
    }
}
