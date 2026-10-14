package com.goldking.filetracker

import android.app.KeyguardManager
import android.content.Context
import android.os.FileObserver
import java.io.File

class DirectoryFileObserver(
    private val context: Context,
    private val directory: File,
    private val onAlertTriggered: (FileAccessEvent) -> Unit
) : FileObserver(directory.absolutePath, ALL_EVENTS) {

    private val keyguardManager =
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    override fun onEvent(event: Int, path: String?) {
        if (path == null) return
        val fullPath = "${directory.absolutePath}/$path"
        val isLocked = keyguardManager.isDeviceLocked
        val baseMask = event and ALL_EVENTS
        val eventTime = System.currentTimeMillis()

        // Resolve which app or process was active at the time of file touch
        val (appName, packageName) = ProcessResolver.getActivePackage(context, eventTime)

        when (baseMask) {
            OPEN, ACCESS -> {
                if (isLocked) {
                    onAlertTriggered(
                        FileAccessEvent(
                            filePath = fullPath,
                            eventType = "OPEN_WHILE_LOCKED",
                            severity = AlertSeverity.RED,
                            description = "File touched/read while device screen was locked!",
                            appName = appName,
                            packageName = packageName,
                            timestamp = eventTime
                        )
                    )
                } else {
                    onAlertTriggered(
                        FileAccessEvent(
                            filePath = fullPath,
                            eventType = "TOUCH_READ",
                            severity = AlertSeverity.YELLOW,
                            description = "File touched/opened normally",
                            appName = appName,
                            packageName = packageName,
                            timestamp = eventTime
                        )
                    )
                }
            }
            MODIFY, CLOSE_WRITE -> {
                if (isLocked) {
                    onAlertTriggered(
                        FileAccessEvent(
                            filePath = fullPath,
                            eventType = "WRITE_WHILE_LOCKED",
                            severity = AlertSeverity.RED,
                            description = "File modified/saved while device was locked!",
                            appName = appName,
                            packageName = packageName,
                            timestamp = eventTime
                        )
                    )
                } else {
                    onAlertTriggered(
                        FileAccessEvent(
                            filePath = fullPath,
                            eventType = "FILE_MODIFIED",
                            severity = AlertSeverity.YELLOW,
                            description = "File modified by external/internal source",
                            appName = appName,
                            packageName = packageName,
                            timestamp = eventTime
                        )
                    )
                }
            }
            CREATE, MOVED_TO -> {
                if (isLocked) {
                    onAlertTriggered(
                        FileAccessEvent(
                            filePath = fullPath,
                            eventType = "CREATED_WHILE_LOCKED",
                            severity = AlertSeverity.RED,
                            description = "File newly placed/moved in while device was locked!",
                            appName = appName,
                            packageName = packageName,
                            timestamp = eventTime
                        )
                    )
                } else {
                    onAlertTriggered(
                        FileAccessEvent(
                            filePath = fullPath,
                            eventType = "FILE_CREATED",
                            severity = AlertSeverity.YELLOW,
                            description = "New file created or received",
                            appName = appName,
                            packageName = packageName,
                            timestamp = eventTime
                        )
                    )
                }
            }
            DELETE, MOVED_FROM -> {
                onAlertTriggered(
                    FileAccessEvent(
                        filePath = fullPath,
                        eventType = "FILE_REMOVED",
                        severity = if (isLocked) AlertSeverity.RED else AlertSeverity.YELLOW,
                        description = if (isLocked) "File removed/extracted while locked!" else "File deleted or moved out",
                        appName = appName,
                        packageName = packageName,
                        timestamp = eventTime
                    )
                )
            }
        }
    }
}
