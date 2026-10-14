package com.goldking.filetracker

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process

object ProcessResolver {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Finds the app/package that was active in the foreground around the given timestamp
     */
    fun getActivePackage(context: Context, targetTimeMs: Long): Pair<String, String> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return Pair("Unknown Process", "unknown.process")

        val startTime = (targetTimeMs - 30_000).coerceAtLeast(0)
        val endTime = targetTimeMs + 5_000

        var lastForegroundPackage = ""
        try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                ) {
                    lastForegroundPackage = event.packageName
                }
            }
        } catch (_: Exception) {
            // fallback
        }

        if (lastForegroundPackage.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startTime,
                    endTime
                )
                val mostRecent = stats?.maxByOrNull { it.lastTimeUsed }
                if (mostRecent != null && mostRecent.lastTimeUsed > 0) {
                    lastForegroundPackage = mostRecent.packageName
                }
            } catch (_: Exception) {
            }
        }

        if (lastForegroundPackage.isEmpty()) {
            return Pair("Background / System Process", "android.system")
        }

        val appLabel = getAppLabel(context, lastForegroundPackage)
        return Pair(appLabel, lastForegroundPackage)
    }

    private fun getAppLabel(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }
}
