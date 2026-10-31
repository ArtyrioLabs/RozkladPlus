package com.mkr.schedule.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.mkr.schedule.data.model.TimeTableDate
import com.mkr.schedule.util.LogCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class WatchSyncPayload(
    val profileTitle: String,
    val profileSubtitle: String,
    val groupId: Int,
    val studentId: Int,
    val isStudentMode: Boolean,
    val notificationsEnabled: Boolean,
    val showBreaks: Boolean,
    val schedule: List<TimeTableDate>,
    val syncedAtMs: Long
)

/**
 * Coordinates synchronization between the Android mobile app and Wear OS companion devices.
 *
 * Transmits active schedules, profile selections, and notification settings
 * via Google Play Services Wearable DataClient and MessageClient.
 */
object PhoneWearSyncManager {
    private const val PREFS_NAME = "wear_companion_prefs"
    private const val KEY_WATCH_NOTIF = "watch_notifications_enabled"
    private const val KEY_WATCH_BREAKS = "watch_show_breaks"
    private const val KEY_LAST_PROFILE_TITLE = "last_profile_title"
    private const val KEY_LAST_PROFILE_SUB = "last_profile_sub"
    private const val KEY_LAST_GROUP_ID = "last_group_id"
    private const val KEY_LAST_STUDENT_ID = "last_student_id"
    private const val KEY_LAST_IS_STUDENT = "last_is_student"
    private const val KEY_LAST_SCHEDULE_JSON = "last_schedule_json"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private val _lastSyncTimestamp = MutableStateFlow(0L)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isWatchNotificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WATCH_NOTIF, true)

    fun setWatchNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_WATCH_NOTIF, enabled).apply()
        pushSavedStateToWatch(context)
    }

    fun isWatchShowBreaksEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WATCH_BREAKS, true)

    fun setWatchShowBreaksEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_WATCH_BREAKS, enabled).apply()
        pushSavedStateToWatch(context)
    }

    suspend fun getConnectedWatchNames(context: Context): List<String> = withContext(Dispatchers.IO) {
        try {
            val nodes = Wearable.getNodeClient(context.applicationContext).connectedNodes.await()
            nodes.map { it.displayName.ifBlank { "Wear OS Watch" } }
        } catch (e: Exception) {
            LogCollector.log("PhoneWearSync", "Failed to query watch nodes: ${e.message}")
            emptyList()
        }
    }

    fun syncScheduleFromViewModel(
        context: Context,
        profileTitle: String,
        profileSubtitle: String,
        groupId: Int,
        studentId: Int,
        isStudentMode: Boolean,
        schedule: List<TimeTableDate>
    ) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val scheduleJson = gson.toJson(schedule)
                prefs(appContext).edit()
                    .putString(KEY_LAST_PROFILE_TITLE, profileTitle)
                    .putString(KEY_LAST_PROFILE_SUB, profileSubtitle)
                    .putInt(KEY_LAST_GROUP_ID, groupId)
                    .putInt(KEY_LAST_STUDENT_ID, studentId)
                    .putBoolean(KEY_LAST_IS_STUDENT, isStudentMode)
                    .putString(KEY_LAST_SCHEDULE_JSON, scheduleJson)
                    .apply()

                sendPayloadToWatch(
                    context = appContext,
                    profileTitle = profileTitle,
                    profileSubtitle = profileSubtitle,
                    groupId = groupId,
                    studentId = studentId,
                    isStudentMode = isStudentMode,
                    schedule = schedule
                )
            } catch (e: Exception) {
                LogCollector.log("PhoneWearSync", "Error syncing from ViewModel: ${e.message}")
            }
        }
    }

    fun pushSavedStateToWatch(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val p = prefs(appContext)
                val title = p.getString(KEY_LAST_PROFILE_TITLE, "ФІТ 2-5") ?: "ФІТ 2-5"
                val sub = p.getString(KEY_LAST_PROFILE_SUB, "ДТЕУ") ?: "ДТЕУ"
                val groupId = p.getInt(KEY_LAST_GROUP_ID, 3344)
                val studentId = p.getInt(KEY_LAST_STUDENT_ID, 87397)
                val isStudent = p.getBoolean(KEY_LAST_IS_STUDENT, true)
                val json = p.getString(KEY_LAST_SCHEDULE_JSON, null)
                val schedule: List<TimeTableDate> = if (!json.isNullOrBlank()) {
                    val type = object : com.google.gson.reflect.TypeToken<List<TimeTableDate>>() {}.type
                    gson.fromJson(json, type) ?: emptyList()
                } else emptyList()

                sendPayloadToWatch(
                    context = appContext,
                    profileTitle = title,
                    profileSubtitle = sub,
                    groupId = groupId,
                    studentId = studentId,
                    isStudentMode = isStudent,
                    schedule = schedule
                )
            } catch (e: Exception) {
                LogCollector.log("PhoneWearSync", "Failed to push saved state: ${e.message}")
            }
        }
    }

    private suspend fun sendPayloadToWatch(
        context: Context,
        profileTitle: String,
        profileSubtitle: String,
        groupId: Int,
        studentId: Int,
        isStudentMode: Boolean,
        schedule: List<TimeTableDate>
    ) {
        val now = System.currentTimeMillis()
        val payload = WatchSyncPayload(
            profileTitle = profileTitle,
            profileSubtitle = profileSubtitle,
            groupId = groupId,
            studentId = studentId,
            isStudentMode = isStudentMode,
            notificationsEnabled = isWatchNotificationsEnabled(context),
            showBreaks = isWatchShowBreaksEnabled(context),
            schedule = schedule,
            syncedAtMs = now
        )
        val payloadJson = gson.toJson(payload)

        // 1. Persist in Wearable DataClient so watch receives it even if temporarily disconnected
        try {
            val putDataReq = PutDataMapRequest.create("/rozklad/sync").apply {
                dataMap.putString("payload_json", payloadJson)
                dataMap.putLong("timestamp", now)
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(putDataReq).await()
        } catch (e: Exception) {
            LogCollector.log("PhoneWearSync", "DataClient put failed: ${e.message}")
        }

        // 2. Send urgent MessageClient message to all connected watch nodes
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val bytes = payloadJson.toByteArray(Charsets.UTF_8)
            for (node in nodes) {
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/rozklad/sync_now", bytes)
                    .await()
            }
            _lastSyncTimestamp.value = now
        } catch (e: Exception) {
            LogCollector.log("PhoneWearSync", "MessageClient send failed: ${e.message}")
        }
    }

    suspend fun sendTestNotificationToWatch(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val appContext = context.applicationContext
            val nodes = Wearable.getNodeClient(appContext).connectedNodes.await()
            if (nodes.isEmpty()) return@withContext false
            for (node in nodes) {
                Wearable.getMessageClient(appContext)
                    .sendMessage(node.id, "/rozklad/test_notification", byteArrayOf())
                    .await()
            }
            true
        } catch (e: Exception) {
            LogCollector.log("PhoneWearSync", "Test notification to watch failed: ${e.message}")
            false
        }
    }
}
