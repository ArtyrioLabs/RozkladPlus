package com.mkr.schedule.util

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.mkr.schedule.R

object LauncherIconColorSync {
    private const val PREFS_NAME = "launcher_icon_sync_prefs"
    private const val KEY_LAST_SIGNATURE = "last_color_signature"
    private const val KEY_TOGGLE_STATE = "toggle_state"
    private const val JOB_ID = 4910

    fun computeSignature(context: Context): String {
        val uiNight = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val bg = runCatching { ContextCompat.getColor(context, R.color.compass_mint) }.getOrDefault(0)
        val fg = runCatching { ContextCompat.getColor(context, R.color.compass_forest) }.getOrDefault(0)
        val ivory = runCatching { ContextCompat.getColor(context, R.color.compass_ivory) }.getOrDefault(0)
        val overlayJson = runCatching {
            Settings.Secure.getString(context.contentResolver, "theme_customization_overlay_packages") ?: ""
        }.getOrDefault("")
        return "${uiNight}_${bg}_${fg}_${ivory}_${overlayJson.hashCode()}"
    }

    fun syncIfNeeded(context: Context, force: Boolean = false) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentSig = computeSignature(context)
            val lastSig = prefs.getString(KEY_LAST_SIGNATURE, null)
            if (force || currentSig != lastSig) {
                prefs.edit().putString(KEY_LAST_SIGNATURE, currentSig).apply()
                triggerLauncherIconCacheRefresh(context)
                // Also fire once more after RRO overlay propagation completes in Launcher
                Handler(Looper.getMainLooper()).postDelayed({
                    val delayedSig = computeSignature(context)
                    prefs.edit().putString(KEY_LAST_SIGNATURE, delayedSig).apply()
                    triggerLauncherIconCacheRefresh(context)
                }, 450L)
            }
        } catch (e: Exception) {
            LogCollector.log("IconColorSync", "Failed to sync launcher icon: ${e.message}")
        }
    }

    fun triggerLauncherIconCacheRefresh(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val nextEnabled = !prefs.getBoolean(KEY_TOGGLE_STATE, false)
            prefs.edit().putBoolean(KEY_TOGGLE_STATE, nextEnabled).apply()

            val component = ComponentName(context, IconColorSyncReceiver::class.java)
            val newState = if (nextEnabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            context.packageManager.setComponentEnabledSetting(
                component,
                newState,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            LogCollector.log("IconColorSync", "Failed setComponentEnabledSetting: ${e.message}")
        }
    }

    fun scheduleMonetWatcherJob(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            val jobScheduler = context.getSystemService(JobScheduler::class.java) ?: return
            val uri = Settings.Secure.getUriFor("theme_customization_overlay_packages")
            val triggerUri = JobInfo.TriggerContentUri(
                uri,
                JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS
            )
            val jobInfo = JobInfo.Builder(
                JOB_ID,
                ComponentName(context, MonetIconSyncJobService::class.java)
            )
                .addTriggerContentUri(triggerUri)
                .setTriggerContentUpdateDelay(50L)
                .setTriggerContentMaxDelay(250L)
                .build()
            jobScheduler.schedule(jobInfo)
        } catch (e: Exception) {
            LogCollector.log("IconColorSync", "Failed to schedule MonetIconSyncJobService: ${e.message}")
        }
    }
}

class IconColorSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Dummy receiver toggled via PackageManager to force LauncherApps.Callback.onPackageChanged
        // so Pixel Launcher evicts stale IconCache bitmaps when Monet wallpaper colors change.
    }
}

class MonetIconSyncJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        LauncherIconColorSync.syncIfNeeded(applicationContext, force = true)
        LauncherIconColorSync.scheduleMonetWatcherJob(applicationContext)
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}
