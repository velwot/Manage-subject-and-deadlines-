package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Alert
import com.example.ui.components.ConfirmDialog
import com.example.ui.viewmodel.CampusMateViewModel
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: CampusMateViewModel
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var alertToDelete by remember { mutableStateOf<Alert?>(null) }
    var filterPendingOnly by remember { mutableStateOf(false) }

    val displayedAlerts = remember(alerts, filterPendingOnly) {
        if (filterPendingOnly) alerts.filter { !it.isCompleted } else alerts
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alerts & Deadlines", fontWeight = FontWeight.Bold) },
                actions = {
                    FilterChip(
                        selected = filterPendingOnly,
                        onClick = { filterPendingOnly = !filterPendingOnly },
                        label = { Text("Pending Only", fontSize = 12.sp) },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Alert") },
                modifier = Modifier.testTag("fab_add_alert")
            )
        }
    ) { innerPadding ->
        if (displayedAlerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Alerts or Submissions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Stay on top of assignment deadlines, quizzes, lab reports, and exam schedules.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("alerts_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedAlerts, key = { it.id }) { alert ->
                    AlertItemCard(
                        alert = alert,
                        onToggle = { viewModel.toggleAlert(alert.id, it) },
                        onDelete = { alertToDelete = alert }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var subjectName by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf(DateUtils.getCurrentIsoDate()) }
        var dueTime by remember { mutableStateOf("23:59") }
        var type by remember { mutableStateOf("Assignment") }
        var priority by remember { mutableStateOf("High") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Alert / Deadline", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Alert Title *") },
                        placeholder = { Text("e.g. Lab 2 Semaphore Submission") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_alert_title"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Subject (Optional)") },
                        placeholder = { Text("e.g. Operating Systems") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Due Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = dueTime,
                            onValueChange = { dueTime = it },
                            label = { Text("Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Assignment", "Quiz", "Exam", "Lab").forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t, fontSize = 11.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Submission Link") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addAlert(title, subjectName, dueDate, dueTime, type, priority, notes)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_alert")
                ) {
                    Text("Add Alert")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (alertToDelete != null) {
        val a = alertToDelete!!
        ConfirmDialog(
            title = "Delete Alert?",
            message = "Are you sure you want to remove '${a.title}'?",
            confirmButtonText = "Delete",
            onConfirm = {
                viewModel.deleteAlert(a)
                alertToDelete = null
            },
            onDismiss = { alertToDelete = null }
        )
    }
}

@Composable
fun AlertItemCard(
    alert: Alert,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = alert.isCompleted,
                onCheckedChange = onToggle
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (alert.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "${alert.subjectName.ifBlank { "General" }} • Due ${DateUtils.formatIsoToDisplay(alert.dueDate)} at ${alert.dueTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (alert.notes.isNotBlank()) {
                    Text(
                        text = alert.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
