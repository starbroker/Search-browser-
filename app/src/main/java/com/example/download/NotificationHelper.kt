package com.example.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import java.io.File

object NotificationHelper {
    const val CHANNEL_DOWNLOADS = "browser_downloads"
    const val CHANNEL_COMPLETED = "browser_downloads_complete"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Progress channel
            val progressChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            
            // Completed channel
            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED,
                "Download Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            
            manager.createNotificationChannel(progressChannel)
            manager.createNotificationChannel(completedChannel)
        }
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(java.util.Locale.US, "%.1f MB", mb)
    }

    fun buildProgressNotification(context: Context, task: DownloadTask): Notification {
        val downloaded = formatBytes(task.downloadedBytes)
        val total = if (task.totalBytes > 0) formatBytes(task.totalBytes) else "Unknown"
        val progressText = "$downloaded / $total"

        val builder = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setContentTitle(task.fileName)
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        if (task.totalBytes > 0) {
            builder.setProgress(100, (task.downloadedBytes * 100 / task.totalBytes).toInt(), false)
        } else {
            builder.setProgress(100, 0, true)
        }

        // Setup actions
        val cancelIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_CANCEL
            putExtra(DownloadService.EXTRA_ID, task.id)
        }
        val cancelPending = PendingIntent.getService(
            context, task.id * 10 + 1, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_delete, "Cancel", cancelPending)

        if (task.status == DownloadStatus.PAUSED) {
            val resumeIntent = Intent(context, DownloadService::class.java).apply {
                action = DownloadService.ACTION_RESUME
                putExtra(DownloadService.EXTRA_ID, task.id)
            }
            val resumePending = PendingIntent.getService(
                context, task.id * 10 + 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
        } else {
            val pauseIntent = Intent(context, DownloadService::class.java).apply {
                action = DownloadService.ACTION_PAUSE
                putExtra(DownloadService.EXTRA_ID, task.id)
            }
            val pausePending = PendingIntent.getService(
                context, task.id * 10 + 3, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
        }

        return builder.build()
    }

    fun buildCompleteNotification(context: Context, task: DownloadTask): Notification {
        val file = File(task.filePath)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, task.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, task.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_COMPLETED)
            .setContentTitle("Download complete")
            .setContentText(task.fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    fun buildFailedNotification(context: Context, task: DownloadTask): Notification {
        val retryIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START
            putExtra(DownloadService.EXTRA_ID, task.id)
        }
        val retryPending = PendingIntent.getService(
            context, task.id, retryIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_COMPLETED)
            .setContentTitle("Download failed")
            .setContentText(task.fileName)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .addAction(android.R.drawable.ic_popup_sync, "Retry", retryPending)
            .setAutoCancel(true)
            .build()
    }
}
