package com.mkr.schedule

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mkr.schedule.data.local.PreferencesManager
import com.mkr.schedule.ui.screens.PublicScheduleScreen
import com.mkr.schedule.ui.screens.ScheduleScreen
import com.mkr.schedule.ui.theme.MKRScheduleTheme
import com.mkr.schedule.ui.viewmodel.AuthViewModel
import com.mkr.schedule.ui.viewmodel.PublicScheduleViewModel
import com.mkr.schedule.ui.viewmodel.ScheduleViewModel
import com.mkr.schedule.util.LogCollector
import com.mkr.schedule.util.localization.Language
import com.mkr.schedule.util.localization.ProvideStrings
import com.mkr.schedule.util.security.AppIntegrity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var pendingUpdate by mutableStateOf<com.mkr.schedule.data.model.AppUpdate?>(null)
    private val preferencesManager by lazy { PreferencesManager(applicationContext) }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        lifecycleScope.launch {
            if (isGranted) {
                try {
                    com.mkr.schedule.notification.LessonReminderWorker.schedule(this@MainActivity)
                    com.mkr.schedule.notification.ExactAlarmScheduler.scheduleAllAlarms(this@MainActivity)
                    LogCollector.log("MainActivity", "Notification permission granted; reminders scheduled")
                } catch (e: Exception) {
                    LogCollector.log("MainActivity", "Failed to schedule reminders after permission grant: ${e.message}")
                }
            } else {
                LogCollector.log("MainActivity", "Notification permission denied")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AppIntegrity.isSignatureTrusted(this)) {
            LogCollector.log("Security", "App signature verification failed. Blocking launch.")
            setContent {
                MKRScheduleTheme {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {},
                        title = { androidx.compose.material3.Text("Попередження безпеки") },
                        text = { androidx.compose.material3.Text("Підпис застосунку не збігається з довіреним.") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { finishAffinity() }) {
                                androidx.compose.material3.Text("Закрити")
                            }
                        }
                    )
                }
            }
            return
        }

        com.mkr.schedule.util.AppAnalytics.logAppOpen()

        // Schedule background notification reminders if enabled
        lifecycleScope.launch {
            if (preferencesManager.getNotificationsEnabled()) {
                try {
                    val hasNotificationPermission =
                        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                    if (!hasNotificationPermission &&
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        LogCollector.log("MainActivity", "Requested POST_NOTIFICATIONS permission")
                    }

                    com.mkr.schedule.notification.LessonReminderWorker.schedule(this@MainActivity)
                    com.mkr.schedule.notification.ExactAlarmScheduler.scheduleAllAlarms(this@MainActivity)
                    LogCollector.log("MainActivity", "Reminder worker and alarms scheduled on app start")
                } catch (e: Exception) {
                    LogCollector.log("MainActivity", "Failed to schedule reminders: ${e.message}")
                }
            } else {
                com.mkr.schedule.notification.LessonReminderWorker.cancel(this@MainActivity)
            }
        }

        // Periodic background worker for schedule changes
        com.mkr.schedule.notification.ScheduleChangeWorker.schedule(this)

        // Check for updates on startup
        checkForUpdates()

        setContent {
            MKRScheduleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MkrApp()
                    pendingUpdate?.let { update ->
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { if (!update.forceUpdate) pendingUpdate = null },
                            title = { androidx.compose.material3.Text("Доступне оновлення") },
                            text = { androidx.compose.material3.Text("Версія ${update.versionName}\n\n${update.changelog}") },
                            confirmButton = {
                                androidx.compose.material3.TextButton(onClick = {
                                    com.mkr.schedule.util.UpdateManager.downloadAndInstall(this@MainActivity, update.downloadUrl)
                                    pendingUpdate = null
                                }) { androidx.compose.material3.Text("Оновити") }
                            },
                            dismissButton = {
                                if (!update.forceUpdate) {
                                    androidx.compose.material3.TextButton(onClick = { pendingUpdate = null }) {
                                        androidx.compose.material3.Text("Пізніше")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkForUpdates() {
        lifecycleScope.launch {
            val update = com.mkr.schedule.util.UpdateManager.checkForUpdate()
            if (update != null) {
                showUpdateDialog(update)
            }
        }
    }

    private fun showUpdateDialog(update: com.mkr.schedule.data.model.AppUpdate) {
        pendingUpdate = update
    }
}

@Composable
fun MkrApp() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context.applicationContext) }
    val languageCode by preferencesManager.language.collectAsState(initial = "uk")
    val selectedLanguage = if (languageCode == "en") Language.ENGLISH else Language.UKRAINIAN

    val publicScheduleViewModel: PublicScheduleViewModel = hiltViewModel()
    ProvideStrings(language = selectedLanguage) {
        PublicScheduleScreen(
            viewModel = publicScheduleViewModel
        )
    }
}

@Composable
fun ScheduleContent(
    token: String,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val scheduleViewModel: ScheduleViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    val studentId = authState.user?.currentId?.takeIf { authState.user?.type == 0 }
    val groupId = authState.user?.model?.let { null }

    ScheduleScreen(
        viewModel = scheduleViewModel,
        token = token,
        studentId = studentId,
        groupId = groupId,
        onLogout = onLogout
    )
}
