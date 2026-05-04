package com.healthbridge.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

object NetworkUtils {

    /**
     * Returns true if the device has an active internet connection (WiFi or mobile data).
     */
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Shows a user-friendly toast when there is no network.
     * Returns true if offline (caller should stop the request).
     */
    fun checkAndWarn(context: Context): Boolean {
        if (!isOnline(context)) {
            Toast.makeText(
                context,
                "📵 No internet connection — showing cached data",
                Toast.LENGTH_LONG
            ).show()
            return true   // IS offline
        }
        return false      // IS online
    }

    /**
     * Shows a toast warning about Render cold start on first load.
     * Call this when the first network request is made after app launch.
     */
    fun warnIfFirstLoad(context: Context, isFirstLoad: Boolean) {
        if (isFirstLoad && isOnline(context)) {
            Toast.makeText(
                context,
                "⏳ Connecting to server… first load may take ~15 seconds",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

