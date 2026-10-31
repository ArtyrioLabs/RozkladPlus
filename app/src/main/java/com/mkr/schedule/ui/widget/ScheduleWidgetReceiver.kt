package com.mkr.schedule.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Re-render widget on system time/date or periodic widget updates
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            intent.action == Intent.ACTION_DATE_CHANGED
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val manager = GlanceAppWidgetManager(context)
                manager.getGlanceIds(ScheduleWidget::class.java).forEach { id ->
                    ScheduleWidget().update(context, id)
                }
            }
        }
    }
}
