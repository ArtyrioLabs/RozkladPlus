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
import com.google.gson.annotations.SerializedName
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
    private const val GITHUB_RELEASES_API =
        "https://api.github.com/repos/ArtyrioLabs/RozkladPlus/releases/latest"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class GitHubRelease(
        @SerializedName("tag_name") val tagName: String? = null,
        val name: String? = null,
        val body: String? = null,
        val assets: List<GitHubAsset>? = null
    )

    private data class GitHubAsset(
        val name: String? = null,
        @SerializedName("browser_download_url") val browserDownloadUrl: String? = null,
        val size: Long? = null
    )

    private fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val rParts = remoteTag.trim().removePrefix("v").removePrefix("V").split('.')
            .mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
        val cParts = currentVersion.trim().removePrefix("v").removePrefix("V").split('.')
            .mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun extractVersionCode(body: String?): Int? {
        if (body.isNullOrBlank()) return null
        val match = Regex("""versionCode:\s*(\d+)""", RegexOption.IGNORE_CASE).find(body)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    suspend fun checkForUpdate(): AppUpdate? = withContext(Dispatchers.IO) {
        val gson = com.google.gson.Gson()

        // 1. Check primary GitHub Releases API
        try {
            val connection = (URL(GITHUB_RELEASES_API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "RozkladPlus-Android/${BuildConfig.VERSION_NAME}")
            }
            if (connection.responseCode in 200..299) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val release = gson.fromJson(json, GitHubRelease::class.java)
                val tagName = release?.tagName?.trim().orEmpty()
                if (tagName.isNotBlank() && isNewerVersion(tagName, BuildConfig.VERSION_NAME)) {
                    val apkAsset = release?.assets?.firstOrNull {
                        it.name?.endsWith(".apk", ignoreCase = true) == true &&
                            !it.browserDownloadUrl.isNullOrBlank()
                    }
                    if (apkAsset != null) {
                        return@withContext AppUpdate(
                            versionCode = extractVersionCode(release.body) ?: (BuildConfig.VERSION_CODE + 1),
                            versionName = tagName.removePrefix("v").removePrefix("V"),
                            downloadUrl = apkAsset.browserDownloadUrl!!,
                            changelog = release.body ?: release.name ?: "New release available",
                            forceUpdate = false
                        )
                    }
                }
            }
        } catch (e: Exception) {
            LogCollector.log("UpdateManager", "GitHub Releases API check failed: ${e.message}")
        }

        // 2. Fallback to secondary endpoint if configured
        val fallbackUrl = BuildConfig.UPDATE_CHECK_URL.takeIf { it.isNotBlank() && !it.contains("example.com") }
        if (fallbackUrl != null) {
            try {
                val connection = (URL(fallbackUrl).openConnection() as HttpURLConnection).apply {
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
                }
            } catch (e: Exception) {
                LogCollector.log("UpdateManager", "Fallback OTA check failed: ${e.message}")
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
