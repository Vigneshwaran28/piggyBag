package com.titanbag.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.titanbag.app.data.api.CloudApiClient

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val sessionManager = SessionManager(applicationContext)
        if (!sessionManager.isLoggedIn()) {
            return Result.success()
        }

        val db = AppDatabase.getDatabase(applicationContext)
        val apiClient = CloudApiClient(sessionManager)
        val repository = CloudRepository(db, apiClient, sessionManager)

        val result = repository.sync()
        return if (result.isSuccess) {
            // No need to show success notifications
            Result.success()
        } else {
            showNotification("Cloud Sync Failed", "Could not synchronize journals: ${result.exceptionOrNull()?.message}")
            Result.retry()
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "titanbag_sync_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TitanBag Cloud Sync",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
