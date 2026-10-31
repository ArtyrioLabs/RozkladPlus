package com.mkr.schedule.util.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

enum class Language(val code: String, val displayName: String) {
    UKRAINIAN("uk", "Українська"),
    ENGLISH("en", "English")
}

val LocalStrings = compositionLocalOf<Strings> { UkrainianStrings }

@Composable
fun ProvideStrings(
    language: Language,
    content: @Composable () -> Unit
) {
    val strings = remember(language) {
        when (language) {
            Language.UKRAINIAN -> UkrainianStrings
            Language.ENGLISH -> EnglishStrings
        }
    }

    CompositionLocalProvider(LocalStrings provides strings) {
        content()
    }
}

interface Strings {
    val appTitle: String
    val back: String
    val settings: String
    val loading: String
    val close: String

    val selectStructure: String
    val selectUniversity: String
    val selectFaculty: String
    val selectCourse: String
    val selectGroup: String
    val selectStudent: String

    val schedule: String
    val callSchedule: String
    val noClasses: String
    val lessons: String
    val course: String

    val monday: String
    val tuesday: String
    val wednesday: String
    val thursday: String
    val friday: String
    val saturday: String
    val sunday: String

    val notifications: String
    val reminders: String
    val enable: String
    val minutesBeforeClass: String
    val allowNotifications: String
    val automaticReminders: String
    val lessonIn5Minutes: String

    val about: String
    val logs: String
    val version: String
    val developer: String
    val updates: String
    val viewDebugLogs: String
    val downloadLogs: String
    val clearLogs: String
    val language: String

    val error: String
    val noInternet: String
    val scheduleLoadError: String
    val studentNotFound: String

    val saveStudentTitle: String
    val saveStudentMessage: String
    val save: String
    val cancel: String
    val later: String

    val updateAvailable: String
    val update: String

    val online: String
}

object UkrainianStrings : Strings {
    override val appTitle = "Розклад+"
    override val back = "Назад"
    override val settings = "Налаштування"
    override val loading = "Завантажуємо..."
    override val close = "Закрити"

    override val selectStructure = "Виберіть структуру"
    override val selectUniversity = "Виберіть університет"
    override val selectFaculty = "Виберіть факультет"
    override val selectCourse = "Виберіть курс"
    override val selectGroup = "Виберіть групу"
    override val selectStudent = "Виберіть студента"

    override val schedule = "Розклад занять"
    override val callSchedule = "Розклад дзвінків"
    override val noClasses = "Немає занять"
    override val lessons = "заняття"
    override val course = "курс"

    override val monday = "Понеділок"
    override val tuesday = "Вівторок"
    override val wednesday = "Середа"
    override val thursday = "Четвер"
    override val friday = "П'ятниця"
    override val saturday = "Субота"
    override val sunday = "Неділя"

    override val notifications = "Сповіщення"
    override val reminders = "Нагадування"
    override val enable = "Увімкнути"
    override val minutesBeforeClass = "За 5 хвилин до пари"
    override val allowNotifications = "Дозвольте сповіщення в налаштуваннях"
    override val automaticReminders = "Автоматичні нагадування за 5 хвилин до кожної пари"
    override val lessonIn5Minutes = "Пара через 5 хвилин!"

    override val about = "Про застосунок"
    override val logs = "Логи"
    override val version = "Версія"
    override val developer = "Розробник"
    override val updates = "Оновлення"
    override val viewDebugLogs = "Переглянути логи"
    override val downloadLogs = "Завантажити логи"
    override val clearLogs = "Очистити логи"
    override val language = "Мова"

    override val error = "Помилка"
    override val noInternet = "Перевірте інтернет-з'єднання"
    override val scheduleLoadError = "Не вийшло завантажити розклад"
    override val studentNotFound = "Розклад для цього студента не знайдено"

    override val saveStudentTitle = "Зберегти вибір?"
    override val saveStudentMessage = "Зберегти студента, щоб швидше відкривати його розклад?"
    override val save = "Зберегти"
    override val cancel = "Скасувати"
    override val later = "Пізніше"

    override val updateAvailable = "Є нова версія"
    override val update = "Оновити"

    override val online = "ONLINE"
}

object EnglishStrings : Strings {
    override val appTitle = "Розклад+"
    override val back = "Back"
    override val settings = "Settings"
    override val loading = "Loading..."
    override val close = "Close"

    override val selectStructure = "Select structure"
    override val selectUniversity = "Select university"
    override val selectFaculty = "Select faculty"
    override val selectCourse = "Select course"
    override val selectGroup = "Select group"
    override val selectStudent = "Select student"

    override val schedule = "Class Schedule"
    override val callSchedule = "Bell Schedule"
    override val noClasses = "No classes"
    override val lessons = "lessons"
    override val course = "course"

    override val monday = "Monday"
    override val tuesday = "Tuesday"
    override val wednesday = "Wednesday"
    override val thursday = "Thursday"
    override val friday = "Friday"
    override val saturday = "Saturday"
    override val sunday = "Sunday"

    override val notifications = "Notifications"
    override val reminders = "Reminders"
    override val enable = "Enable"
    override val minutesBeforeClass = "5 minutes before class"
    override val allowNotifications = "Allow notifications in settings"
    override val automaticReminders = "Automatic reminders 5 minutes before each class"
    override val lessonIn5Minutes = "Class in 5 minutes!"

    override val about = "About"
    override val logs = "Logs"
    override val version = "Version"
    override val developer = "Developer"
    override val updates = "Updates"
    override val viewDebugLogs = "View debug logs"
    override val downloadLogs = "Download logs"
    override val clearLogs = "Clear logs"
    override val language = "Language"

    override val error = "Error"
    override val noInternet = "Check internet connection"
    override val scheduleLoadError = "Failed to load schedule"
    override val studentNotFound = "Schedule for this student not found"

    override val saveStudentTitle = "Save selection?"
    override val saveStudentMessage = "Save student for quick access?"
    override val save = "Save"
    override val cancel = "Cancel"
    override val later = "Later"

    override val updateAvailable = "Update available"
    override val update = "Update"

    override val online = "ONLINE"
}
