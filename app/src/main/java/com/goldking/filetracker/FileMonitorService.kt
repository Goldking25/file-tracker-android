package com.goldking.filetracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File

class FileMonitorService : Service() {

    private val observers = mutableListOf<DirectoryFileObserver>()
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private val channelId = "file_tracker_monitor_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1001, buildNotification("File Shield active - Monitoring storage & process events"))
        startWatching()
        registerClipboardWatcher()
        EventRepository.setMonitoring(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "File Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when background file monitoring is active"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("File Shield Tracker")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startWatching() {
        val targets = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        )

        for (dir in targets) {
            if (dir.exists() || dir.mkdirs()) {
                val obs = DirectoryFileObserver(this, dir) { event ->
                    EventRepository.addEvent(event)
                    if (event.severity == AlertSeverity.RED) {
                        pushAlertNotification(event)
                    }
                }
                obs.startWatching()
                observers.add(obs)
            }
        }
    }

    private fun registerClipboardWatcher() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val item = clip.getItemAt(0)
                val textSnippet = item.text?.toString() ?: item.uri?.toString() ?: "Binary/Media"
                val eventTime = System.currentTimeMillis()
                val (appName, packageName) = ProcessResolver.getActivePackage(this, eventTime)

                val event = FileAccessEvent(
                    filePath = item.uri?.toString() ?: "System Clipboard",
                    eventType = "DATA_COPIED_CLIPBOARD",
                    severity = AlertSeverity.RED,
                    description = "Data or file copied from an app to clipboard: ${textSnippet.take(40)}",
                    appName = appName,
                    packageName = packageName,
                    timestamp = eventTime
                )
                EventRepository.addEvent(event)
                pushAlertNotification(event)
            }
        }
        clipboard.addPrimaryClipChangedListener(clipboardListener)
    }

    private fun pushAlertNotification(event: FileAccessEvent) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alertChannelId = "file_tracker_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                alertChannelId,
                "Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val alertNotif = NotificationCompat.Builder(this, alertChannelId)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("RED FLAG: [${event.appName}] Unauthorized Access")
            .setContentText("${event.description} - (${event.packageName})")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify((System.currentTimeMillis() % 10000).toInt(), alertNotif)
    }

    override fun onDestroy() {
        super.onDestroy()
        observers.forEach { it.stopWatching() }
        observers.clear()
        clipboardListener?.let {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.removePrimaryClipChangedListener(it)
        }
        EventRepository.setMonitoring(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
