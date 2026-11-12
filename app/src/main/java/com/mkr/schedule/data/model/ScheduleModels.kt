package com.mkr.schedule.data.model

import com.google.gson.annotations.SerializedName

data class GroupTimeTableForm(
    @SerializedName("groupId") val groupId: Int,
    @SerializedName("dateStart") val dateStart: String,
    @SerializedName("dateEnd") val dateEnd: String
)

data class StudentTimeTableForm(
    @SerializedName("studentId") val studentId: Int,
    @SerializedName("dateStart") val dateStart: String,
    @SerializedName("dateEnd") val dateEnd: String
)

data class TimeTableDate(
    val date: String,
    val lessons: List<TimeTableLesson>
)

data class TimeTableLesson(
    val number: Int,
    val periods: List<TimeTablePeriod>
)

data class TimeTablePeriod(
    val r1: Int?,
    val rz14: Int?,
    val rz15: Int?,
    val r5: Int?,
    @SerializedName("disciplineId") val disciplineId: Int?,
    @SerializedName("educationDisciplineId") val educationDisciplineId: Int?,
    @SerializedName("disciplineFullName") val disciplineFullName: String?,
    @SerializedName("disciplineShortName") val disciplineShortName: String?,
    val classroom: String?,
    @SerializedName("timeStart") val timeStart: String?,
    @SerializedName("timeEnd") val timeEnd: String?,
    @SerializedName("teachersName") val teachersName: String?,
    @SerializedName("teachersNameFull") val teachersNameFull: String?,
    val type: Int?,
    @SerializedName("dateUpdated") val dateUpdated: String?,
    @SerializedName("nonstandardTime") val nonstandardTime: Boolean?,
    val groups: String?,
    @SerializedName("extraText") val extraText: Boolean?
)

data class CallSchedule(
    @SerializedName("timeStart") val timeStart: String,
    @SerializedName("timeEnd") val timeEnd: String,
    val number: Int,
    val length: Double
)

// Запрос объявления к конкретной паре (по коду занятия r1 и дате r2)
data class ScheduleAdForm(
    @SerializedName("r1") val r1: Int,
    @SerializedName("r2") val r2: String
)

// Ответ МКР с HTML-объявлением от преподавателя (обычно тут лежит ссылка на Zoom/Meet)
data class ScheduleAdResponse(
    @SerializedName("html") val html: String?
)


