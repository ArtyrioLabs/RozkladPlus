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

    val today: String
    val tomorrow: String
    val yesterday: String
    val lecture: String
    val practice: String
    val laboratory: String
    val lessonDefault: String
    val pairLabel: String
    val longBreak: String
    val windowBreak: String
    val disciplineFallback: String
    val conferenceLinkPlaceholder: String
    val untilTimePrefix: String
    val minutesShort: String
    val onlineBadge: String
    val nonstandardTimeBadge: String

    // Navigation Drawer
    val quickActions: String
    val viewWeekGrid: String
    val viewDayList: String
    val changeGroupOrUniversity: String
    val groupPrefix: String
    val noProfileSelected: String
    val bellScheduleUnavailable: String
    fun formatLevel(level: Int, xp: Int): String
    fun formatOrdinalClass(number: Int): String

    // Settings
    val personalizationSection: String
    val themeTitle: String
    val themeSubtitle: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val monetTitle: String
    val monetSubtitle: String
    val gamificationTitle: String
    val gamificationSubtitle: String
    val languageSubtitle: String

    val notificationsSection: String
    val remindersTitle: String
    val remindersSubtitle: String
    val remindersEnabledSnackbar: String
    val remindersDisabledSnackbar: String
    val remindersStartFailed: String
    val testNotificationTitle: String
    val testNotificationSubtitle: String
    val testNotificationSentSnackbar: String
    val clearCacheTitle: String
    val clearCacheSubtitle: String
    val clearCacheSnackbar: String

    val wearOsSection: String
    val wearSyncTitle: String
    val wearSyncSubtitleDefault: String
    fun wearSyncSubtitleConnected(names: String): String
    fun wearSyncSnackbarConnected(name: String): String
    val wearSyncSnackbarDefault: String
    val wearNotificationsTitle: String
    val wearNotificationsSubtitle: String
    val wearNotificationsEnabledSnackbar: String
    val wearNotificationsDisabledSnackbar: String
    val wearBreaksTitle: String
    val wearBreaksSubtitle: String
    val wearTestNotificationTitle: String
    val wearTestNotificationSubtitle: String
    val wearTestNotificationSuccess: String
    val wearTestNotificationNotFound: String

    val systemSection: String
    val checkUpdatesTitle: String
    val checkingUpdatesTitle: String
    fun currentVersionSubtitle(versionName: String, versionCode: Int): String
    val latestVersionInstalledSnackbar: String
    val diagnosticLogsSubtitle: String
    val rateAppTitle: String
    val rateAppSubtitle: String
    val donateTitle: String
    val donateSubtitle: String
    val aboutAppSubtitle: String

    // Dialogs & Extras
    val appNameLabel: String
    val designLabel: String
    val securityLabel: String
    val securityDetails: String
    val gotIt: String
    val donateDialogTitle: String
    val donateDialogText: String
    val donateConfirmButton: String
    val donateDismissButton: String
    val logsTitle: String
    val logsEmpty: String
    val share: String
    val shareLogs: String
    val logsReadySnackbar: String

    fun formatPairsCount(count: Int): String
    fun formatWindowWithPairs(count: Int): String
    fun translateDiscipline(name: String?): String
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
    override val onlineBadge = "Онлайн"
    override val nonstandardTimeBadge = "Зміщений час"

    override val today = "Сьогодні"
    override val tomorrow = "Завтра"
    override val yesterday = "Вчора"
    override val lecture = "Лекція"
    override val practice = "Практика"
    override val laboratory = "Лабораторна"
    override val lessonDefault = "Заняття"
    override val pairLabel = "пара"
    override val longBreak = "Велика перерва"
    override val windowBreak = "Вікно"
    override val disciplineFallback = "Дисципліна"
    override val conferenceLinkPlaceholder = "Посилання на онлайн-пару (Zoom / Meet)"
    override val untilTimePrefix = "до"
    override val minutesShort = "хв"

    // Drawer
    override val quickActions = "Швидкі дії"
    override val viewWeekGrid = "Вигляд: Таблиця тижня"
    override val viewDayList = "Вигляд: Список днів"
    override val changeGroupOrUniversity = "Змінити групу або ЗВО"
    override val groupPrefix = "Група"
    override val noProfileSelected = "Профіль не вибрано"
    override val bellScheduleUnavailable = "Розклад дзвінків для обраного ЗВО недоступний"
    override fun formatLevel(level: Int, xp: Int): String = "Рівень $level • $xp XP"
    override fun formatOrdinalClass(number: Int): String = "$number-та пара"

    // Settings
    override val personalizationSection = "Персоналізація та Material You"
    override val themeTitle = "Тема оформлення"
    override val themeSubtitle = "Автоматична, світла або темна палітра"
    override val themeSystem = "Системна"
    override val themeLight = "Світла"
    override val themeDark = "Темна"
    override val monetTitle = "Динамічні кольори Monet"
    override val monetSubtitle = "Адаптувати кольори інтерфейсу під шпалери"
    override val gamificationTitle = "Гейміфікація прогресу (XP)"
    override val gamificationSubtitle = "Показувати рівень та досвід за пройдені пари в меню"
    override val languageSubtitle = "Мова інтерфейсу застосунку"

    override val notificationsSection = "Сповіщення та розклад"
    override val remindersTitle = "Нагадування про пари"
    override val remindersSubtitle = "За 15 та 5 хвилин до початку, а також таймер поточної пари"
    override val remindersEnabledSnackbar = "Нагадування увімкнено"
    override val remindersDisabledSnackbar = "Нагадування вимкнено"
    override val remindersStartFailed = "Не вдалося запустити нагадування"
    override val testNotificationTitle = "Перевірити сповіщення"
    override val testNotificationSubtitle = "Надіслати тестове нагадування прямо зараз"
    override val testNotificationSentSnackbar = "Тестове сповіщення надіслано"
    override val clearCacheTitle = "Очистити кеш розкладу"
    override val clearCacheSubtitle = "Видалити збережені офлайн-дані для повторного завантаження"
    override val clearCacheSnackbar = "Локальний кеш розкладу очищено"

    override val wearOsSection = "Годинник Wear OS (Pixel Watch)"
    override val wearSyncTitle = "Синхронізувати з годинником"
    override val wearSyncSubtitleDefault = "Надіслати розклад та налаштування на годинник"
    override fun wearSyncSubtitleConnected(names: String): String = "Підключено: $names"
    override fun wearSyncSnackbarConnected(name: String): String = "Розклад синхронізовано з $name"
    override val wearSyncSnackbarDefault = "Дані підготовлено для синхронізації з годинником"
    override val wearNotificationsTitle = "Сповіщення на годиннику"
    override val wearNotificationsSubtitle = "Нагадування за 5 хв до пари та живий таймер на Pixel Watch"
    override val wearNotificationsEnabledSnackbar = "Сповіщення на годиннику увімкнено"
    override val wearNotificationsDisabledSnackbar = "Сповіщення на годиннику вимкнено"
    override val wearBreaksTitle = "Перерви та вікна на годиннику"
    override val wearBreaksSubtitle = "Показувати картки великої перерви та вікон між парами"
    override val wearTestNotificationTitle = "Тестове сповіщення на годинник"
    override val wearTestNotificationSubtitle = "Перевірити вібрацію та картку пари на годиннику"
    override val wearTestNotificationSuccess = "Тестове сповіщення надіслано на годинник"
    override val wearTestNotificationNotFound = "Годинник не знайдено поруч по Bluetooth"

    override val systemSection = "Система та підтримка"
    override val checkUpdatesTitle = "Перевірити оновлення"
    override val checkingUpdatesTitle = "Перевірка оновлень..."
    override fun currentVersionSubtitle(versionName: String, versionCode: Int): String =
        "Поточна версія $versionName ($versionCode)"
    override val latestVersionInstalledSnackbar = "У вас встановлена актуальна версія"
    override val diagnosticLogsSubtitle = "Журнал діагностики та мережевих запитів"
    override val rateAppTitle = "Оцінити застосунок"
    override val rateAppSubtitle = "Ваша оцінка допомагає розвитку проєкту"
    override val donateTitle = "Підтримати розробника"
    override val donateSubtitle = "Подякувати за безкоштовний застосунок без реклами"
    override val aboutAppSubtitle = "Інформація про Розклад+"

    // Dialogs & Extras
    override val appNameLabel = "Застосунок"
    override val designLabel = "Дизайн"
    override val securityLabel = "Безпека"
    override val securityDetails = "AES256-GCM • Перевірка підпису"
    override val gotIt = "Зрозуміло"
    override val donateDialogTitle = "Дякую за підтримку Розклад+"
    override val donateDialogText = "Цей застосунок розробляється у вільний час і є повністю безкоштовним.\n\nЯкщо розклад допомагає тобі не запізнюватись — можна підтримати автора символічним донатом."
    override val donateConfirmButton = "Підтримати"
    override val donateDismissButton = "Наступного разу"
    override val logsTitle = "Журнал роботи"
    override val logsEmpty = "Журнал наразі порожній"
    override val share = "Поділитися"
    override val shareLogs = "Поділитися логами"
    override val logsReadySnackbar = "Логи підготовлено"

    override fun formatPairsCount(count: Int): String {
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

    override fun formatWindowWithPairs(count: Int): String {
        return when {
            count == 1 -> "Вікно (1 пара)"
            count in 2..4 -> "Вікно ($count пари)"
            count >= 5 -> "Вікно ($count пар)"
            else -> "Вікно між парами"
        }
    }

    override fun translateDiscipline(name: String?): String =
        DisciplineTranslator.translateDiscipline(name, Language.UKRAINIAN)
}

