package com.goldking.filetracker

enum class AlertSeverity {
    YELLOW, // Normal external/internal touch/access
    RED     // Unauthorized / Locked access / Suspected exfiltration or copy
}

data class FileAccessEvent(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val filePath: String,
    val eventType: String,
    val severity: AlertSeverity,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
