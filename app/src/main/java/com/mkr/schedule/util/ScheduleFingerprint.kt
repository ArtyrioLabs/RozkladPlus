package com.mkr.schedule.util

import com.google.gson.Gson
import com.mkr.schedule.data.model.TimeTableDate
import java.security.MessageDigest

/** A stable fingerprint of all lesson fields, independent of API response ordering. */
object ScheduleFingerprint {
    fun calculate(schedule: List<TimeTableDate>): String {
        val gson = Gson()
        val normalized = schedule.sortedBy { it.date }.map { day ->
            day.copy(lessons = day.lessons.sortedBy { it.number }.map { lesson ->
                lesson.copy(periods = lesson.periods.sortedBy { gson.toJson(it) })
            })
        }
        return MessageDigest.getInstance("SHA-256").digest(gson.toJson(normalized).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
