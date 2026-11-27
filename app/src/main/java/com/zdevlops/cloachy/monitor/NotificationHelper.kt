package com.zdevlops.cloachy.monitor

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zdevlops.cloachy.MainActivity

object NotificationHelper {

    const val CHANNEL_GUARD_ID = "cloachy_gpu_guard_alerts"
    const val CHANNEL_SERVICE_ID = "cloachy_gpu_guard_service"
    const val CHANNEL_APP_UPDATE_ID = "cloachy_app_updates"

    const val SERVICE_NOTIF_ID = 7001
    const val ALERT_NOTIF_ID = 7002
    const val APP_UPDATE_NOTIF_ID = 7003

    const val EXTRA_OPEN_TAB = "extra_open_tab"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alertChannel = NotificationChannel(
                CHANNEL_GUARD_ID,
                "BLOXMOD GPU Auto-Optimizer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi peringatan saat GPU > 80% atau 100% dan grafik otomatis diturunkan"
                enableVibration(true)
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "BLOXMOD Background GPU Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Memantau beban GPU render saat bermain Roblox"
            }

            val updateChannel = NotificationChannel(
                CHANNEL_APP_UPDATE_ID,
                "Update Aplikasi CLOACHY",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pemberitahuan rilis versi baru CLOACHY dan pembaruan di dalam aplikasi"
                enableVibration(true)
            }

            nm.createNotificationChannel(alertChannel)
            nm.createNotificationChannel(serviceChannel)
            nm.createNotificationChannel(updateChannel)
        }
    }

    fun buildServiceNotification(context: Context, statusText: String): android.app.Notification {
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("CLOACHY · GPU Guard Aktif")
            .setContentText(statusText)
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun sendGpuAutoDowngradeAlert(
        context: Context,
        gpuPercent: Int,
        isMax100: Boolean,
        actionDetail: String
    ) {
        ensureChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isMax100) {
            "BLOXMOD: GPU Mentok 100% · Grafik Turun ke Lv. 1"
        } else {
            "BLOXMOD: GPU Tinggi ($gpuPercent%) · Grafik Disesuaikan"
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_GUARD_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(actionDetail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(actionDetail))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ALERT_NOTIF_ID, notif)
        } catch (_: SecurityException) {
        }
    }

    fun sendAppUpdateNotification(
        context: Context,
        latestVersion: String,
        releaseNotes: String
    ) {
        ensureChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openProfilIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TAB, "PROFIL")
        }
        val pi = PendingIntent.getActivity(
            context,
            2,
            openProfilIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = "CLOACHY v$latestVersion tersedia! Ketuk untuk update langsung di tab Profil. $releaseNotes"

        val notif = NotificationCompat.Builder(context, CHANNEL_APP_UPDATE_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Update CLOACHY v$latestVersion Tersedia")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(APP_UPDATE_NOTIF_ID, notif)
        } catch (_: SecurityException) {
        }
    }
}
