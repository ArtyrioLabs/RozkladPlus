package com.mkr.schedule.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mkr.schedule.data.model.TimeTableDate
import com.mkr.schedule.data.model.TimeTablePeriod
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScheduleGridView(
    schedule: List<TimeTableDate>,
    modifier: Modifier = Modifier
) {
    if (schedule.isEmpty()) return

    val horizontalScrollState = rememberScrollState()
    val dayFormat = remember { SimpleDateFormat("EEEE\ndd MMM", Locale("uk", "UA")) }
    val parseDate = { dateStr: String ->
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr.take(10))
        } catch (e: Exception) {
            null
        }
    }

    val sortedDays = schedule.sortedBy { it.date }
    val allUniquePeriods = schedule
        .flatMap { it.lessons }
        .flatMap { it.periods }
        .distinctBy { it.timeStart?.take(5) }
        .sortedBy { it.timeStart?.take(5) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(horizontalScrollState)) {
                Box(
                    modifier = Modifier
                        .width(68.dp)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Час",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                sortedDays.forEach { day ->
                    val dateObj = parseDate(day.date)
                    val dateTitle = if (dateObj != null) {
                        dayFormat.format(dateObj).replaceFirstChar { it.uppercase() }
                    } else {
                        day.date.take(10)
                    }

                    Box(
                        modifier = Modifier
                            .width(164.dp)
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = dateTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(horizontalScrollState)) {
                    Column(modifier = Modifier.width(68.dp)) {
                        allUniquePeriods.forEach { periodData ->
                            Box(
                                modifier = Modifier
                                    .height(140.dp)
                                    .fillMaxWidth()
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(
                                    modifier = Modifier.padding(top = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = periodData.timeStart?.take(5) ?: "",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    periodData.timeEnd?.take(5)?.let { end ->
                                        Text(
                                            text = end,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    sortedDays.forEach { day ->
                        Column(modifier = Modifier.width(164.dp)) {
                            allUniquePeriods.forEach { uniquePeriod ->
                                val currentPeriod = day.lessons
                                    .flatMap { it.periods }
                                    .find { it.timeStart?.take(5) == uniquePeriod.timeStart?.take(5) }

                                Box(
                                    modifier = Modifier
                                        .height(140.dp)
                                        .fillMaxWidth()
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                        .padding(4.dp)
                                ) {
                                    if (currentPeriod != null) {
                                        GridLessonCard(period = currentPeriod)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GridLessonCard(period: TimeTablePeriod) {
    val shortName = period.disciplineShortName?.takeIf { it.isNotBlank() }
        ?: period.disciplineFullName ?: "Пара"
    val classroom = period.classroom ?: ""

    val isOnline = classroom.contains("ONLINE", ignoreCase = true) ||
        classroom.contains("ON-LINE", ignoreCase = true)

    val badgeBg = if (isOnline) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val badgeFg = if (isOnline) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = shortName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                period.teachersNameFull?.let { teacher ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = teacher,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = if (isOnline) "Онлайн" else classroom.ifEmpty { "Ауд." },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
