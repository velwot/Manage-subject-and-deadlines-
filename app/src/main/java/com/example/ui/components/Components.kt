package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Subject
import com.example.ui.viewmodel.Screen
import com.example.util.DateUtils

@Composable
fun CampusBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bottom_nav_bar")
    ) {
        val isHome = currentScreen is Screen.Home
        val isSubjects = currentScreen is Screen.Subjects || currentScreen is Screen.SubjectDetail
        val isTodoList = currentScreen is Screen.TodoList
        val isAlerts = currentScreen is Screen.Alerts
        val isCalendar = currentScreen is Screen.Calendar
        val isSettings = currentScreen is Screen.Settings || currentScreen is Screen.Ideas

        NavigationBarItem(
            selected = isHome,
            onClick = { onNavigate(Screen.Home) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            modifier = Modifier.testTag("nav_home")
        )
        NavigationBarItem(
            selected = isSubjects,
            onClick = { onNavigate(Screen.Subjects) },
            icon = { Icon(Icons.Default.MenuBook, contentDescription = "Subjects") },
            label = { Text("Subjects") },
            modifier = Modifier.testTag("nav_subjects")
        )
        NavigationBarItem(
            selected = isTodoList,
            onClick = { onNavigate(Screen.TodoList) },
            icon = { Icon(Icons.Default.Checklist, contentDescription = "To-Do") },
            label = { Text("To-Do") },
            modifier = Modifier.testTag("nav_todo")
        )
        NavigationBarItem(
            selected = isAlerts,
            onClick = { onNavigate(Screen.Alerts) },
            icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Alerts") },
            label = { Text("Alerts") },
            modifier = Modifier.testTag("nav_alerts")
        )
        NavigationBarItem(
            selected = isCalendar,
            onClick = { onNavigate(Screen.Calendar) },
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
            label = { Text("Calendar") },
            modifier = Modifier.testTag("nav_calendar")
        )
        NavigationBarItem(
            selected = isSettings,
            onClick = { onNavigate(Screen.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            modifier = Modifier.testTag("nav_settings")
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmButtonText: String = "Delete",
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.testTag("confirm_dialog_btn")
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_dialog_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}

val SubjectColorPalette = listOf(
    "#2563EB", // Blue
    "#10B981", // Emerald
    "#8B5CF6", // Violet
    "#F59E0B", // Amber
    "#EF4444", // Rose
    "#06B6D4", // Cyan
    "#EC4899", // Pink
    "#475569"  // Slate
)

@Composable
fun ColorPickerRow(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubjectColorPalette.forEach { hex ->
            val color = try {
                Color(android.graphics.Color.parseColor(hex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }
            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(hex) }
                    .testTag("color_picker_$hex")
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateCountdownDialog(
    initialTargetDate: String = DateUtils.getIsoDatePlusDays(7),
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetDate: String, category: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf(initialTargetDate) }
    var category by remember { mutableStateOf("Exam") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Countdown", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Track remaining days until midterms, final exams, submissions, or holidays.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event / Exam Title *") },
                    placeholder = { Text("e.g. Operating Systems Final Exam") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_countdown_title"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Target Date (YYYY-MM-DD) *") },
                    trailingIcon = {
                        val days = DateUtils.getDaysRemaining(targetDate)
                        Text(
                            text = "$days d",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_countdown_date"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))
                // Quick date presets
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "+3d" to 3,
                        "+1wk" to 7,
                        "+2wk" to 14,
                        "+1mo" to 30
                    ).forEach { (label, days) ->
                        val presetDate = DateUtils.getIsoDatePlusDays(days)
                        FilterChip(
                            selected = targetDate == presetDate,
                            onClick = { targetDate = presetDate },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("chip_preset_$label")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Category:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Exam", "Milestone", "Holiday", "Deadline", "Quiz").forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c, fontSize = 11.sp) },
                            modifier = Modifier.testTag("chip_category_$c")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Chapters 1-5, Hall B, bring calculator...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_countdown_notes"),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && targetDate.isNotBlank()) {
                        onConfirm(title, targetDate, category, notes)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && targetDate.isNotBlank(),
                modifier = Modifier.testTag("btn_confirm_countdown")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Countdown")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_countdown")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateAlertDialog(
    subjects: List<Subject> = emptyList(),
    initialSubjectName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (title: String, subjectName: String, dueDate: String, dueTime: String, type: String, priority: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subjectName by remember { mutableStateOf(initialSubjectName) }
    var dueDate by remember { mutableStateOf(DateUtils.getIsoDatePlusDays(3)) }
    var dueTime by remember { mutableStateOf("23:59") }
    var type by remember { mutableStateOf("Assignment") }
    var priority by remember { mutableStateOf("High") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Alert / Deadline", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add reminders for assignment submissions, quizzes, and project milestones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    placeholder = { Text("e.g. Operating Systems Lab 3 Submission") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_alert_title"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject (Optional)") },
                    placeholder = { Text("e.g. Operating Systems") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_alert_subject"),
                    singleLine = true
                )

                if (subjects.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Or pick subject:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.take(4).forEach { s ->
                            FilterChip(
                                selected = subjectName == s.name,
                                onClick = { subjectName = s.name },
                                label = { Text(s.code.ifBlank { s.name.take(10) }, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date *") },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("input_alert_date"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dueTime,
                        onValueChange = { dueTime = it },
                        label = { Text("Time") },
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("input_alert_time"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                // Quick date presets for alerts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Today" to 0,
                        "Tomorrow" to 1,
                        "+3d" to 3,
                        "+1wk" to 7
                    ).forEach { (label, days) ->
                        val preset = DateUtils.getIsoDatePlusDays(days)
                        FilterChip(
                            selected = dueDate == preset,
                            onClick = { dueDate = preset },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Type:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Assignment", "Quiz", "Project", "Exam", "Lab").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Priority:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("High", "Medium", "Low").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = {
                                Text(
                                    text = when (p) {
                                        "High" -> "🔥 High"
                                        "Medium" -> "⚡ Medium"
                                        else -> "💤 Low"
                                    },
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier.testTag("chip_priority_$p")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Submission Portal Link") },
                    placeholder = { Text("Upload PDF to portal before 11:59 PM") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_alert_notes"),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && dueDate.isNotBlank()) {
                        onConfirm(title, subjectName, dueDate, dueTime, type, priority, notes)
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank() && dueDate.isNotBlank(),
                modifier = Modifier.testTag("btn_confirm_alert")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Alert")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_alert")
            ) {
                Text("Cancel")
            }
        }
    )
}
