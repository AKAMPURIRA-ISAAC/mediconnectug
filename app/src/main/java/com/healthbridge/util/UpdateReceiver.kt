package com.healthbridge.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import java.io.File

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == UpdateChecker.downloadId) {
            // Download complete
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    // Show install dialog
                    showInstallDialog(context, Uri.parse(uri))
                }
            }
            cursor.close()
        }
    }

    private fun showInstallDialog(context: Context, apkUri: Uri) {
        AlertDialog.Builder(context)
            .setTitle("Update Downloaded")
            .setMessage("The update has been downloaded. Install now to apply the update?")
            .setPositiveButton("Install & Restart") { _, _ ->
                installApk(context, apkUri)
            }
            .setNegativeButton("Later") { _, _ ->
                // Do nothing, user can install later
            }
            .setCancelable(false)
            .show()
    }

    private fun installApk(context: Context, apkUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
