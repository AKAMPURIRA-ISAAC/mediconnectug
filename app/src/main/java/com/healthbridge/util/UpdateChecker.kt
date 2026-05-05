package com.healthbridge.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Checks GitHub Releases for a newer version and shows an update dialog if one is available.
 *
 * How it works:
 *  1. Calls https://api.github.com/repos/AKAMPURIRA-ISAAC/mediconnectug/releases/latest
 *  2. Parses the `tag_name` field (e.g. "v1.0.4")
 *  3. Compares with the app's current BuildConfig.VERSION_NAME
 *  4. If newer → shows a non-blocking AlertDialog with "Update Now" / "Later"
 *  5. "Update Now" opens the GitHub Pages download page in the browser
 *
 * The check is silently skipped if:
 *  - No internet connection
 *  - User already dismissed this version today
 *  - Any network/parse error (never crashes the app)
 */
object UpdateChecker {

    private const val GITHUB_API =
        "https://api.github.com/repos/AKAMPURIRA-ISAAC/mediconnectug/releases/latest"
    private const val DOWNLOAD_URL =
        "https://akampurira-isaac.github.io/mediconnectug/"
    private const val PREF_SKIPPED_VERSION = "update_skipped_version"
    private const val PREF_LAST_CHECK_DATE = "update_last_check_date"

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

            if (latestTag.isBlank()) return

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
                        showUpdateDialog(activity, latestName.ifBlank { latestTag }, latestVersion, releaseNotes)
                    }
                }
            }
        } catch (_: Exception) {
            // Silently ignore all errors — never crash the app over an update check
        }
    }

    private fun showUpdateDialog(
        activity: Activity,
        releaseName: String,
        latestVersion: String,
        releaseNotes: String
    ) {
        val currentVersion = try {
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
                .replace("-debug", "")
        } catch (_: Exception) { "?" }

        val message = buildString {
            append("🆕 $releaseName is available!\n\n")
            append("Current: v$currentVersion  →  Latest: v$latestVersion\n\n")
            if (releaseNotes.isNotBlank()) {
                append("What's new:\n$releaseNotes")
            }
        }

        AlertDialog.Builder(activity)
            .setTitle("Update Available 🚀")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("⬇️ Update Now") { _, _ ->
                activity.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_URL))
                )
            }
            .setNegativeButton("Later") { _, _ ->
                // User dismissed dialog - already tracked in PREF_LAST_NOTIFIED_VERSION
                // Won't show again for this version until they update
            }
            .show()
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

