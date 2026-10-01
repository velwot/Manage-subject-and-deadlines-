package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Chapter
import com.example.data.model.ChapterTask
import com.example.data.model.ClassNote
import com.example.data.model.Notice
import com.example.data.model.Subject
import com.example.ui.components.ConfirmDialog
import com.example.ui.viewmodel.CampusMateViewModel
import com.example.ui.viewmodel.NoteFilter
import com.example.ui.viewmodel.SubjectTab
import com.example.util.DateUtils
import com.example.util.FileUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    viewModel: CampusMateViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val subject by viewModel.currentSubject.collectAsStateWithLifecycle()
    val chapters by viewModel.currentChapters.collectAsStateWithLifecycle()
    val notes by viewModel.currentNotes.collectAsStateWithLifecycle()
    val notices by viewModel.currentNotices.collectAsStateWithLifecycle()
    val chapterTasks by viewModel.currentSubjectChapterTasks.collectAsStateWithLifecycle()

    val currentTab by viewModel.subjectTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.notesSearchQuery.collectAsStateWithLifecycle()
    val noteFilter by viewModel.notesFilter.collectAsStateWithLifecycle()
    val expandedChapters by viewModel.expandedChapterIds.collectAsStateWithLifecycle()

    val pendingNoteInfo by viewModel.pendingNoteInfo.collectAsStateWithLifecycle()

    // Dialog & UI states
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var chapterToRename by remember { mutableStateOf<Chapter?>(null) }
    var chapterToDelete by remember { mutableStateOf<Chapter?>(null) }

    var showAddNotePickerSheet by remember { mutableStateOf(false) }
    var noteChapterTargetId by remember { mutableStateOf<Long?>(null) }

    var noteToRename by remember { mutableStateOf<ClassNote?>(null) }
    var noteToMove by remember { mutableStateOf<ClassNote?>(null) }
    var noteToDelete by remember { mutableStateOf<ClassNote?>(null) }

    var showAddNoticeDialog by remember { mutableStateOf(false) }
    var noticeToDelete by remember { mutableStateOf<Notice?>(null) }

    // Chapter Task Dialog states
    var chapterTaskChapterId by remember { mutableStateOf<Long?>(null) }
    var chapterTaskToUpdateDueDate by remember { mutableStateOf<ChapterTask?>(null) }
    var chapterTaskToDelete by remember { mutableStateOf<ChapterTask?>(null) }

    // Camera capture state
    var showCameraSaveDialog by remember { mutableStateOf(false) }
    var capturedPhotoTitle by remember { mutableStateOf("") }
    var capturedPhotoDescription by remember { mutableStateOf("") }
    var capturedPhotoChapterId by remember { mutableStateOf<Long?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val defaultTitle = "Class Note - ${DateUtils.getCurrentDateFormatted()}.jpg"
            capturedPhotoTitle = defaultTitle
            capturedPhotoDescription = ""
            capturedPhotoChapterId = noteChapterTargetId ?: chapters.firstOrNull()?.id
            showCameraSaveDialog = true
        } else {
            viewModel.pendingCameraFile?.delete()
            viewModel.pendingCameraFile = null
            Toast.makeText(context, "Camera capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val (uri, _) = viewModel.prepareCameraCapture(noteChapterTargetId)
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required to capture class notes", Toast.LENGTH_LONG).show()
        }
    }

    // PDF picker launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.preparePickedFile(uri, noteChapterTargetId)
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.preparePickedFile(uri, noteChapterTargetId)
        }
    }

    val subjectColor = remember(subject?.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(subject?.colorHex ?: "#2563EB"))
        } catch (e: Exception) {
            Color(0xFF2563EB)
        }
    }

    if (subject == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(subjectColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = subject!!.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (subject!!.code.isNotBlank() || subject!!.professor.isNotBlank()) {
                            Text(
                                text = listOfNotNull(
                                    subject!!.code.ifBlank { null },
                                    subject!!.professor.ifBlank { null }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("subject_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            when (currentTab) {
                SubjectTab.ClassNotes -> {
                    ExtendedFloatingActionButton(
                        onClick = {
                            noteChapterTargetId = chapters.firstOrNull()?.id
                            showAddNotePickerSheet = true
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Add Class Note") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_class_note")
                    )
                }
                SubjectTab.Notices -> {
                    FloatingActionButton(
                        onClick = { showAddNoticeDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_notice")
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = "Add Notice")
                    }
                }
                else -> {}
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // M3 Primary Tab Row: "Class Notes" and "Notices" and "Overview"
            PrimaryTabRow(
                selectedTabIndex = currentTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = currentTab == SubjectTab.ClassNotes,
                    onClick = { viewModel.subjectTab.value = SubjectTab.ClassNotes },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Class Notes (${notes.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_class_notes")
                )
                Tab(
                    selected = currentTab == SubjectTab.Notices,
                    onClick = { viewModel.subjectTab.value = SubjectTab.Notices },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Notices (${notices.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_notices")
                )
                Tab(
                    selected = currentTab == SubjectTab.Overview,
                    onClick = { viewModel.subjectTab.value = SubjectTab.Overview },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Overview")
                        }
                    },
                    modifier = Modifier.testTag("tab_overview")
                )
            }

            when (currentTab) {
                SubjectTab.ClassNotes -> {
                    ClassNotesSection(
                        subject = subject!!,
                        chapters = chapters,
                        notes = notes,
                        chapterTasks = chapterTasks,
                        searchQuery = searchQuery,
                        noteFilter = noteFilter,
                        expandedChapters = expandedChapters,
                        onSearchChange = { viewModel.notesSearchQuery.value = it },
                        onFilterChange = { viewModel.notesFilter.value = it },
                        onToggleExpand = { viewModel.toggleChapterExpanded(it) },
                        onAddChapterClick = { showAddChapterDialog = true },
                        onAddNoteClickForChapter = { chId ->
                            noteChapterTargetId = chId
                            showAddNotePickerSheet = true
                        },
                        onRenameChapter = { chapterToRename = it },
                        onDeleteChapter = { chapterToDelete = it },
                        onOpenNote = { viewModel.openNote(it) },
                        onRenameNote = { noteToRename = it },
                        onMoveNote = { noteToMove = it },
                        onDeleteNote = { noteToDelete = it },
                        onAddChapterTaskClick = { chId -> chapterTaskChapterId = chId },
                        onToggleChapterTask = { taskId, completed -> viewModel.toggleChapterTask(taskId, completed) },
                        onSetChapterTaskDueDate = { task -> chapterTaskToUpdateDueDate = task },
                        onDeleteChapterTask = { task -> chapterTaskToDelete = task }
                    )
                }
                SubjectTab.Notices -> {
                    NoticesSection(
                        notices = notices,
                        onDeleteNotice = { noticeToDelete = it }
                    )
                }
                SubjectTab.Overview -> {
                    OverviewSection(subject = subject!!)
                }
            }
        }
    }

    // --- DIALOGS & BOTTOM SHEETS ---

    // 1. Add Note Picker Options (Camera, Gallery Image, PDF Document)
    if (showAddNotePickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddNotePickerSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Add Class Note",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                val targetChapter = chapters.find { it.id == noteChapterTargetId }
                if (targetChapter != null) {
                    Text(
                        text = "Adding to: ${targetChapter.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                } else {
                    Text(
                        text = "Choose a source to add your class notes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                }

                // Option 1: 📷 Camera (Take Photo)
                Card(
                    onClick = {
                        showAddNotePickerSheet = false
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            val (uri, _) = viewModel.prepareCameraCapture(noteChapterTargetId)
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("btn_choose_camera"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "📷 Take Photo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Capture blackboard, whiteboard, or paper notes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 2: 🖼️ Choose Image (Gallery)
                Card(
                    onClick = {
                        showAddNotePickerSheet = false
                        imagePickerLauncher.launch("image/*")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("btn_choose_image"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "🖼️ Choose Image",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Select diagrams, photos or screenshots (JPG, PNG)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 3: 📄 Choose PDF / Document
                Card(
                    onClick = {
                        showAddNotePickerSheet = false
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("btn_choose_pdf"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "📄 Choose PDF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Select lecture slides, handouts, or textbook chapters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // 2. Camera photo save confirmation dialog
    if (showCameraSaveDialog) {
        var selectedChapterId by remember {
            mutableStateOf(capturedPhotoChapterId ?: chapters.firstOrNull()?.id ?: 0L)
        }
        AlertDialog(
            onDismissRequest = {
                showCameraSaveDialog = false
                viewModel.pendingCameraFile?.delete()
                viewModel.pendingCameraFile = null
            },
            title = {
                Text(
                    text = "Save Captured Class Note",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Captured image successfully! Set details below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = capturedPhotoTitle,
                        onValueChange = { capturedPhotoTitle = it },
                        label = { Text("Note Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_camera_note_title"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chapter selection dropdown / options
                    Text(
                        text = "Assign to Chapter:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (chapters.isEmpty()) {
                        Text(
                            text = "No chapters found. A new chapter will be created.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        chapters.forEach { chapter ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedChapterId = chapter.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedChapterId == chapter.id,
                                    onClick = { selectedChapterId = chapter.id }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = capturedPhotoDescription,
                        onValueChange = { capturedPhotoDescription = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_camera_note_desc"),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = if (selectedChapterId != 0L) {
                            selectedChapterId
                        } else {
                            chapters.firstOrNull()?.id ?: 0L
                        }
                        if (targetId == 0L) {
                            // Create chapter first if none exists
                            viewModel.addChapter(subject!!.id, "Chapter 1 — Introduction")
                        }
                        val finalChapterId = if (targetId != 0L) targetId else 1L
                        viewModel.onCameraCaptureSuccess(
                            userTitle = capturedPhotoTitle,
                            targetChapterId = finalChapterId,
                            description = capturedPhotoDescription
                        )
                        showCameraSaveDialog = false
                    },
                    modifier = Modifier.testTag("btn_save_camera_note")
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showCameraSaveDialog = false
                        viewModel.pendingCameraFile?.delete()
                        viewModel.pendingCameraFile = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Picked File Save Confirmation Dialog (for imported PDF or Gallery image)
    if (pendingNoteInfo != null) {
        val info = pendingNoteInfo!!
        var noteTitle by remember(info) { mutableStateOf(info.originalName) }
        var noteDescription by remember { mutableStateOf("") }
        var selectedChapterId by remember(info) {
            mutableStateOf(noteChapterTargetId ?: chapters.firstOrNull()?.id ?: 0L)
        }

        AlertDialog(
            onDismissRequest = { viewModel.cancelPendingNote() },
            title = {
                Text(
                    text = if (info.mimeType.contains("pdf")) "Add PDF Class Note" else "Add Image Class Note",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "File: ${info.originalName} (${FileUtils.formatFileSize(info.sizeBytes)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Note Title / File Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_picked_note_title"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Assign to Chapter:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (chapters.isEmpty()) {
                        Text(
                            text = "Please create a chapter first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        chapters.forEach { chapter ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedChapterId = chapter.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedChapterId == chapter.id,
                                    onClick = { selectedChapterId = chapter.id }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = noteDescription,
                        onValueChange = { noteDescription = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_picked_note_desc"),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chId = if (selectedChapterId != 0L) selectedChapterId else chapters.firstOrNull()?.id ?: 0L
                        if (chId != 0L) {
                            viewModel.savePendingNote(noteTitle, chId, noteDescription)
                        } else {
                            Toast.makeText(context, "Please select or create a chapter first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("btn_save_picked_note")
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.cancelPendingNote() }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Add Chapter Dialog
    if (showAddChapterDialog) {
        var chapterTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddChapterDialog = false },
            title = { Text("Add Chapter", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter chapter name (e.g., Chapter 1 — Introduction)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = chapterTitle,
                        onValueChange = { chapterTitle = it },
                        label = { Text("Chapter Title") },
                        placeholder = { Text("Chapter ${chapters.size + 1} — Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_chapter_title"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (chapterTitle.isNotBlank()) {
                            viewModel.addChapter(subject!!.id, chapterTitle)
                            showAddChapterDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_add_chapter")
                ) {
                    Text("Add Chapter")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddChapterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Rename Chapter Dialog
    if (chapterToRename != null) {
        val chapter = chapterToRename!!
        var newTitle by remember(chapter) { mutableStateOf(chapter.title) }
        AlertDialog(
            onDismissRequest = { chapterToRename = null },
            title = { Text("Rename Chapter", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Chapter Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_rename_chapter"),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.renameChapter(chapter, newTitle)
                            chapterToRename = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { chapterToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 6. Delete Chapter Confirmation Dialog
    if (chapterToDelete != null) {
        val chapter = chapterToDelete!!
        val notesInChapter = notes.count { it.chapterId == chapter.id }
        ConfirmDialog(
            title = "Delete Chapter?",
            message = "Do you want to delete chapter '${chapter.title}' and its $notesInChapter notes? This action cannot be undone.",
            confirmButtonText = "Delete Chapter & Notes",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteChapter(chapter, deleteNotes = true)
                chapterToDelete = null
            },
            onDismiss = { chapterToDelete = null }
        )
    }

    // 7. Rename Note Dialog
    if (noteToRename != null) {
        val note = noteToRename!!
        var noteTitle by remember(note) { mutableStateOf(note.title) }
        var noteDesc by remember(note) { mutableStateOf(note.description) }

        AlertDialog(
            onDismissRequest = { noteToRename = null },
            title = { Text("Rename Class Note", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Note Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_rename_note_title"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = noteDesc,
                        onValueChange = { noteDesc = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_rename_note_desc"),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteTitle.isNotBlank()) {
                            viewModel.renameNote(note, noteTitle, noteDesc)
                            noteToRename = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { noteToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 8. Move Note Dialog
    if (noteToMove != null) {
        val note = noteToMove!!
        var targetChapterId by remember(note) { mutableStateOf(note.chapterId) }

        AlertDialog(
            onDismissRequest = { noteToMove = null },
            title = { Text("Move Note to Chapter", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Select chapter within '${subject!!.name}':",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    chapters.forEach { chapter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { targetChapterId = chapter.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = targetChapterId == chapter.id,
                                onClick = { targetChapterId = chapter.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moveNote(note, targetChapterId)
                        noteToMove = null
                    }
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { noteToMove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 9. Delete Note Confirmation Dialog
    if (noteToDelete != null) {
        val note = noteToDelete!!
        ConfirmDialog(
            title = "Delete Class Note?",
            message = "Are you sure you want to delete '${note.title}'? The file will be removed from your device.",
            confirmButtonText = "Delete Note",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteNote(note)
                noteToDelete = null
            },
            onDismiss = { noteToDelete = null }
        )
    }

    // 10. Add Notice Dialog
    if (showAddNoticeDialog) {
        var noticeTitle by remember { mutableStateOf("") }
        var noticeContent by remember { mutableStateOf("") }
        var isPinned by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddNoticeDialog = false },
            title = { Text("Post Subject Notice", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = noticeTitle,
                        onValueChange = { noticeTitle = it },
                        label = { Text("Notice Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_notice_title"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = noticeContent,
                        onValueChange = { noticeContent = it },
                        label = { Text("Announcement Details") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_notice_content"),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isPinned,
                            onCheckedChange = { isPinned = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pin notice to top")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noticeTitle.isNotBlank()) {
                            viewModel.addNotice(subject!!.id, noticeTitle, noticeContent, isPinned)
                            showAddNoticeDialog = false
                        }
                    }
                ) {
                    Text("Post")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddNoticeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 11. Delete Notice Confirmation Dialog
    if (noticeToDelete != null) {
        val notice = noticeToDelete!!
        ConfirmDialog(
            title = "Delete Notice?",
            message = "Are you sure you want to delete notice '${notice.title}'?",
            confirmButtonText = "Delete Notice",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteNotice(notice)
                noticeToDelete = null
            },
            onDismiss = { noticeToDelete = null }
        )
    }

    // 12. Add Chapter Task Dialog
    if (chapterTaskChapterId != null) {
        val targetChId = chapterTaskChapterId!!
        val chapterObj = chapters.find { it.id == targetChId }
        var taskTitle by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("Medium") }
        var addDueDateLater by remember { mutableStateOf(true) }
        var dueDate by remember { mutableStateOf(DateUtils.getCurrentIsoDate()) }
        var taskNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { chapterTaskChapterId = null },
            title = {
                Column {
                    Text("Add Chapter Task", fontWeight = FontWeight.Bold)
                    if (chapterObj != null) {
                        Text(
                            text = chapterObj.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Chapter Task Title *") },
                        placeholder = { Text("e.g. Read textbook pages 45–60") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_chapter_task_title"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))
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
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Due Date", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = if (addDueDateLater) "Add due date later" else "Set due date now",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = !addDueDateLater,
                            onCheckedChange = { addDueDateLater = !it }
                        )
                    }

                    if (!addDueDateLater) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Coming Due Date (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = taskNotes,
                        onValueChange = { taskNotes = it },
                        label = { Text("Notes / Instructions (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            val finalDueDate = if (addDueDateLater) "" else dueDate
                            viewModel.addChapterTask(
                                subjectId = subject!!.id,
                                chapterId = targetChId,
                                title = taskTitle,
                                priority = priority,
                                dueDate = finalDueDate,
                                notes = taskNotes
                            )
                            chapterTaskChapterId = null
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_add_chapter_task")
                ) {
                    Text("Add Task")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { chapterTaskChapterId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 13. Update Chapter Task Due Date Dialog
    if (chapterTaskToUpdateDueDate != null) {
        val task = chapterTaskToUpdateDueDate!!
        var newDueDate by remember(task) {
            mutableStateOf(task.dueDate.ifBlank { DateUtils.getCurrentIsoDate() })
        }

        AlertDialog(
            onDismissRequest = { chapterTaskToUpdateDueDate = null },
            title = { Text("Set Due Date for Chapter Task", fontWeight = FontWeight.Bold) },
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
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateChapterTaskDueDate(task.id, newDueDate)
                        chapterTaskToUpdateDueDate = null
                    }
                ) {
                    Text("Save Due Date")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { chapterTaskToUpdateDueDate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 14. Delete Chapter Task Dialog
    if (chapterTaskToDelete != null) {
        val task = chapterTaskToDelete!!
        ConfirmDialog(
            title = "Delete Chapter Task?",
            message = "Are you sure you want to remove '${task.title}'?",
            confirmButtonText = "Delete",
            onConfirm = {
                viewModel.deleteChapterTask(task)
                chapterTaskToDelete = null
            },
            onDismiss = { chapterTaskToDelete = null }
        )
    }
}

@Composable
fun ClassNotesSection(
    subject: Subject,
    chapters: List<Chapter>,
    notes: List<ClassNote>,
    chapterTasks: List<ChapterTask>,
    searchQuery: String,
    noteFilter: NoteFilter,
    expandedChapters: Set<Long>,
    onSearchChange: (String) -> Unit,
    onFilterChange: (NoteFilter) -> Unit,
    onToggleExpand: (Long) -> Unit,
    onAddChapterClick: () -> Unit,
    onAddNoteClickForChapter: (Long) -> Unit,
    onRenameChapter: (Chapter) -> Unit,
    onDeleteChapter: (Chapter) -> Unit,
    onOpenNote: (ClassNote) -> Unit,
    onRenameNote: (ClassNote) -> Unit,
    onMoveNote: (ClassNote) -> Unit,
    onDeleteNote: (ClassNote) -> Unit,
    onAddChapterTaskClick: (Long) -> Unit,
    onToggleChapterTask: (Long, Boolean) -> Unit,
    onSetChapterTaskDueDate: (ChapterTask) -> Unit,
    onDeleteChapterTask: (ChapterTask) -> Unit
) {
    // Filter notes according to search and filter chips
    val filteredNotes = remember(notes, searchQuery, noteFilter) {
        notes.filter { note ->
            val matchesSearch = searchQuery.isBlank() ||
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.description.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (noteFilter) {
                NoteFilter.ALL -> true
                NoteFilter.PDF -> FileUtils.isPdf(note.fileType, note.fileName)
                NoteFilter.IMAGE -> FileUtils.isImage(note.fileType, note.fileName)
            }

            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("class_notes_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search & Filter header
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_search_bar"),
                placeholder = { Text("Search notes in ${subject.name}...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips + Add Chapter row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = noteFilter == NoteFilter.ALL,
                        onClick = { onFilterChange(NoteFilter.ALL) },
                        label = { Text("All (${notes.size})") }
                    )
                    FilterChip(
                        selected = noteFilter == NoteFilter.PDF,
                        onClick = { onFilterChange(NoteFilter.PDF) },
                        label = {
                            Text("PDFs (${notes.count { FileUtils.isPdf(it.fileType, it.fileName) }})")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    FilterChip(
                        selected = noteFilter == NoteFilter.IMAGE,
                        onClick = { onFilterChange(NoteFilter.IMAGE) },
                        label = {
                            Text("Photos (${notes.count { FileUtils.isImage(it.fileType, it.fileName) }})")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                FilledTonalButton(
                    onClick = onAddChapterClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_add_chapter")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chapter", fontSize = 13.sp)
                }
            }
        }

        // If no chapters exist
        if (chapters.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Chapters Created",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Organize your lecture notes, handouts, and blackboard photos chapter by chapter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddChapterClick,
                            modifier = Modifier.testTag("empty_add_chapter_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Chapter 1")
                        }
                    }
                }
            }
        } else {
            // Chapter-wise expandable cards
            items(chapters, key = { "chapter_${it.id}" }) { chapter ->
                val chapterNotes = filteredNotes.filter { it.chapterId == chapter.id }
                val tasksForChapter = chapterTasks.filter { it.chapterId == chapter.id }
                val isExpanded = expandedChapters.contains(chapter.id)

                ChapterCard(
                    chapter = chapter,
                    notes = chapterNotes,
                    chapterTasks = tasksForChapter,
                    allNotesCount = notes.count { it.chapterId == chapter.id },
                    isExpanded = isExpanded,
                    onToggleExpand = { onToggleExpand(chapter.id) },
                    onAddNote = { onAddNoteClickForChapter(chapter.id) },
                    onRename = { onRenameChapter(chapter) },
                    onDelete = { onDeleteChapter(chapter) },
                    onOpenNote = onOpenNote,
                    onRenameNote = onRenameNote,
                    onMoveNote = onMoveNote,
                    onDeleteNote = onDeleteNote,
                    onAddChapterTask = { onAddChapterTaskClick(chapter.id) },
                    onToggleChapterTask = onToggleChapterTask,
                    onSetChapterTaskDueDate = onSetChapterTaskDueDate,
                    onDeleteChapterTask = onDeleteChapterTask
                )
            }
        }

        // Bottom space so FAB doesn't obscure content
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ChapterCard(
    chapter: Chapter,
    notes: List<ClassNote>,
    chapterTasks: List<ChapterTask>,
    allNotesCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddNote: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onOpenNote: (ClassNote) -> Unit,
    onRenameNote: (ClassNote) -> Unit,
    onMoveNote: (ClassNote) -> Unit,
    onDeleteNote: (ClassNote) -> Unit,
    onAddChapterTask: () -> Unit,
    onToggleChapterTask: (Long, Boolean) -> Unit,
    onSetChapterTaskDueDate: (ChapterTask) -> Unit,
    onDeleteChapterTask: (ChapterTask) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Notes, 1 = Chapter To-Do

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chapter_card_${chapter.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Chapter Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand / Collapse Chevron
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$allNotesCount note${if (allNotesCount != 1) "s" else ""} • ✓ ${chapterTasks.count { it.isCompleted }}/${chapterTasks.size} tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Add note quick action button
                IconButton(
                    onClick = onAddNote,
                    modifier = Modifier.testTag("chapter_add_note_btn_${chapter.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = "Add Note to ${chapter.title}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Chapter options menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("chapter_menu_btn_${chapter.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Chapter options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add Class Note") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onAddNote()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Chapter Task") },
                            leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onAddChapterTask()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename Chapter") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Chapter", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Expandable Content (Notes & Chapter-Specific To-Do List)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Segmented Filter Chips for Notes vs Chapter To-Do
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            label = { Text("📄 Notes (${notes.size})") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            label = { Text("✅ Chapter To-Do (${chapterTasks.count { it.isCompleted }}/${chapterTasks.size})") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (selectedTab == 0) {
                        // Section: Class Notes
                        if (notes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No notes in this chapter yet. Tap + to add PDF or photo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            notes.forEach { note ->
                                ClassNoteItem(
                                    note = note,
                                    chapterTitle = chapter.title,
                                    onOpen = { onOpenNote(note) },
                                    onRename = { onRenameNote(note) },
                                    onMove = { onMoveNote(note) },
                                    onDelete = { onDeleteNote(note) }
                                )
                            }
                        }
                    } else {
                        // Section: Chapter-Specific To-Do List
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chapter Checklist",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FilledTonalButton(
                                onClick = onAddChapterTask,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_add_chapter_task_${chapter.id}")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Task", fontSize = 12.sp)
                            }
                        }

                        if (chapterTasks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No tasks for ${chapter.title} yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = onAddChapterTask,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Chapter Study Task", fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            chapterTasks.forEach { task ->
                                ChapterTaskItem(
                                    task = task,
                                    onToggle = { onToggleChapterTask(task.id, it) },
                                    onSetDueDate = { onSetChapterTaskDueDate(task) },
                                    onDelete = { onDeleteChapterTask(task) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterTaskItem(
    task: ChapterTask,
    onToggle: (Boolean) -> Unit,
    onSetDueDate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chapter_task_${task.id}"),
        shape = RoundedCornerShape(10.dp),
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
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onToggle,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (task.priority) {
                            "High" -> Color(0xFFFEE2E2)
                            "Medium" -> Color(0xFFFEF3C7)
                            else -> Color(0xFFEFF6FF)
                        }
                    ) {
                        Text(
                            text = when (task.priority) {
                                "High" -> "🔥 High"
                                "Medium" -> "⚡ Med"
                                else -> "💤 Low"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (task.priority) {
                                "High" -> Color(0xFFDC2626)
                                "Medium" -> Color(0xFFD97706)
                                else -> Color(0xFF2563EB)
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 10.sp
                        )
                    }

                    if (task.dueDate.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "📅 Due: ${DateUtils.formatIsoToDisplay(task.dueDate)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 10.sp
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onSetDueDate() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Due: Add later",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                if (task.notes.isNotBlank()) {
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
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

@Composable
fun ClassNoteItem(
    note: ClassNote,
    chapterTitle: String,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val isPdf = FileUtils.isPdf(note.fileType, note.fileName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("note_item_${note.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Preview: Image Thumbnail OR PDF Icon
            if (isPdf) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEE2E2), // Soft red background
                    modifier = Modifier.size(54.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF File",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "PDF",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            } else {
                // Image Thumbnail with Coil
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val file = remember(note.localPath) { File(note.localPath) }
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(file)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Photo note thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Note Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle: e.g. "Chapter 2 • PDF • Uploaded: 11 Sep 2026"
                val typeLabel = if (isPdf) "PDF" else "JPG/Image"
                val metaText = "$chapterTitle • $typeLabel"
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Uploaded: ${note.uploadedDate.ifBlank { "Recently" }}${if (note.fileSizeBytes > 0) " • ${FileUtils.formatFileSize(note.fileSizeBytes)}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                if (note.description.isNotBlank()) {
                    Text(
                        text = note.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                }
            }

            // Note Actions Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("note_menu_${note.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Note actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open Note") },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onOpen()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to Chapter") },
                        leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onMove()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
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
fun NoticesSection(
    notices: List<Notice>,
    onDeleteNotice: (Notice) -> Unit
) {
    if (notices.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Notices Posted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Post exam announcements, lab guidelines, and professor notices here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(notices, key = { "notice_${it.id}" }) { notice ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notice.isPinned) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (notice.isPinned) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = notice.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onDeleteNotice(notice) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete Notice",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = notice.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Date: ${DateUtils.formatIsoToDisplay(notice.date)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun OverviewSection(subject: Subject) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Subject Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                DetailItem(label = "Course Code", value = subject.code.ifBlank { "Not specified" })
                DetailItem(label = "Professor / Instructor", value = subject.professor.ifBlank { "Not specified" })
                DetailItem(label = "Classroom / Room", value = subject.room.ifBlank { "Not specified" })
                DetailItem(label = "Schedule", value = subject.schedule.ifBlank { "Flexible" })
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100% Offline & Private",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "All class notes, PDFs, and whiteboard photos are saved inside CampusMate private storage. Back up your notes anytime from the Settings tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
