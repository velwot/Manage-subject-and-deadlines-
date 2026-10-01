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
import com.example.ui.components.CreateAlertDialog
import com.example.ui.viewmodel.CampusMateViewModel
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: CampusMateViewModel
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
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
                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("top_bar_create_alert_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Alert", fontSize = 13.sp)
                    }
                    FilterChip(
                        selected = filterPendingOnly,
                        onClick = { filterPendingOnly = !filterPendingOnly },
                        label = { Text("Pending", fontSize = 12.sp) },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Create Alert") },
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
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("empty_create_alert_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Alert")
                    }
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
        CreateAlertDialog(
            subjects = subjects,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, subjectName, dueDate, dueTime, type, priority, notes ->
                viewModel.addAlert(title, subjectName, dueDate, dueTime, type, priority, notes)
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
