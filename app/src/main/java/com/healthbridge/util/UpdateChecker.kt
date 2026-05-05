package com.healthbridge.util

import android.app.Activity
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Checks GitHub Releases for a newer version and downloads the update in the background.
 *
 * How it works:
 *  1. Calls https://api.github.com/repos/AKAMPURIRA-ISAAC/mediconnectug/releases/latest
 *  2. Parses the `tag_name` and assets for APK download URL
 *  3. Compares with the app's current BuildConfig.VERSION_NAME
 *  4. If newer → starts background download using DownloadManager
 *  5. When download completes, UpdateReceiver shows install dialog
 *
 * The check is silently skipped if:
 *  - No internet connection
 *  - User already dismissed this version today
 *  - Any network/parse error (never crashes the app)
 */
object UpdateChecker {

    private const val GITHUB_API =
        "https://api.github.com/repos/AKAMPURIRA-ISAAC/mediconnectug/releases/latest"
    private const val PREF_SKIPPED_VERSION = "update_skipped_version"
    private const val PREF_LAST_CHECK_DATE = "update_last_check_date"

    var downloadId: Long = -1

    /**
     * Run this from HomeActivity (lifecycleScope.launch) after the user is logged in.
     */
    suspend fun checkForUpdate(activity: Activity) {
        try {
            val prefs = activity.getSharedPreferences("HealthBridge", Context.MODE_PRIVATE)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())

            // Only check once per day to avoid spamming the GitHub API
            val lastCheck = prefs.getString(PREF_LAST_CHECK_DATE, "")
            if (lastCheck == today) return

            // Fetch latest release info from GitHub
            val json = withContext(Dispatchers.IO) {
                URL(GITHUB_API).openConnection().apply {
                    setRequestProperty("Accept", "application/vnd.github+json")
                    connectTimeout = 5000
                    readTimeout = 5000
                }.getInputStream().bufferedReader().readText()
            }

            val latestTag  = JSONObject(json).optString("tag_name", "") // e.g. "v1.0.4"
            val latestName = JSONObject(json).optString("name", "")     // e.g. "MediConnectUG v1.0.4"
            val releaseNotes = JSONObject(json).optString("body", "")
                .lines().take(6).joinToString("\n")                     // first 6 lines of notes

            // Get APK download URL from assets
            val assets = JSONObject(json).optJSONArray("assets")
            var apkUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            if (latestTag.isBlank() || apkUrl.isBlank()) return

            // Strip leading "v" → "1.0.4"
            val latestVersion  = latestTag.trimStart('v')
            val currentVersion = activity.packageManager
                .getPackageInfo(activity.packageName, 0).versionName
                .replace("-debug", "").trimStart('v')

            // Mark today as checked
            prefs.edit().putString(PREF_LAST_CHECK_DATE, today).apply()

            // Skip if user already said "Later" for this exact version today
            val skippedVersion = prefs.getString(PREF_SKIPPED_VERSION, "")
            if (skippedVersion == latestVersion) return

            // Compare semantic versions
            if (isNewerVersion(latestVersion, currentVersion)) {
                withContext(Dispatchers.Main) {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        // Automatically start download without user confirmation
                        startDownload(activity, apkUrl)
                        // Show notification that update is downloading
                        showDownloadNotification(activity, latestVersion)
                    }
                }
            }
        } catch (_: Exception) {
            // Silently ignore all errors — never crash the app over an update check
        }
    }

    private fun startDownload(activity: Activity, apkUrl: String) {
        val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("HealthBridge Update")
            setDescription("Downloading app update...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir("Download", "healthbridge_update.apk")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        downloadId = downloadManager.enqueue(request)
    }

    private fun showDownloadNotification(activity: Activity, latestVersion: String) {
        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "update_download"
        val channelName = "Update Download"
        val notificationId = 1

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        // PendingIntent to open the app when the notification is tapped
        val pendingIntent = PendingIntent.getActivity(
            activity, 0, Intent(activity, activity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build and show the notification
        val notification = NotificationCompat.Builder(activity, channelId)
            .setContentTitle("HealthBridge Update")
            .setContentText("Update to version $latestVersion is downloading...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Returns true if [latest] > [current] using basic semantic version comparison.
     * Handles "1.0.4" > "1.0.3", "2.0.0" > "1.9.9", etc.
     */
    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts  = latest.split(".").mapNotNull  { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val len = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until len) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
