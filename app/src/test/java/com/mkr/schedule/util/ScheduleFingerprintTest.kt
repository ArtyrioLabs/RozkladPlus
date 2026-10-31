package com.mkr.schedule.util

import com.google.gson.Gson
import com.mkr.schedule.data.model.TimeTableDate
import com.mkr.schedule.data.model.University
import org.junit.Assert.*
import org.junit.Test

class ScheduleFingerprintTest {
    private fun day(
        end: String = "10:00",
        classroom: String = "205",
        teacher: String = "Іваненко О.П."
    ): TimeTableDate = Gson().fromJson(
        """{"date":"2026-10-30","lessons":[{"number":1,"periods":[{"disciplineId":1,"timeStart":"08:30","timeEnd":"$end","classroom":"$classroom","teachersNameFull":"$teacher"}]}]}""",
        TimeTableDate::class.java
    )

    @Test
    fun detectsChangedEndTime() {
        assertNotEquals(
            ScheduleFingerprint.calculate(listOf(day())),
            ScheduleFingerprint.calculate(listOf(day(end = "10:10")))
        )
    }

    @Test
    fun detectsChangedClassroomOrTeacher() {
        assertNotEquals(
            ScheduleFingerprint.calculate(listOf(day())),
            ScheduleFingerprint.calculate(listOf(day(classroom = "ONLINE")))
        )
        assertNotEquals(
            ScheduleFingerprint.calculate(listOf(day())),
            ScheduleFingerprint.calculate(listOf(day(teacher = "Петренко В.В.")))
        )
    }

    @Test
    fun ignoresResponseOrder() {
        val first = day()
        val second = first.copy(date = "2026-10-31")
        assertEquals(
            ScheduleFingerprint.calculate(listOf(first, second)),
            ScheduleFingerprint.calculate(listOf(second, first))
        )
    }

    @Test
    fun detectsRemovedLessons() {
        assertNotEquals(
            ScheduleFingerprint.calculate(listOf(day())),
            ScheduleFingerprint.calculate(listOf(day().copy(lessons = emptyList())))
        )
    }

    @Test
    fun universityBaseUrlsAlwaysEndWithSlash() {
        University.MKR_UNIVERSITIES.forEach { university ->
            assertTrue(
                "Retrofit baseUrl for ${university.shortName} must end with /",
                university.getRetrofitBaseUrl().endsWith("/")
            )
            university.getAlternativeUrls().forEach { altUrl ->
                assertTrue(
                    "Alternative URL $altUrl for ${university.shortName} must end with /",
                    altUrl.endsWith("/")
                )
            }
        }
    }
}