object EnglishStrings : Strings {
    override val appTitle = "Rozklad+"
    override val back = "Back"
    override val settings = "Settings"
    override val loading = "Loading..."
    override val close = "Close"

    override val selectStructure = "Select structure"
    override val selectUniversity = "Select university"
    override val selectFaculty = "Select faculty"
    override val selectCourse = "Select year"
    override val selectGroup = "Select group"
    override val selectStudent = "Select student"

    override val schedule = "Class Schedule"
    override val callSchedule = "Bell Schedule"
    override val noClasses = "No classes"
    override val lessons = "classes"
    override val course = "year"

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
    override val onlineBadge = "Online"
    override val nonstandardTimeBadge = "Shifted time"

    override val today = "Today"
    override val tomorrow = "Tomorrow"
    override val yesterday = "Yesterday"
    override val lecture = "Lecture"
    override val practice = "Practice"
    override val laboratory = "Lab"
    override val lessonDefault = "Class"
    override val pairLabel = "class"
    override val longBreak = "Long break"
    override val windowBreak = "Free period"
    override val disciplineFallback = "Course"
    override val conferenceLinkPlaceholder = "Online class link (Zoom / Meet)"
    override val untilTimePrefix = "until"
    override val minutesShort = "min"

    // Drawer
    override val quickActions = "Quick Actions"
    override val viewWeekGrid = "View: Week Table"
    override val viewDayList = "View: Day List"
    override val changeGroupOrUniversity = "Change Group or University"
    override val groupPrefix = "Group"
    override val noProfileSelected = "No profile selected"
    override val bellScheduleUnavailable = "Bell schedule for this institution is unavailable"
    override fun formatLevel(level: Int, xp: Int): String = "Level $level • $xp XP"
    override fun formatOrdinalClass(number: Int): String {
        return when (number) {
            1 -> "1st class"
            2 -> "2nd class"
            3 -> "3rd class"
            else -> "${number}th class"
        }
    }

