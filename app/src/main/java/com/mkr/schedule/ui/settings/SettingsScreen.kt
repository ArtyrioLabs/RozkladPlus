@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.mkr.schedule.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.mkr.schedule.BuildConfig
import com.mkr.schedule.data.local.PreferencesManager
import com.mkr.schedule.data.model.AppUpdate
import com.mkr.schedule.notification.ExactAlarmScheduler
import com.mkr.schedule.notification.LessonNotificationManager
import com.mkr.schedule.notification.LessonReminderWorker
import com.mkr.schedule.util.LogCollector
import com.mkr.schedule.util.UpdateManager
import com.mkr.schedule.util.localization.LocalStrings
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val preferences = remember(context) { PreferencesManager(context.applicationContext) }
    val dynamicColors by preferences.dynamicColors.collectAsState(initial = true)
    val themeMode by preferences.themeMode.collectAsState(initial = "system")
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val isChillModeEnabled by viewModel.isChillModeEnabled.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLogsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var updateInProgress by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdate?>(null) }
    var showDonateConfirm by remember { mutableStateOf(false) }

    var watchNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var watchNotificationsEnabled by remember {
        mutableStateOf(com.mkr.schedule.wear.PhoneWearSyncManager.isWatchNotificationsEnabled(context))
    }
    var watchShowBreaks by remember {
        mutableStateOf(com.mkr.schedule.wear.PhoneWearSyncManager.isWatchShowBreaksEnabled(context))
    }

    LaunchedEffect(Unit) {
        watchNames = com.mkr.schedule.wear.PhoneWearSyncManager.getConnectedWatchNames(context)
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    fun startReminderServiceSafely() {
        try {
            LessonReminderWorker.schedule(context)
            ExactAlarmScheduler.scheduleAllAlarms(context)
            LogCollector.log("SettingsScreen", "Reminder worker and alarms scheduling requested")
        } catch (e: Exception) {
            LogCollector.log("SettingsScreen", "Failed to schedule alarms: ${e.message}")
            scope.launch { snackbarHostState.showSnackbar("Не вдалося запустити нагадування") }
        }
    }

    fun stopReminderServiceSafely() {
        try {
            LessonReminderWorker.cancel(context)
            ExactAlarmScheduler.cancelAllAlarms(context)
            LogCollector.log("SettingsScreen", "Reminder worker and alarms cancellation requested")
        } catch (e: Exception) {
            LogCollector.log("SettingsScreen", "Failed to cancel alarms: ${e.message}")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            viewModel.toggleNotifications(true)
            startReminderServiceSafely()
            scope.launch { snackbarHostState.showSnackbar("Нагадування увімкнено") }
        }
    }

    LaunchedEffect(notificationsEnabled, hasNotificationPermission) {
        if (notificationsEnabled && hasNotificationPermission) {
            startReminderServiceSafely()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = strings.settings,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBackClick,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Appearance & Monet Dynamic Theme
            SettingsSection(title = "Персоналізація та Material You") {
                val hasMonet = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                SettingsGroupItem(
                    position = if (hasMonet) GroupPosition.TOP else GroupPosition.SINGLE
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SettingIconBox(
                                icon = if (themeMode == "dark") Icons.Default.DarkMode else Icons.Default.LightMode,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Тема оформлення",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Автоматична, світла або темна палітра",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val modes = listOf(
                            "system" to "Системна",
                            "light" to "Світла",
                            "dark" to "Темна"
                        )
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            modes.forEachIndexed { index, (mode, label) ->
                                SegmentedButton(
                                    selected = themeMode == mode,
                                    onClick = { scope.launch { preferences.setThemeMode(mode) } },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = modes.size
                                    ),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (hasMonet) {
                    SettingsGroupItem(position = GroupPosition.MIDDLE) {
                        SettingsSwitchRow(
                            icon = Icons.Default.Palette,
                            title = "Динамічні кольори Monet",
                            subtitle = "Адаптувати кольори інтерфейсу під шпалери",
                            checked = dynamicColors,
                            onCheckedChange = { enabled ->
                                scope.launch { preferences.setDynamicColors(enabled) }
                            }
                        )
                    }
                }

                SettingsGroupItem(position = GroupPosition.MIDDLE) {
                    SettingsSwitchRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Гейміфікація прогресу (XP)",
                        subtitle = "Показувати рівень та досвід за пройдені пари в меню",
                        checked = isChillModeEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleChillMode(enabled)
                        }
                    )
                }

                SettingsGroupItem(position = GroupPosition.BOTTOM) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SettingIconBox(
                                icon = Icons.Default.Language,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.language,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Мова інтерфейсу застосунку",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        val langs = listOf("uk" to "Українська", "en" to "English")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            langs.forEachIndexed { index, (code, label) ->
                                SegmentedButton(
                                    selected = currentLanguage == code,
                                    onClick = { viewModel.setLanguage(code) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = langs.size),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(text = label, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Notifications & Reminders
            SettingsSection(title = "Сповіщення та розклад") {
                SettingsGroupItem(position = GroupPosition.TOP) {
                    SettingsSwitchRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Нагадування про пари",
                        subtitle = "За 15 та 5 хвилин до початку, а також таймер поточної пари",
                        checked = notificationsEnabled && hasNotificationPermission,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.toggleNotifications(enabled)
                                if (enabled) {
                                    startReminderServiceSafely()
                                } else {
                                    stopReminderServiceSafely()
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (enabled) "Нагадування увімкнено" else "Нагадування вимкнено"
                                    )
                                }
                            }
                        }
                    )
                }

                SettingsGroupItem(position = GroupPosition.MIDDLE) {
                    SettingsActionRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Перевірити сповіщення",
                        subtitle = "Надіслати тестове нагадування прямо зараз",
                        onClick = {
                            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                LessonNotificationManager(context).showTestNotification()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Тестове сповіщення надіслано")
                                }
                            }
                        }
                    )
                }

                SettingsGroupItem(position = GroupPosition.BOTTOM) {
                    SettingsActionRow(
                        icon = Icons.Default.DeleteSweep,
                        title = "Очистити кеш розкладу",
                        subtitle = "Видалити збережені офлайн-дані для повторного завантаження",
                        onClick = {
                            viewModel.clearScheduleCache()
                            scope.launch {
                                snackbarHostState.showSnackbar("Локальний кеш розкладу очищено")
                            }
                        }
                    )
                }
            }

            // Section 2.5: Wear OS Companion (Pixel Watch)
            SettingsSection(title = "Годинник Wear OS (Pixel Watch)") {
                SettingsGroupItem(position = GroupPosition.TOP) {
                    SettingsActionRow(
                        icon = Icons.Default.Sync,
                        title = "Синхронізувати з годинником",
                        subtitle = if (watchNames.isNotEmpty()) {
                            "Підключено: ${watchNames.joinToString(", ")}"
                        } else {
                            "Надіслати розклад та налаштування на годинник"
                        },
                        onClick = {
                            com.mkr.schedule.wear.PhoneWearSyncManager.pushSavedStateToWatch(context)
                            scope.launch {
                                watchNames = com.mkr.schedule.wear.PhoneWearSyncManager.getConnectedWatchNames(context)
                                snackbarHostState.showSnackbar(
                                    if (watchNames.isNotEmpty()) {
                                        "Розклад синхронізовано з ${watchNames.first()}"
                                    } else {
                                        "Дані підготовлено для синхронізації з годинником"
                                    }
                                )
                            }
                        }
                    )
                }

                SettingsGroupItem(position = GroupPosition.MIDDLE) {
                    SettingsSwitchRow(
                        icon = Icons.Default.Watch,
                        title = "Сповіщення на годиннику",
                        subtitle = "Нагадування за 5 хв до пари та живий таймер на Pixel Watch",
                        checked = watchNotificationsEnabled,
                        onCheckedChange = { enabled ->
                            watchNotificationsEnabled = enabled
                            com.mkr.schedule.wear.PhoneWearSyncManager.setWatchNotificationsEnabled(context, enabled)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (enabled) "Сповіщення на годиннику увімкнено" else "Сповіщення на годиннику вимкнено"
                                )
                            }
                        }
                    )
                }

                SettingsGroupItem(position = GroupPosition.MIDDLE) {
                    SettingsSwitchRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Перерви та вікна на годиннику",
                        subtitle = "Показувати картки великої перерви та вікон між парами",
                        checked = watchShowBreaks,
                        onCheckedChange = { enabled ->
                            watchShowBreaks = enabled
                            com.mkr.schedule.wear.PhoneWearSyncManager.setWatchShowBreaksEnabled(context, enabled)
                        }
                    )
                }

                SettingsGroupItem(position = GroupPosition.BOTTOM) {
                    SettingsActionRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Тестове сповіщення на годинник",
                        subtitle = "Перевірити вібрацію та картку пари на годиннику",
                        onClick = {
                            scope.launch {
                                val ok = com.mkr.schedule.wear.PhoneWearSyncManager.sendTestNotificationToWatch(context)
                                snackbarHostState.showSnackbar(
                                    if (ok) "Тестове сповіщення надіслано на годинник"
                                    else "Годинник не знайдено поруч по Bluetooth"
                                )
                            }
                        }
                    )
                }
            }

            // Section 3: System & Updates
            SettingsSection(title = "Система та підтримка") {
                SettingsGroupItem(position = GroupPosition.TOP) {
                    SettingsActionRow(
                        icon = Icons.Default.SystemUpdate,
                        title = if (updateInProgress) "Перевірка оновлень..." else "Перевірити оновлення",
                        subtitle = "Поточна версія ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        enabled = !updateInProgress,
                        onClick = {
                            scope.launch {
                                updateInProgress = true
                                val update = UpdateManager.checkForUpdate()
                                updateInProgress = false
                                if (update != null) {
                                    updateInfo = update
                                } else {
                                    snackbarHostState.showSnackbar("У вас встановлена актуальна версія")
                                }
                            }
                        }
                    )
                }

                SettingsGroupItem(position = GroupPosition.MIDDLE) {
                    SettingsActionRow(
                        icon = Icons.Default.BugReport,
                        title = strings.logs,
                        subtitle = "Журнал діагностики та мережевих запитів",
                        onClick = { showLogsDialog = true }
                    )
                }

                SettingsGroupItem(position = GroupPosition.MIDDLE) {
                    SettingsActionRow(
                        icon = Icons.Default.StarRate,
                        title = "Оцінити застосунок",
                        subtitle = "Ваша оцінка допомагає розвитку проєкту",
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=${context.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    )
                }

                SettingsGroupItem(position = GroupPosition.MIDDLE) {
                    SettingsActionRow(
                        icon = Icons.Default.Favorite,
                        title = "Підтримати розробника",
                        subtitle = "Подякувати за безкоштовний застосунок без реклами",
                        onClick = { showDonateConfirm = true }
                    )
                }

                SettingsGroupItem(position = GroupPosition.BOTTOM) {
                    SettingsActionRow(
                        icon = Icons.Default.Info,
                        title = strings.about,
                        subtitle = "Інформація про Розклад+",
                        onClick = { showAboutDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showLogsDialog) {
            LogsDialog(
                onDismiss = { showLogsDialog = false },
                onShared = {
                    scope.launch { snackbarHostState.showSnackbar("Логи підготовлено") }
                }
            )
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = {
                    Text(
                        text = strings.about,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoRow("Застосунок", "Розклад+")
                        InfoRow(strings.version, "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        InfoRow("Дизайн", "Material 3 Expressive • Monet")
                        InfoRow(strings.developer, "ArtyrioLabs")
                        InfoRow("Безпека", "AES256-GCM • Перевірка підпису")
                    }
                },
                confirmButton = {
                    FilledTonalButton(onClick = { showAboutDialog = false }) {
                        Text("Зрозуміло")
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }

        updateInfo?.let { update ->
            AlertDialog(
                onDismissRequest = { updateInfo = null },
                title = {
                    Text(
                        text = strings.updateAvailable,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("Версія ${update.versionName}\n\n${update.changelog}")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            UpdateManager.downloadAndInstall(context, update.downloadUrl)
                            updateInfo = null
                        }
                    ) { Text(strings.update) }
                },
                dismissButton = {
                    TextButton(onClick = { updateInfo = null }) { Text(strings.cancel) }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }

        if (showDonateConfirm) {
            AlertDialog(
                onDismissRequest = { showDonateConfirm = false },
                title = {
                    Text(
                        text = "Дякую за підтримку Розклад+",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Цей застосунок розробляється у вільний час і є повністю безкоштовним.\n\n" +
                            "Якщо розклад допомагає тобі не запізнюватись — можна підтримати автора символічним донатом."
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showDonateConfirm = false
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.DONATION_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) {
                        Text("Підтримати")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDonateConfirm = false }) { Text("Наступного разу") }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }
    }
}

private enum class GroupPosition {
    SINGLE,
    TOP,
    MIDDLE,
    BOTTOM
}

@Composable
private fun groupItemShape(position: GroupPosition): Shape {
    val outer = 24.dp
    val inner = 6.dp
    return when (position) {
        GroupPosition.SINGLE -> RoundedCornerShape(outer)
        GroupPosition.TOP -> RoundedCornerShape(
            topStart = outer,
            topEnd = outer,
            bottomStart = inner,
            bottomEnd = inner
        )
        GroupPosition.MIDDLE -> RoundedCornerShape(inner)
        GroupPosition.BOTTOM -> RoundedCornerShape(
            topStart = inner,
            topEnd = inner,
            bottomStart = outer,
            bottomEnd = outer
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
        )
        content()
    }
}

@Composable
private fun SettingsGroupItem(
    position: GroupPosition,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = groupItemShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        content()
    }
}

@Composable
private fun SettingIconBox(
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = contentColor
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val iconBg by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "switch_icon_bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "switch_icon_tint"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingIconBox(
            icon = icon,
            containerColor = iconBg,
            contentColor = iconTint
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else null
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingIconBox(
            icon = icon,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LogsDialog(
    onDismiss: () -> Unit,
    onShared: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Журнал роботи",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            val logs: List<String> = LogCollector.getLogs()
            if (logs.isEmpty()) {
                Text(
                    text = "Журнал наразі порожній",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseLayout = true
                ) {
                    items(logs.reversed()) { log: String ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    val logs = LogCollector.getLogs()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                    val fileName = "mkr_2_0_${dateFormat.format(Date())}.log"
                    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                    file.writeText(logs.joinToString("\n"))
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_STREAM,
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Поділитися логами"))
                    onShared()
                }
            ) {
                Text("Поділитися")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрити") }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
