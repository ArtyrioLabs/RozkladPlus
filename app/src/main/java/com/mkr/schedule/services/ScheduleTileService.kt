package com.mkr.schedule.services

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mkr.schedule.data.local.PreferencesManager
import com.mkr.schedule.data.model.TimeTableDate
import com.mkr.schedule.util.LogCollector
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleTileService : TileService() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val openPassIntent = Intent(this, com.mkr.schedule.MainActivity::class.java).apply {
            action = "com.mkr.schedule.ACTION_SHOW_STUDENT_CARD"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                1001,
                openPassIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(openPassIntent)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        
        try {
            val schedule = getScheduleFromCache()
            if (schedule.isNullOrEmpty()) {
                tile.label = "Розклад порожній"
                tile.state = Tile.STATE_INACTIVE
                tile.updateTile()
                return
            }

            val now = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = dateFormat.format(now.time)
            
            val todaySchedule = schedule.find { it.date.startsWith(todayStr) }
            if (todaySchedule == null) {
                tile.label = "Немає пар сьогодні"
                tile.state = Tile.STATE_INACTIVE
                tile.updateTile()
                return
            }

            val currentTime = String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
            
            // Find ongoing or next upcoming lesson for quick settings tile display
            val currentPair = todaySchedule.lessons.find { lesson ->
                lesson.periods.any { period ->
                    val start = period.timeStart ?: ""
                    val end = period.timeEnd ?: ""
                    start.isNotEmpty() && end.isNotEmpty() && currentTime >= start && currentTime <= end
                }
            }

            if (currentPair != null) {
                val name = currentPair.periods.firstOrNull()?.disciplineShortName ?: "Пара"
                tile.label = "Зараз: $name"
                tile.state = Tile.STATE_ACTIVE
            } else {
                val nextPair = todaySchedule.lessons.filter { lesson ->
                    lesson.periods.any { it.timeStart ?: "" > currentTime }
                }.minByOrNull { it.number }

                if (nextPair != null) {
                    val nextTime = nextPair.periods.firstOrNull()?.timeStart ?: ""
                    val name = nextPair.periods.firstOrNull()?.disciplineShortName ?: "Пара"
                    tile.label = "Наст: $nextTime $name"
                    tile.state = Tile.STATE_INACTIVE
                } else {
                    tile.label = "Пар більше немає"
                    tile.state = Tile.STATE_INACTIVE
                }
            }
        } catch (e: Exception) {
            tile.label = "MKR Розклад"
            tile.state = Tile.STATE_UNAVAILABLE
        }
        
        tile.updateTile()
    }

    private fun getScheduleFromCache(): List<TimeTableDate>? {
        return try {
            val prefs = preferencesManager.getScheduleCachePrefs()
            val rawJson = prefs.getString("cached_schedule_raw", null)
            if (rawJson.isNullOrEmpty()) return null
            val type = object : TypeToken<List<TimeTableDate>>() {}.type
            Gson().fromJson(rawJson, type)
        } catch (e: Exception) { null }
    }
}