    // Settings
    override val personalizationSection = "Personalization & Material You"
    override val themeTitle = "Theme"
    override val themeSubtitle = "Automatic, light or dark color palette"
    override val themeSystem = "System"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val monetTitle = "Dynamic Monet Colors"
    override val monetSubtitle = "Adapt UI colors to system wallpaper"
    override val gamificationTitle = "Gamification & Progress (XP)"
    override val gamificationSubtitle = "Show level and XP for completed classes in menu"
    override val languageSubtitle = "Application interface language"

    override val notificationsSection = "Notifications & Schedule"
    override val remindersTitle = "Class Reminders"
    override val remindersSubtitle = "15 and 5 minutes before class, ongoing timer"
    override val remindersEnabledSnackbar = "Reminders enabled"
    override val remindersDisabledSnackbar = "Reminders disabled"
    override val remindersStartFailed = "Failed to start reminders"
    override val testNotificationTitle = "Test Notifications"
    override val testNotificationSubtitle = "Send a test reminder notification right now"
    override val testNotificationSentSnackbar = "Test notification sent"
    override val clearCacheTitle = "Clear Schedule Cache"
    override val clearCacheSubtitle = "Remove saved offline schedule data to reload"
    override val clearCacheSnackbar = "Local schedule cache cleared"

