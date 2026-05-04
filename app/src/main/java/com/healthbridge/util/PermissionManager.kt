package com.healthbridge.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Central manager for all runtime permission requests in MediConnectUG.
 *
 * Usage:
 *   // In HomeActivity — request on-boarding permissions:
 *   PermissionManager.requestOnboardingPermissions(this)
 *
 *   // Before making a phone call:
 *   PermissionManager.withCallPermission(this) { makeTheCall() }
 *
 *   // Before accessing camera or gallery:
 *   PermissionManager.withMediaPermission(this) { openCamera() }
 *
 *   // Before showing nearby hospitals on map:
 *   PermissionManager.withLocationPermission(this) { openMap() }
 */
object PermissionManager {

    // ── Request codes ────────────────────────────────────────────────────────
    const val RC_CALL_PHONE         = 101
    const val RC_LOCATION           = 102
    const val RC_CAMERA             = 103
    const val RC_MEDIA              = 104
    const val RC_NOTIFICATIONS      = 105
    const val RC_ONBOARDING_ALL     = 110

    // ── Permission lists ─────────────────────────────────────────────────────
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val MEDIA_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val ONBOARDING_PERMISSIONS: Array<String> get() {
        val list = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list += Manifest.permission.POST_NOTIFICATIONS
            list += Manifest.permission.READ_MEDIA_IMAGES
        } else {
            list += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return list.toTypedArray()
    }

    // ── Check helpers ────────────────────────────────────────────────────────

    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasCallPermission(context: Context)     = hasPermission(context, Manifest.permission.CALL_PHONE)
    fun hasLocationPermission(context: Context) = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            || hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    fun hasCameraPermission(context: Context)   = hasPermission(context, Manifest.permission.CAMERA)
    fun hasMediaPermission(context: Context)    = MEDIA_PERMISSIONS.all { hasPermission(context, it) }
    fun hasNotificationPermission(context: Context) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        else true

    // ── Request on first launch (covers most needed permissions at once) ───

    fun requestOnboardingPermissions(activity: Activity) {
        val needed = ONBOARDING_PERMISSIONS.filter { !hasPermission(activity, it) }.toTypedArray()
        if (needed.isEmpty()) return
        ActivityCompat.requestPermissions(activity, needed, RC_ONBOARDING_ALL)
    }

    // ── Just-in-time permission gates ─────────────────────────────────────

    /**
     * Run [action] only if CALL_PHONE is granted.
     * If denied, show a rationale dialog. If permanently denied, send user to app settings.
     */
    fun withCallPermission(activity: Activity, action: () -> Unit) {
        when {
            hasCallPermission(activity) -> action()
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CALL_PHONE) -> {
                AlertDialog.Builder(activity)
                    .setTitle("📞 Phone Permission Needed")
                    .setMessage("MediConnectUG needs permission to call emergency numbers and doctors directly from the app.")
                    .setPositiveButton("Allow") { _, _ ->
                        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CALL_PHONE), RC_CALL_PHONE)
                    }
                    .setNegativeButton("Not now", null)
                    .show()
            }
            else -> {
                // Check if previously denied permanently
                val prefs = activity.getSharedPreferences("HealthBridge", Context.MODE_PRIVATE)
                val askedBefore = prefs.getBoolean("asked_call_perm", false)
                if (askedBefore) {
                    showSettingsDialog(activity, "Call Permission Denied",
                        "To call doctors and emergency services, enable the Phone permission in App Settings.")
                } else {
                    prefs.edit().putBoolean("asked_call_perm", true).apply()
                    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CALL_PHONE), RC_CALL_PHONE)
                }
            }
        }
    }

    /**
     * Run [action] only if location permission is granted.
     */
    fun withLocationPermission(activity: Activity, action: () -> Unit) {
        when {
            hasLocationPermission(activity) -> action()
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                AlertDialog.Builder(activity)
                    .setTitle("📍 Location Permission Needed")
                    .setMessage("MediConnectUG uses your location to find nearby hospitals and doctors.")
                    .setPositiveButton("Allow") { _, _ ->
                        ActivityCompat.requestPermissions(activity, LOCATION_PERMISSIONS, RC_LOCATION)
                    }
                    .setNegativeButton("Not now", null)
                    .show()
            }
            else -> ActivityCompat.requestPermissions(activity, LOCATION_PERMISSIONS, RC_LOCATION)
        }
    }

    /**
     * Run [action] only if camera permission is granted.
     */
    fun withCameraPermission(activity: Activity, action: () -> Unit) {
        when {
            hasCameraPermission(activity) -> action()
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(activity)
                    .setTitle("📷 Camera Permission Needed")
                    .setMessage("Allow camera access to take your profile photo or scan medical documents.")
                    .setPositiveButton("Allow") { _, _ ->
                        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), RC_CAMERA)
                    }
                    .setNegativeButton("Not now", null)
                    .show()
            }
            else -> ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), RC_CAMERA)
        }
    }

    /**
     * Run [action] only if media/storage permission is granted.
     */
    fun withMediaPermission(activity: Activity, action: () -> Unit) {
        when {
            hasMediaPermission(activity) -> action()
            else -> ActivityCompat.requestPermissions(activity, MEDIA_PERMISSIONS, RC_MEDIA)
        }
    }

    // ── Helper: send user to App Settings when permanently denied ──────────

    fun showSettingsDialog(activity: Activity, title: String, message: String) {
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage("$message\n\nGo to App Settings → Permissions to enable it.")
            .setPositiveButton("Open Settings") { _, _ ->
                activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Call this from onRequestPermissionsResult() in any Activity to handle
     * the result and run the appropriate action.
     *
     * Returns true if the permission was granted, false if denied.
     */
    fun handleResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: (requestCode: Int) -> Unit = {},
        onDenied: (requestCode: Int) -> Unit = {}
    ) {
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            onGranted(requestCode)
        } else {
            onDenied(requestCode)
        }
    }
}

