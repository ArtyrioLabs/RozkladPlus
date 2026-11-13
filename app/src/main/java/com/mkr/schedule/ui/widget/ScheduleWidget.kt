package com.mkr.schedule.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
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
 * Адаптивный Material 3 Expressive виджет без скролла:
 * подстраивает плотность, шрифты и количество следующих пар под текущий размер на экране.
 */
class ScheduleWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

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
                WidgetExpressiveContent(schedules, LocalSize.current.width, LocalSize.current.height)
            }
        }
    }

    @Composable
    private fun WidgetExpressiveContent(
        schedules: List<DaySchedule>,
        width: Dp,
        height: Dp
    ) {
        val colors = GlanceTheme.colors
        val now = Date()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.forLanguageTag("uk-UA"))
        val todayStr = isoFormat.format(now)
        val nowStr = timeFormat.format(now)

        val todaySchedule = schedules.find { it.date.startsWith(todayStr) }
        val todayEntries = extractEntries(todaySchedule)

        val hasRemainingToday = todayEntries.any { entry ->
            val end = entry.period.timeEnd?.take(5) ?: ""
            end.isNotEmpty() && end >= nowStr
        }

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

        // Подбираем плотность и число карточек под реальный размер виджета (без скролла)
        val isCompact = height < 140.dp || width < 210.dp
        val isTall = height >= 235.dp
        val maxUpcomingRows = when {
            isCompact -> 0
            height < 185.dp -> 1
            height < 235.dp -> 2
            height < 300.dp -> 3
            else -> 4
        }
        val outerPadding = if (isCompact) 10.dp else 14.dp
        val visibleUpcoming = remainingEntries.take(maxUpcomingRows)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.surface)
                .cornerRadius(24.dp)
                .padding(outerPadding)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Шапка виджета
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = if (isCompact) 6.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    if (!isCompact || groupName != null) {
                        Text(
                            text = if (groupName != null) "Розклад+ • $groupName" else "Розклад+",
                            style = TextStyle(
                                fontSize = if (isCompact) 10.sp else 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.primary
                            ),
                            maxLines = 1
                        )
                    }
                    Text(
                        text = headerDateText,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompact) 13.sp else 15.sp,
                            color = colors.onSurface
                        ),
                        maxLines = 1
                    )
                }
                if (entries.isNotEmpty() && !isCompact) {
                    Box(
                        modifier = GlanceModifier
                            .background(colors.secondaryContainer)
                            .cornerRadius(12.dp)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formatCount(entries.size),
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSecondaryContainer
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            if (heroEntry != null) {
                val isNow = heroEntry == currentEntry
                val p = heroEntry.period
                val timeStart = p.timeStart?.take(5) ?: "--:--"
                val timeEnd = p.timeEnd?.take(5) ?: ""
                val isExam = isExamPeriod(p)
                val typeText = resolveTypeLabel(p)
                val pairBadge = if (heroEntry.lessonNumber > 0) "${heroEntry.lessonNumber} пара" else "Пара"
                val statusPrefix = when {
                    isNow -> "Зараз"
                    isShowingToday -> "Далі"
                    else -> "Перша"
                }
                val topBadgeText = "$statusPrefix • $pairBadge • $typeText"
                val subjectTitle = p.disciplineFullName?.takeIf { it.isNotBlank() && it.length <= 44 }
                    ?: p.disciplineShortName?.takeIf { it.isNotBlank() }
                    ?: p.disciplineFullName
                    ?: "Дисципліна"
                val roomText = formatRoom(p.classroom)
                val teacherText = p.teachersName?.takeIf { it.isNotBlank() }
                    ?: p.teachersNameFull?.takeIf { it.isNotBlank() }
                val metaLine = if (isCompact) {
                    roomText
                } else {
                    listOfNotNull(roomText, teacherText).joinToString(" • ")
                }

                val heroBg = when {
                    isExam -> colors.errorContainer
                    isNow -> colors.primaryContainer
                    else -> colors.secondaryContainer
                }
                val heroBadgeColor = when {
                    isExam -> colors.onErrorContainer
                    isNow -> colors.onPrimaryContainer
                    else -> colors.primary
                }
                val heroTitleColor = when {
                    isExam -> colors.onErrorContainer
                    isNow -> colors.onPrimaryContainer
                    else -> colors.onSecondaryContainer
                }
                val timePillBg = when {
                    isNow -> colors.primary
                    else -> colors.surface
                }
                val timePillFg = when {
                    isNow -> colors.onPrimary
                    else -> colors.onSurface
                }

                // Главная карточка текущей/следующей пары
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(heroBg)
                        .cornerRadius(if (isCompact) 16.dp else 20.dp)
                        .padding(if (isCompact) 9.dp else 12.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .background(timePillBg)
                                .cornerRadius(if (isCompact) 11.dp else 14.dp)
                                .padding(
                                    horizontal = if (isCompact) 8.dp else 10.dp,
                                    vertical = if (isCompact) 6.dp else 8.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = timeStart,
                                    style = TextStyle(
                                        fontSize = if (isCompact) 13.sp else 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = timePillFg
                                    )
                                )
                                if (timeEnd.isNotEmpty() && !isCompact) {
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

                        Spacer(modifier = GlanceModifier.width(if (isCompact) 9.dp else 12.dp))

                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = topBadgeText,
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = heroBadgeColor
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = subjectTitle,
                                style = TextStyle(
                                    fontSize = if (isCompact) 13.sp else 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = heroTitleColor
                                ),
                                maxLines = if (isCompact) 1 else 2
                            )
                            if (metaLine.isNotEmpty()) {
                                Text(
                                    text = metaLine,
                                    style = TextStyle(
                                        fontSize = if (isCompact) 10.sp else 11.sp,
                                        color = heroTitleColor
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Следующие пары (адаптивное число строк без скролла)
                if (visibleUpcoming.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(if (isTall) 8.dp else 6.dp))
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        visibleUpcoming.forEachIndexed { idx, entry ->
                            val itemPeriod = entry.period
                            val itemName = itemPeriod.disciplineShortName?.takeIf { it.isNotBlank() }
                                ?: itemPeriod.disciplineFullName
                                ?: "Пара"
                            val itemStart = itemPeriod.timeStart?.take(5) ?: "--:--"
                            val itemRoom = formatRoom(itemPeriod.classroom)
                            val itemType = resolveTypeLabel(itemPeriod)
                            val itemIsExam = isExamPeriod(itemPeriod)

                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(if (itemIsExam) colors.errorContainer else colors.surfaceVariant)
                                    .cornerRadius(13.dp)
                                    .padding(horizontal = 10.dp, vertical = if (isTall) 7.dp else 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = itemStart,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (itemIsExam) colors.onErrorContainer else colors.onSurface
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Text(
                                    text = itemName,
                                    style = TextStyle(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (itemIsExam) colors.onErrorContainer else colors.onSurface
                                    ),
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                Text(
                                    text = "$itemType • $itemRoom",
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        color = if (itemIsExam) colors.onErrorContainer else colors.onSurfaceVariant
                                    ),
                                    maxLines = 1
                                )
                            }
                            if (idx < visibleUpcoming.lastIndex) {
                                Spacer(modifier = GlanceModifier.height(4.dp))
                            }
                        }

                        val hiddenCount = remainingEntries.size - visibleUpcoming.size
                        if (hiddenCount > 0) {
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = "+ ще $hiddenCount ${formatWordOnly(hiddenCount)}",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.primary
                                ),
                                maxLines = 1
                            )
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
                            fontSize = if (isCompact) 12.sp else 13.sp,
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

    private fun isExamPeriod(period: TimeTablePeriod): Boolean {
        val typeTag = period.typeStr?.trim()?.lowercase(Locale.getDefault()).orEmpty()
        val text = "$typeTag ${period.disciplineShortName ?: ""} ${period.disciplineFullName ?: ""}".lowercase(Locale.getDefault())
        return typeTag.contains("екз") || typeTag.contains("экз") || typeTag.contains("exam") ||
            text.contains("екзам") || text.contains("іспит") || period.type == 5
    }

    private fun resolveTypeLabel(period: TimeTablePeriod): String {
        val typeTag = period.typeStr?.trim()?.lowercase(Locale.getDefault()).orEmpty()
        val text = "$typeTag ${period.disciplineShortName ?: ""} ${period.disciplineFullName ?: ""}".lowercase(Locale.getDefault())
        return when {
            isExamPeriod(period) -> "Екзамен"
            typeTag.contains("зал") || text.contains("залік") || period.type == 6 -> "Залік"
            typeTag.contains("конс") || text.contains("консульт") || period.type == 4 -> "Консульт."
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

    private fun formatCount(count: Int): String = "$count ${formatWordOnly(count)}"

    private fun formatWordOnly(count: Int): String {
        val rem100 = count % 100
        val rem10 = count % 10
        return when {
            rem100 in 11..14 -> "пар"
            rem10 == 1 -> "пара"
            rem10 in 2..4 -> "пари"
            else -> "пар"
        }
    }
}
