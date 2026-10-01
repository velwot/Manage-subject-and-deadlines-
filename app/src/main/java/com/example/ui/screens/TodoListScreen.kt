package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TodoTask
import com.example.ui.components.ConfirmDialog
import com.example.ui.viewmodel.CampusMateViewModel
import com.example.util.DateUtils

enum class TodoFilter {
    ALL,
    HIGH_PRIORITY,
    COMING_DUE,
    ADD_LATER,
    COMPLETED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    viewModel: CampusMateViewModel
) {
    val tasks by viewModel.todoTasks.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(TodoFilter.ALL) }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToUpdateDueDate by remember { mutableStateOf<TodoTask?>(null) }
    var taskToDelete by remember { mutableStateOf<TodoTask?>(null) }

    val filteredTasks = remember(tasks, selectedFilter) {
        when (selectedFilter) {
            TodoFilter.ALL -> tasks
            TodoFilter.HIGH_PRIORITY -> tasks.filter { it.priority == "High" && !it.isCompleted }
            TodoFilter.COMING_DUE -> tasks.filter { it.dueDate.isNotBlank() && !it.isCompleted }
            TodoFilter.ADD_LATER -> tasks.filter { it.dueDate.isBlank() && !it.isCompleted }
            TodoFilter.COMPLETED -> tasks.filter { it.isCompleted }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "To-Do List",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Prioritized Tasks & Upcoming Deadlines",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddTaskDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Task") },
                modifier = Modifier.testTag("fab_add_todo_task")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == TodoFilter.ALL,
                        onClick = { selectedFilter = TodoFilter.ALL },
                        label = { Text("All (${tasks.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TodoFilter.HIGH_PRIORITY,
                        onClick = { selectedFilter = TodoFilter.HIGH_PRIORITY },
                        label = {
                            Text("🔥 High (${tasks.count { it.priority == "High" && !it.isCompleted }})")
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TodoFilter.COMING_DUE,
                        onClick = { selectedFilter = TodoFilter.COMING_DUE },
                        label = {
                            Text("📅 Coming Due (${tasks.count { it.dueDate.isNotBlank() && !it.isCompleted }})")
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TodoFilter.ADD_LATER,
                        onClick = { selectedFilter = TodoFilter.ADD_LATER },
                        label = {
                            Text("⏳ Add Later (${tasks.count { it.dueDate.isBlank() && !it.isCompleted }})")
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TodoFilter.COMPLETED,
                        onClick = { selectedFilter = TodoFilter.COMPLETED },
                        label = {
                            Text("✓ Done (${tasks.count { it.isCompleted }})")
                        }
                    )
                }
            }

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Tasks in this Filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add academic or personal tasks with priority and coming due dates.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddTaskDialog = true },
                            modifier = Modifier.testTag("empty_add_task_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Task")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("todo_tasks_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TodoTaskCard(
                            task = task,
                            onToggle = { viewModel.toggleTodoTask(task.id, it) },
                            onSetDueDate = { taskToUpdateDueDate = task },
                            onDelete = { taskToDelete = task }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        var title by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("Medium") }
        var addDueDateLater by remember { mutableStateOf(false) }
        var dueDate by remember { mutableStateOf(DateUtils.getCurrentIsoDate()) }
        var dueTime by remember { mutableStateOf("17:00") }
        var category by remember { mutableStateOf("Academic") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add To-Do Task", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title *") },
                        placeholder = { Text("e.g. Borrow OS textbook from library") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_todo_title"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Priority:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.testTag("chip_priority_$p")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Due Date switch: Coming Due Date vs Add Later
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Due Date Option", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = if (addDueDateLater) "Due date will be added later" else "Set coming due date now",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = !addDueDateLater,
                            onCheckedChange = { addDueDateLater = !it },
                            modifier = Modifier.testTag("switch_due_date")
                        )
                    }

                    if (!addDueDateLater) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = dueDate,
                                onValueChange = { dueDate = it },
                                label = { Text("Coming Due Date") },
                                placeholder = { Text("YYYY-MM-DD") },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("input_todo_due_date"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = dueTime,
                                onValueChange = { dueTime = it },
                                label = { Text("Time") },
                                placeholder = { Text("17:00") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val finalDueDate = if (addDueDateLater) "" else dueDate
                            val finalDueTime = if (addDueDateLater) "" else dueTime
                            viewModel.addTodoTask(title, priority, finalDueDate, finalDueTime, category, notes)
                            showAddTaskDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_add_task")
                ) {
                    Text("Add Task")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Update / Set Due Date Dialog (for tasks that have "Add Later" or want to change date)
    if (taskToUpdateDueDate != null) {
        val task = taskToUpdateDueDate!!
        var newDueDate by remember(task) {
            mutableStateOf(task.dueDate.ifBlank { DateUtils.getCurrentIsoDate() })
        }
        var newDueTime by remember(task) {
            mutableStateOf(task.dueTime.ifBlank { "17:00" })
        }

        AlertDialog(
            onDismissRequest = { taskToUpdateDueDate = null },
            title = { Text("Set Coming Due Date", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Task: ${task.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newDueDate,
                        onValueChange = { newDueDate = it },
                        label = { Text("Coming Due Date (YYYY-MM-DD)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_update_due_date"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDueTime,
                        onValueChange = { newDueTime = it },
                        label = { Text("Time (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTodoTaskDueDate(task.id, newDueDate, newDueTime)
                        taskToUpdateDueDate = null
                    },
                    modifier = Modifier.testTag("btn_save_due_date")
                ) {
                    Text("Save Due Date")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { taskToUpdateDueDate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Task Dialog
    if (taskToDelete != null) {
        val t = taskToDelete!!
        ConfirmDialog(
            title = "Delete Task?",
            message = "Are you sure you want to remove '${t.title}'?",
            confirmButtonText = "Delete",
            onConfirm = {
                viewModel.deleteTodoTask(t)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }
}

@Composable
fun TodoTaskCard(
    task: TodoTask,
    onToggle: (Boolean) -> Unit,
    onSetDueDate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("todo_task_card_${task.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                checked = task.isCompleted,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("checkbox_task_${task.id}")
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Priority Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (task.priority) {
                            "High" -> Color(0xFFFEE2E2)
                            "Medium" -> Color(0xFFFEF3C7)
                            else -> Color(0xFFEFF6FF)
                        }
                    ) {
                        Text(
                            text = when (task.priority) {
                                "High" -> "🔥 High"
                                "Medium" -> "⚡ Medium"
                                else -> "💤 Low"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (task.priority) {
                                "High" -> Color(0xFFDC2626)
                                "Medium" -> Color(0xFFD97706)
                                else -> Color(0xFF2563EB)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Due Date Badge / Add Later Chip
                    if (task.dueDate.isNotBlank()) {
                        val days = DateUtils.getDaysRemaining(task.dueDate)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "📅 Due: ${DateUtils.formatIsoToDisplay(task.dueDate)} ($days d)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        // Due date to be added later
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onSetDueDate() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Due: Add later",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.notes.isNotBlank()) {
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Set / Change Due Date") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onSetDueDate()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete Task", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
