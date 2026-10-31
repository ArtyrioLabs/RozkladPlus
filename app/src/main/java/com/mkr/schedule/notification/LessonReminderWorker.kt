package com.mkr.schedule.notification

import android.content.Context
import android.content.Intent
import androidx.work.*
import com.mkr.schedule.data.api.MkrApiService
import com.mkr.schedule.data.local.PreferencesManager
import com.mkr.schedule.data.model.GroupTimeTableForm
import com.mkr.schedule.data.model.StudentTimeTableForm
import com.mkr.schedule.data.model.TimeTableDate
import com.mkr.schedule.data.model.University
import com.mkr.schedule.util.LogCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class LessonReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val reminderWindows = mapOf(
        15 to 14L..16L,
        5 to 4L..6L
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            LogCollector.log("LessonReminder", "Checking for upcoming lessons...")

            val preferencesManager = PreferencesManager(applicationContext)
            val notificationsEnabled = preferencesManager.getNotificationsEnabled()

            if (!notificationsEnabled) {
                LogCollector.log("LessonReminder", "Notifications disabled, skipping")
                return@withContext Result.success()
            }

            val studentId = preferencesManager.selectedStudentId.first()?.toIntOrNull()
            val groupId = preferencesManager.selectedGroupId.first()?.toIntOrNull()

            if (studentId == null && groupId == null) {
                LogCollector.log("LessonReminder", "No student or group selected, skipping")
                return@withContext Result.success()
            }

            val universityId = preferencesManager.selectedUniversityId.first()
            val university = University.MKR_UNIVERSITIES.firstOrNull { it.id == universityId }
                ?: University.MKR_UNIVERSITIES.first()

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

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
                            token = "",
                            form = StudentTimeTableForm(studentId, today, tomorrow)
                        )
                    } else {
                        apiService.getGroupTimeTable(
                            token = "",
                            form = GroupTimeTableForm(groupId!!, today, tomorrow)
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
                val now = LocalDateTime.now()
                val notificationManager = LessonNotificationManager(applicationContext)
                val reminderPrefs = applicationContext.getSharedPreferences(
                    "lesson_reminder_state",
                    Context.MODE_PRIVATE
                )

                schedule.forEach { day ->
                    val lessonDate = try {
                        LocalDate.parse(day.date.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        null
                    } ?: return@forEach

                    day.lessons.forEach { lesson ->
                        lesson.periods.forEach { period ->
                            val startTime = period.timeStart?.let {
                                try {
                                    LocalTime.parse(it.take(5), DateTimeFormatter.ofPattern("HH:mm"))
                                } catch (e: Exception) {
                                    null
                                }
                            } ?: return@forEach

                            val lessonDateTime = LocalDateTime.of(lessonDate, startTime)
                            val endTime = period.timeEnd?.let {
                                try {
                                    LocalTime.parse(it.take(5), DateTimeFormatter.ofPattern("HH:mm"))
                                } catch (_: Exception) {
                                    null
                                }
                            } ?: startTime.plusMinutes(80)
                            val lessonEndDateTime = LocalDateTime.of(lessonDate, endTime)
                            val endTimeStr = endTime.format(DateTimeFormatter.ofPattern("HH:mm"))

                            val subjectShort = period.disciplineShortName?.takeIf { it.isNotBlank() }
                                ?: period.disciplineFullName
                                ?: "Пара"
                            val subjectFull = period.disciplineFullName?.takeIf { it.isNotBlank() } ?: subjectShort
                            val lessonType = LessonNotificationManager.resolveLessonTypeLabel(
                                period.type,
                                subjectShort,
                                subjectFull
                            )
                            val room = period.classroom ?: "?"
                            val teacher = period.teachersNameFull ?: period.teachersName ?: "Невідомий викладач"

                            val timeDiff = java.time.Duration.between(now, lessonDateTime).toMinutes()

                            // 1. Manage ongoing foreground service for active lesson
                            if (now.isAfter(lessonDateTime) && now.isBefore(lessonEndDateTime)) {
                                LogCollector.log("LessonReminder", "Starting ongoing service for: $subjectFull")
                                val serviceIntent = Intent(applicationContext, com.mkr.schedule.services.OngoingLessonService::class.java).apply {
                                    putExtra("lesson_name", subjectFull)
                                    putExtra("lesson_number", lesson.number)
                                    putExtra("lesson_type", lessonType)
                                    putExtra("room", room)
                                    putExtra("teacher", teacher)
                                    putExtra("start_time", period.timeStart?.take(5))
                                    putExtra("end_time", endTimeStr)
                                }
                                try {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        applicationContext.startForegroundService(serviceIntent)
                                    } else {
                                        applicationContext.startService(serviceIntent)
                                    }
                                } catch (e: Exception) {
                                    LogCollector.log("LessonReminder", "Failed to start ongoing service: ${e.message}")
                                }
                            }

                            // 2. Schedule heads-up advance reminder
                            if (timeDiff < 0) return@forEach

                            reminderWindows.forEach { (minutesBefore, window) ->
                                if (timeDiff in window) {
                                    val reminderKey = buildReminderKey(
                                        lessonDate = day.date.take(10),
                                        period = period,
                                        minutesBefore = minutesBefore
                                    )

                                    if (!reminderPrefs.getBoolean(reminderKey, false)) {
                                        LogCollector.log(
                                            "LessonReminder",
                                            "Sending $minutesBefore min reminder for: $subjectShort (${period.timeStart})"
                                        )
                                        notificationManager.showLessonReminder(
                                            subjectName = subjectFull,
                                            subjectShortName = subjectShort,
                                            teacher = teacher,
                                            classroom = room,
                                            startTime = period.timeStart?.take(5) ?: "",
                                            endTime = endTimeStr,
                                            lessonNumber = lesson.number,
                                            lessonType = lessonType,
                                            reminderMinutes = minutesBefore,
                                            lessonDate = day.date.take(10)
                                        )
                                        reminderPrefs.edit().putBoolean(reminderKey, true).apply()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            LogCollector.log("LessonReminder", "Error: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "lesson_reminder_work"

        private fun buildReminderKey(
            lessonDate: String,
            period: com.mkr.schedule.data.model.TimeTablePeriod,
            minutesBefore: Int
        ): String {
            val lessonCode = period.disciplineId?.toString()
                ?: period.disciplineShortName
                ?: period.disciplineFullName
                ?: "unknown"
            return "${lessonDate}_${period.timeStart?.take(5)}_${lessonCode}_$minutesBefore"
        }

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<LessonReminderWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )

            val oneTimeRequest = OneTimeWorkRequestBuilder<LessonReminderWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "lesson_reminder_now",
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )

            LogCollector.log("LessonReminder", "Worker scheduled")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            LogCollector.log("LessonReminder", "Worker cancelled")
        }
    }
}
