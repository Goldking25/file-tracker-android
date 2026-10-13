package com.goldking.filetracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            val events by EventRepository.eventsFlow.collectAsState()
            val isMonitoring by EventRepository.isMonitoring.collectAsState()

            FileTrackerScreen(
                isMonitoring = isMonitoring,
                events = events,
                onToggleMonitoring = {
                    if (isMonitoring) {
                        stopService(Intent(this, FileMonitorService::class.java))
                    } else {
                        val intent = Intent(this, FileMonitorService::class.java)
                        ContextCompat.startForegroundService(this, intent)
                    }
                },
                onClearLogs = { EventRepository.clearEvents() }
            )
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTrackerScreen(
    isMonitoring: Boolean,
    events: List<FileAccessEvent>,
    onToggleMonitoring: () -> Unit,
    onClearLogs: () -> Unit
) {
    val redCount = events.count { it.severity == AlertSeverity.RED }
    val yellowCount = events.count { it.severity == AlertSeverity.YELLOW }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Shield Monitor", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onClearLogs) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear logs")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("RED FLAGS", color = Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("$redCount", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828))
                        Text("Locked / Copied data", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("YELLOW FLAGS", color = Color(0xFFF57F17), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("$yellowCount", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF57F17))
                        Text("Normal touch/modified", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = onToggleMonitoring,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isMonitoring) "Stop Shield Protection" else "Start Shield Protection")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Live Access Audit Trail (${events.size})", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(8.dp))

            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No file access incidents recorded yet.\nStart protection to monitor files.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events, key = { it.id }) { item ->
                        EventItemCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun EventItemCard(event: FileAccessEvent) {
    val isRed = event.severity == AlertSeverity.RED
    val tagBg = if (isRed) Color(0xFFD32F2F) else Color(0xFFFBC02D)
    val cardBg = if (isRed) Color(0xFFFFF5F5) else Color(0xFFFFFFFA)
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(tagBg, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = event.eventType,
                        fontWeight = FontWeight.Bold,
                        color = if (isRed) Color(0xFFC62828) else Color(0xFFE65100),
                        fontSize = 13.sp
                    )
                    Text(
                        text = timeFormat.format(Date(event.timestamp)),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(event.description, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    event.filePath,
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    maxLines = 2
                )
            }
        }
    }
}
