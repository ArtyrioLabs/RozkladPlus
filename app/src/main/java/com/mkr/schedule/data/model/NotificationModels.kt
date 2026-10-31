package com.mkr.schedule.data.model

data class DaySchedule(
    val date: String,
    val lessons: List<NotificationLesson>
)

data class NotificationLesson(
    val numberPair: String? = null,
    val periods: List<TimeTablePeriod>
)
