package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Subject
import com.example.ui.components.ColorPickerRow
import com.example.ui.components.ConfirmDialog
import com.example.ui.viewmodel.CampusMateViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.SubjectTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    viewModel: CampusMateViewModel,
    onNavigate: (Screen) -> Unit
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Subjects & Courses",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("top_bar_add_subject_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Subject", fontSize = 13.sp)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Subject") },
                modifier = Modifier.testTag("fab_add_subject")
            )
        }
    ) { innerPadding ->
        if (subjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Subjects Yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add your academic courses to organize chapter notes, PDFs, whiteboard photos, and exam notices.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("btn_empty_add_subject")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Subject")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("subjects_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(subjects, key = { it.id }) { subject ->
                    SubjectListItem(
                        subject = subject,
                        onClick = {
                            onNavigate(Screen.SubjectDetail(subject.id, SubjectTab.ClassNotes))
                        },
                        onEdit = { subjectToEdit = subject },
                        onDelete = { subjectToDelete = subject }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // Add Subject Dialog
    if (showAddDialog) {
        SubjectDialog(
            title = "Add Subject",
            confirmText = "Create Subject",
            initialName = "",
            initialCode = "",
            initialProfessor = "",
            initialRoom = "",
            initialSchedule = "",
            initialColor = "#2563EB",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, code, prof, room, color, schedule ->
                viewModel.addSubject(name, code, prof, room, color, schedule)
                showAddDialog = false
            }
        )
    }

    // Edit Subject Dialog
    if (subjectToEdit != null) {
        val s = subjectToEdit!!
        SubjectDialog(
            title = "Edit Subject",
            confirmText = "Save Changes",
            initialName = s.name,
            initialCode = s.code,
            initialProfessor = s.professor,
            initialRoom = s.room,
            initialSchedule = s.schedule,
            initialColor = s.colorHex,
            onDismiss = { subjectToEdit = null },
            onConfirm = { name, code, prof, room, color, schedule ->
                viewModel.updateSubject(
                    s.copy(
                        name = name,
                        code = code,
                        professor = prof,
                        room = room,
                        colorHex = color,
                        schedule = schedule
                    )
                )
                subjectToEdit = null
            }
        )
    }

    // Delete Subject Confirmation Dialog
    if (subjectToDelete != null) {
        val s = subjectToDelete!!
        ConfirmDialog(
            title = "Delete Subject?",
            message = "Are you sure you want to delete '${s.name}'? All its chapters, notes, files, and notices will be deleted permanently.",
            confirmButtonText = "Delete Subject",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteSubject(s)
                subjectToDelete = null
            },
            onDismiss = { subjectToDelete = null }
        )
    }
}

@Composable
fun SubjectListItem(
    subject: Subject,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val color = try {
        Color(android.graphics.Color.parseColor(subject.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("subject_item_${subject.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (subject.code.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = subject.code,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (subject.professor.isNotBlank()) {
                    Text(
                        text = subject.professor,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (subject.schedule.isNotBlank() || subject.room.isNotBlank()) {
                    Text(
                        text = listOfNotNull(subject.schedule.ifBlank { null }, subject.room.ifBlank { null }).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Subject options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open Class Notes") },
                        leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Subject") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete Subject", color = MaterialTheme.colorScheme.error) },
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

@Composable
fun SubjectDialog(
    title: String,
    confirmText: String,
    initialName: String,
    initialCode: String,
    initialProfessor: String,
    initialRoom: String,
    initialSchedule: String,
    initialColor: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, prof: String, room: String, color: String, schedule: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var code by remember { mutableStateOf(initialCode) }
    var prof by remember { mutableStateOf(initialProfessor) }
    var room by remember { mutableStateOf(initialRoom) }
    var schedule by remember { mutableStateOf(initialSchedule) }
    var colorHex by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name *") },
                    placeholder = { Text("e.g. Operating Systems") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_subject_name"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course Code") },
                    placeholder = { Text("e.g. CS-301") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = prof,
                    onValueChange = { prof = it },
                    label = { Text("Professor / Lecturer") },
                    placeholder = { Text("e.g. Dr. Alan Turing") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("Schedule") },
                    placeholder = { Text("e.g. Mon/Wed 10:00 AM") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Hall") },
                    placeholder = { Text("e.g. Hall B-204") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Subject Color Badge:", style = MaterialTheme.typography.labelMedium)
                ColorPickerRow(
                    selectedColorHex = colorHex,
                    onColorSelected = { colorHex = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, code, prof, room, colorHex, schedule)
                    }
                },
                modifier = Modifier.testTag("btn_confirm_subject")
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
