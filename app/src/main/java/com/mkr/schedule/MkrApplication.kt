package com.mkr.schedule

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import com.mkr.schedule.util.LauncherIconColorSync
import com.mkr.schedule.util.LogCollector
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MkrApplication : Application() {
	override fun onCreate() {
		super.onCreate()

		val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

		Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
			LogCollector.log(
				"FatalCrash",
				"Thread=${thread.name}, ${throwable.message}\n${throwable.stackTraceToString()}"
			)
			previousHandler?.uncaughtException(thread, throwable)
		}

		LauncherIconColorSync.syncIfNeeded(this)
		LauncherIconColorSync.scheduleMonetWatcherJob(this)

		try {
			val filter = IntentFilter().apply {
				addAction(Intent.ACTION_CONFIGURATION_CHANGED)
				addAction(Intent.ACTION_WALLPAPER_CHANGED)
			}
			val receiver = object : BroadcastReceiver() {
				override fun onReceive(context: Context, intent: Intent?) {
					LauncherIconColorSync.syncIfNeeded(context)
				}
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
			} else {
				@Suppress("DEPRECATION")
				registerReceiver(receiver, filter)
			}
		} catch (e: Exception) {
			LogCollector.log("MkrApplication", "Failed to register config receiver: ${e.message}")
		}
	}

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		LauncherIconColorSync.syncIfNeeded(this)
	}
}

