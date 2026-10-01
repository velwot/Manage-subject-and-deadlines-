package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.Screen

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