    override val wearOsSection = "Wear OS Companion (Pixel Watch)"
    override val wearSyncTitle = "Sync with Watch"
    override val wearSyncSubtitleDefault = "Send schedule and settings to smartwatch"
    override fun wearSyncSubtitleConnected(names: String): String = "Connected: $names"
    override fun wearSyncSnackbarConnected(name: String): String = "Schedule synced with $name"
    override val wearSyncSnackbarDefault = "Data queued for sync with watch"
    override val wearNotificationsTitle = "Watch Notifications"
    override val wearNotificationsSubtitle = "5-minute reminders and ongoing countdown on Pixel Watch"
    override val wearNotificationsEnabledSnackbar = "Watch notifications enabled"
    override val wearNotificationsDisabledSnackbar = "Watch notifications disabled"
    override val wearBreaksTitle = "Breaks & Free Periods on Watch"
    override val wearBreaksSubtitle = "Display break cards and free periods between classes"
    override val wearTestNotificationTitle = "Test Watch Notification"
    override val wearTestNotificationSubtitle = "Test vibration and class card on smartwatch"
    override val wearTestNotificationSuccess = "Test notification sent to watch"
    override val wearTestNotificationNotFound = "Watch not found nearby via Bluetooth"

    override val systemSection = "System & Support"
    override val checkUpdatesTitle = "Check for Updates"
    override val checkingUpdatesTitle = "Checking for updates..."
    override fun currentVersionSubtitle(versionName: String, versionCode: Int): String =
        "Current version $versionName ($versionCode)"
    override val latestVersionInstalledSnackbar = "You have the latest version installed"
    override val diagnosticLogsSubtitle = "Diagnostic and network activity logs"
    override val rateAppTitle = "Rate App"
    override val rateAppSubtitle = "Your rating supports further development"
    override val donateTitle = "Support Developer"
    override val donateSubtitle = "Say thanks for an ad-free, open-source app"
    override val aboutAppSubtitle = "About Rozklad+"

    // Dialogs & Extras
    override val appNameLabel = "Application"
    override val designLabel = "Design"
    override val securityLabel = "Security"
    override val securityDetails = "AES256-GCM • Signature Verification"
    override val gotIt = "Got it"
    override val donateDialogTitle = "Thank you for supporting Rozklad+"
    override val donateDialogText = "This application is developed in spare time and is completely free and open-source.\n\nIf the schedule helps you stay on track — you can support the author with a small coffee donation."
    override val donateConfirmButton = "Support"
    override val donateDismissButton = "Maybe later"
    override val logsTitle = "Diagnostic Logs"
    override val logsEmpty = "Log history is currently empty"
    override val share = "Share"
    override val shareLogs = "Share diagnostic logs"
    override val logsReadySnackbar = "Logs prepared"

    override fun formatPairsCount(count: Int): String {
        return "$count ${if (count == 1) "class" else "classes"}"
    }

    override fun formatWindowWithPairs(count: Int): String {
        return when {
            count == 1 -> "Free period (1 class)"
            count > 1 -> "Free period ($count classes)"
            else -> "Break between classes"
        }
    }

    override fun translateDiscipline(name: String?): String =
        DisciplineTranslator.translateDiscipline(name, Language.ENGLISH)
}
