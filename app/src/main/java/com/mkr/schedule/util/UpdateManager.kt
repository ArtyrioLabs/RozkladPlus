package com.mkr.schedule.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.mkr.schedule.BuildConfig
import com.mkr.schedule.R
import com.mkr.schedule.data.model.AppUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    private const val OTA_CHANNEL_ID = "rozklad_ota_channel"
    private const val OTA_NOTIFICATION_ID = 9401
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val CANDIDATE_URLS = listOfNotNull(
        BuildConfig.UPDATE_CHECK_URL.takeIf { it.isNotBlank() },
        "https://ArtyrioLabs.github.io/mkr2.0/mkr-update.json"
    ).distinct()

    suspend fun checkForUpdate(): AppUpdate? = withContext(Dispatchers.IO) {
        val gson = com.google.gson.Gson()
        for (url in CANDIDATE_URLS) {
            if (url.isBlank() || url.contains("example.com")) continue
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    requestMethod = "GET"
                }
                if (connection.responseCode in 200..299) {
                    val json = connection.inputStream.bufferedReader().use { it.readText() }
                    val update = gson.fromJson(json, AppUpdate::class.java)
                    if (update != null && update.versionCode > BuildConfig.VERSION_CODE) {
                        return@withContext update
                    }
                    return@withContext null
                }
            } catch (e: Exception) {
                LogCollector.log("UpdateManager", "Candidate OTA check ($url) failed: ${e.message}")
            }
        }
        null
    }

    fun downloadAndInstall(context: Context, downloadUrl: String) {
        val appContext = context.applicationContext
        val downloadUri = Uri.parse(downloadUrl)
        val scheme = downloadUri.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") {
            LogCollector.log("UpdateManager", "Invalid update URL scheme: $downloadUrl")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !appContext.packageManager.canRequestPackageInstalls()
        ) {
            Toast.makeText(
                appContext,
                "Дозвольте встановлення оновлень для Розклад+",
                Toast.LENGTH_LONG
            ).show()
            try {
                val permIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${appContext.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(permIntent)
            } catch (e: Exception) {
                LogCollector.log("UpdateManager", "Failed to open unknown sources settings: ${e.message}")
            }
        }

        Toast.makeText(appContext, "Завантаження оновлення Розклад+...", Toast.LENGTH_SHORT).show()
        ensureChannel(appContext)
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        scope.launch {
            try {
                val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: appContext.filesDir
                if (!dir.exists()) dir.mkdirs()
                val targetFile = File(dir, "rozklad-plus-update.apk")
                if (targetFile.exists()) targetFile.delete()

                val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 30_000
                    requestMethod = "GET"
                }

                val totalBytes = conn.contentLength.coerceAtLeast(1)
                var downloadedBytes = 0
                var lastNotifPercent = -1

                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            val percent = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            if (percent - lastNotifPercent >= 10) {
                                lastNotifPercent = percent
                                val progressNotif = NotificationCompat.Builder(appContext, OTA_CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_stat_mkr)
                                    .setContentTitle("Завантаження Розклад+")
                                    .setContentText("$percent%")
                                    .setOnlyAlertOnce(true)
                                    .setOngoing(true)
                                    .build()
                                nm.notify(OTA_NOTIFICATION_ID, progressNotif)
                            }
                        }
                        output.flush()
                    }
                }

                val apkUri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.provider",
                    targetFile
                )

                val installIntent = createInstallIntent(apkUri)
                val pendingInstall = PendingIntent.getActivity(
                    appContext,
                    9402,
                    installIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val readyNotif = NotificationCompat.Builder(appContext, OTA_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_mkr)
                    .setContentTitle("Оновлення Розклад+ завантажено")
                    .setContentText("Натисніть, щоб встановити нову версію")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingInstall)
                    .build()
                nm.notify(OTA_NOTIFICATION_ID, readyNotif)

                withContext(Dispatchers.Main) {
                    try {
                        appContext.startActivity(installIntent)
                    } catch (e: Exception) {
                        LogCollector.log("UpdateManager", "Direct startActivity failed: ${e.message}")
                        Toast.makeText(
                            appContext,
                            "Натисніть на сповіщення у шторці для встановлення",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                LogCollector.log("UpdateManager", "OTA download failed: ${e.message}")
                nm.cancel(OTA_NOTIFICATION_ID)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        "Помилка завантаження оновлення: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun createInstallIntent(apkUri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            OTA_CHANNEL_ID,
            "Оновлення додатку",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Завантаження та встановлення оновлень Розклад+"
        }
        nm.createNotificationChannel(channel)
    }
}
