package com.mkr.schedule.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mkr.schedule.MainActivity
import com.mkr.schedule.data.model.DaySchedule
import com.mkr.schedule.data.model.TimeTablePeriod
import java.text.SimpleDateFormat
import java.util.*

private data class WidgetPeriodEntry(
    val lessonNumber: Int,
    val period: TimeTablePeriod
)

/**
 * Material 3 Expressive home screen widget implemented with Jetpack Glance.
 *
 * Dynamically renders daily schedules, bell times, and active lesson statuses
 * with adaptive layout scaling and Material You dynamic color support.
 */
class ScheduleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("schedule_cache", Context.MODE_PRIVATE)
        val json = prefs.getString("cached_schedule", null)

        var schedules: List<DaySchedule> = emptyList()
        if (!json.isNullOrEmpty()) {
            try {
                val listType = object : TypeToken<List<DaySchedule>>() {}.type
                schedules = Gson().fromJson(json, listType) ?: emptyList()
            } catch (_: Exception) {
            }
        }

        provideContent {
            GlanceTheme {
                WidgetExpressiveContent(schedules)
            }
        }
    }

    @Composable
    private fun WidgetExpressiveContent(schedules: List<DaySchedule>) {
        val colors = GlanceTheme.colors
        val now = Date()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMM", Locale.forLanguageTag("uk-UA"))
        val todayStr = isoFormat.format(now)
        val nowStr = timeFormat.format(now)

        val todaySchedule = schedules.find { it.date.startsWith(todayStr) }
        val todayEntries = extractEntries(todaySchedule)

        val hasRemainingToday = todayEntries.any { entry ->
            val end = entry.period.timeEnd?.take(5) ?: ""
            end.isNotEmpty() && end >= nowStr
        }

        // If today has ongoing/upcoming lessons, show today; otherwise show the next available day with lessons
        val targetDaySchedule: DaySchedule?
        val isShowingToday: Boolean
        if (hasRemainingToday && todaySchedule != null) {
            targetDaySchedule = todaySchedule
            isShowingToday = true
        } else {
            val nextDay = schedules
                .filter { it.date.take(10) > todayStr && extractEntries(it).isNotEmpty() }
                .minByOrNull { it.date.take(10) }
            if (nextDay != null) {
                targetDaySchedule = nextDay
                isShowingToday = false
            } else {
                targetDaySchedule = todaySchedule
                isShowingToday = true
            }
        }

        val entries = extractEntries(targetDaySchedule)
        val groupName = entries.firstOrNull()?.period?.groups?.trim()?.takeIf { it.isNotEmpty() }

        val headerDateText = if (targetDaySchedule != null) {
            val parsedDate = runCatching { isoFormat.parse(targetDaySchedule.date.take(10)) }.getOrNull()
            val formatted = if (parsedDate != null) {
                dateFormat.format(parsedDate).replaceFirstChar { it.uppercase() }
            } else {
                targetDaySchedule.date.take(10)
            }
            if (isShowingToday) "Сьогодні • $formatted" else formatted
        } else {
            dateFormat.format(now).replaceFirstChar { it.uppercase() }
        }

        val currentEntry = if (isShowingToday) {
            entries.firstOrNull { entry ->
                val start = entry.period.timeStart?.take(5)
                val end = entry.period.timeEnd?.take(5)
                start != null && end != null && start <= nowStr && nowStr <= end
            }
        } else null

        val nextEntry = if (isShowingToday) {
            entries.firstOrNull { entry ->
                val start = entry.period.timeStart?.take(5)
                start != null && start > nowStr
            }
        } else {
            entries.firstOrNull()
        }

        val heroEntry = currentEntry ?: nextEntry
        val remainingEntries = if (heroEntry != null) {
            entries.filter { entry ->
                val start = entry.period.timeStart?.take(5) ?: ""
                val heroStart = heroEntry.period.timeStart?.take(5) ?: ""
                entry != heroEntry && start >= heroStart
            }
        } else emptyList()

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.surface)
                .cornerRadius(26.dp)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Header Capsule Row
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = if (groupName != null) "Розклад+ • $groupName" else "Розклад+",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.primary
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = headerDateText,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.onSurface
                        ),
                        maxLines = 1
                    )
                }
                if (entries.isNotEmpty()) {
                    Box(
                        modifier = GlanceModifier
                            .background(colors.secondaryContainer)
                            .cornerRadius(12.dp)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formatCount(entries.size),
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSecondaryContainer
                            )
                        )
                    }
                }
            }

            if (heroEntry != null) {
                val isNow = heroEntry == currentEntry
                val p = heroEntry.period
                val timeStart = p.timeStart?.take(5) ?: "--:--"
                val timeEnd = p.timeEnd?.take(5) ?: ""
                val typeText = resolveTypeLabel(p)
                val pairBadge = if (heroEntry.lessonNumber > 0) "${heroEntry.lessonNumber} пара" else "Пара"
                val statusPrefix = when {
                    isNow -> "Зараз"
                    isShowingToday -> "Далі"
                    else -> "Перша пара"
                }
                val topBadgeText = "$statusPrefix • $pairBadge • $typeText"
                val subjectTitle = p.disciplineFullName?.takeIf { it.isNotBlank() && it.length <= 48 }
                    ?: p.disciplineShortName?.takeIf { it.isNotBlank() }
                    ?: p.disciplineFullName
                    ?: "Дисципліна"
                val roomText = formatRoom(p.classroom)
                val teacherText = p.teachersName?.takeIf { it.isNotBlank() }
                    ?: p.teachersNameFull?.takeIf { it.isNotBlank() }
                val metaLine = listOfNotNull(roomText, teacherText).joinToString(" • ")

                // Hero Capsule Card (Material 3 Expressive)
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(if (isNow) colors.primaryContainer else colors.surfaceVariant)
                        .cornerRadius(20.dp)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Time Pill (Matching ModernLessonCard)
                        Box(
                            modifier = GlanceModifier
                                .background(if (isNow) colors.primary else colors.surface)
                                .cornerRadius(14.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = timeStart,
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNow) colors.onPrimary else colors.onSurface
                                    )
                                )
                                if (timeEnd.isNotEmpty()) {
                                    Text(
                                        text = "до $timeEnd",
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            color = if (isNow) colors.onPrimary else colors.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = GlanceModifier.width(12.dp))

                        // Right Subject & Metadata
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = topBadgeText,
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNow) colors.onPrimaryContainer else colors.primary
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = subjectTitle,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNow) colors.onPrimaryContainer else colors.onSurface
                                ),
                                maxLines = 2
                            )
                            if (metaLine.isNotEmpty()) {
                                Text(
                                    text = metaLine,
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = if (isNow) colors.onPrimaryContainer else colors.onSurfaceVariant
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (remainingEntries.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        itemsIndexed(remainingEntries) { idx, entry ->
                            val prev = if (idx == 0) heroEntry else remainingEntries[idx - 1]
                            val gapPairs = if (prev.lessonNumber > 0 && entry.lessonNumber > prev.lessonNumber) {
                                (entry.lessonNumber - prev.lessonNumber - 1).coerceAtLeast(0)
                            } else 0

                            val itemPeriod = entry.period
                            val itemName = itemPeriod.disciplineShortName?.takeIf { it.isNotBlank() }
                                ?: itemPeriod.disciplineFullName
                                ?: "Пара"
                            val itemStart = itemPeriod.timeStart?.take(5) ?: "--:--"
                            val itemRoom = formatRoom(itemPeriod.classroom)
                            val itemType = resolveTypeLabel(itemPeriod)
                            val pairText = if (entry.lessonNumber > 0) "${entry.lessonNumber} пара" else itemType
                            val windowPrefix = if (gapPairs > 0) "Вікно • " else ""

                            Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                Row(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .background(colors.surfaceVariant)
                                        .cornerRadius(14.dp)
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = itemStart,
                                        style = TextStyle(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = colors.onSurface
                                        )
                                    )
                                    Spacer(modifier = GlanceModifier.width(10.dp))
                                    Column(modifier = GlanceModifier.defaultWeight()) {
                                        Text(
                                            text = itemName,
                                            style = TextStyle(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = colors.onSurface
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "$windowPrefix$pairText • $itemType • $itemRoom",
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                color = colors.onSurfaceVariant
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (schedules.isEmpty()) {
                            "Натисніть, щоб відкрити Розклад+"
                        } else {
                            "Занять за розкладом немає"
                        },
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }

    private fun extractEntries(daySchedule: DaySchedule?): List<WidgetPeriodEntry> {
        if (daySchedule == null) return emptyList()
        return daySchedule.lessons.flatMap { lesson ->
            val num = lesson.numberPair?.toIntOrNull() ?: 0
            lesson.periods.map { period ->
                WidgetPeriodEntry(lessonNumber = num, period = period)
            }
        }.sortedBy { it.period.timeStart ?: "" }
    }

    private fun resolveTypeLabel(period: TimeTablePeriod): String {
        val text = "${period.disciplineShortName ?: ""} ${period.disciplineFullName ?: ""}".lowercase(Locale.getDefault())
        return when {
            text.contains("лк") || text.contains("лекц") || period.type == 1 -> "Лекція"
            text.contains("пз") || text.contains("практ") || text.contains("сем") || period.type == 2 -> "Практика"
            text.contains("лб") || text.contains("лаб") || period.type == 3 -> "Лаб."
            else -> "Заняття"
        }
    }

    private fun formatRoom(raw: String?): String {
        val clean = raw?.trim() ?: return "Онлайн"
        if (clean.isEmpty()) return "Онлайн"
        if (clean.contains("ONLINE", ignoreCase = true) ||
            clean.contains("ON-LINE", ignoreCase = true) ||
            clean.contains("ДИСТАНЦ", ignoreCase = true)
        ) {
            return "Онлайн"
        }
        return if (clean.lowercase(Locale.getDefault()).startsWith("ауд")) clean else "Ауд. $clean"
    }

    private fun formatCount(count: Int): String {
        val rem100 = count % 100
        val rem10 = count % 10
        val word = when {
            rem100 in 11..14 -> "пар"
            rem10 == 1 -> "пара"
            rem10 in 2..4 -> "пари"
            else -> "пар"
        }
        return "$count $word"
    }
}
