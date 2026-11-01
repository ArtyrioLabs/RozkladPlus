package com.mkr.schedule.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Lightweight privacy-conscious wrapper for Firebase Analytics and Crashlytics.
 */
object AppAnalytics {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        try {
            val analytics = FirebaseAnalytics.getInstance(context.applicationContext)
            firebaseAnalytics = analytics
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

            val bundle = Bundle().apply {
                putString("app_version", com.mkr.schedule.BuildConfig.VERSION_NAME)
                putLong("timestamp", System.currentTimeMillis())
            }
            analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle)
        } catch (e: Exception) {
            LogCollector.log("AppAnalytics", "Failed to init Firebase: ${e.message}")
        }
    }

    fun logAppOpen() {
        try {
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
        } catch (_: Exception) {}
    }

    fun logScheduleView(groupName: String?, university: String?) {
        try {
            val bundle = Bundle().apply {
                putString("group_name", groupName ?: "none")
                putString("university", university ?: "none")
            }
            firebaseAnalytics?.logEvent("view_schedule", bundle)
        } catch (_: Exception) {}
    }

    fun logGroupSelected(groupName: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_NAME, groupName)
                putString(FirebaseAnalytics.Param.CONTENT_TYPE, "group")
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            FirebaseCrashlytics.getInstance().setCustomKey("selected_group", groupName)
        } catch (_: Exception) {}
    }

    fun logLanguageChanged(lang: String) {
        try {
            val bundle = Bundle().apply {
                putString("language", lang)
            }
            firebaseAnalytics?.logEvent("change_language", bundle)
            FirebaseCrashlytics.getInstance().setCustomKey("app_language", lang)
        } catch (_: Exception) {}
    }

    fun logCallScheduleView() {
        try {
            firebaseAnalytics?.logEvent("view_bell_schedule", null)
        } catch (_: Exception) {}
    }

    fun recordException(throwable: Throwable) {
        try {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        } catch (_: Exception) {}
    }
}
