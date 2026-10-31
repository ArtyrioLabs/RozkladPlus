package com.mkr.schedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mkr.schedule.data.local.PreferencesManager
import com.mkr.schedule.util.LogCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val preferencesManager = PreferencesManager(context.applicationContext)
                    if (preferencesManager.getNotificationsEnabled()) {
                        LogCollector.log("BootReceiver", "Device booted. Rescheduling alarms...")
                        LessonReminderWorker.schedule(context.applicationContext)
                        ExactAlarmScheduler.scheduleAllAlarms(context.applicationContext)
                    } else {
                        LogCollector.log("BootReceiver", "Device booted. Notifications disabled, skipping alarms.")
                    }
                    ScheduleChangeWorker.schedule(context.applicationContext)
                } catch (e: Exception) {
                    LogCollector.log("BootReceiver", "Error in BootReceiver: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
