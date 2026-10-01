package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.BackupManager
import com.example.data.repository.CampusMateRepository
import com.example.util.DateUtils
import com.example.util.FileUtils
import com.example.util.NoteFileInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed class Screen {
    data object Home : Screen()
    data object Subjects : Screen()
    data class SubjectDetail(val subjectId: Long, val initialTab: SubjectTab = SubjectTab.ClassNotes) : Screen()
    data object TodoList : Screen()
    data object Alerts : Screen()
    data object Calendar : Screen()
    data object Ideas : Screen()
    data object Settings : Screen()
}

enum class SubjectTab {
    ClassNotes,
    Notices,
    Overview
}

enum class NoteFilter {
    ALL,
    PDF,
    IMAGE
}

class CampusMateViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = CampusMateRepository(application, database)
    private val backupManager = BackupManager(application, database)

    // Navigation stack
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Data streams
    val subjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<Alert>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val events: StateFlow<List<CalendarEvent>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ideas: StateFlow<List<IdeaProblem>> = repository.allIdeas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todoTasks: StateFlow<List<TodoTask>> = repository.allTodoTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently viewed subject state
    private val _selectedSubjectId = MutableStateFlow<Long?>(null)
    val selectedSubjectId: StateFlow<Long?> = _selectedSubjectId.asStateFlow()

    val currentSubject: StateFlow<Subject?> = _selectedSubjectId
        .flatMapLatest { id ->
            if (id != null) repository.getSubjectById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentChapters: StateFlow<List<Chapter>> = _selectedSubjectId
        .flatMapLatest { id ->
            if (id != null) repository.getChaptersForSubject(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentNotes: StateFlow<List<ClassNote>> = _selectedSubjectId
        .flatMapLatest { id ->
            if (id != null) repository.getNotesForSubject(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentNotices: StateFlow<List<Notice>> = _selectedSubjectId
        .flatMapLatest { id ->
            if (id != null) repository.getNoticesForSubject(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSubjectChapterTasks: StateFlow<List<ChapterTask>> = _selectedSubjectId
        .flatMapLatest { id ->
            if (id != null) repository.getChapterTasksForSubject(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Subject Detail UI state
    val subjectTab = MutableStateFlow(SubjectTab.ClassNotes)
    val notesSearchQuery = MutableStateFlow("")
    val notesFilter = MutableStateFlow(NoteFilter.ALL)
    val expandedChapterIds = MutableStateFlow<Set<Long>>(emptySet())

    // Feedback message (Snackbar/Toast)
    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    // Temporary camera capture state
    var pendingCameraFile: File? = null
    var pendingTargetChapterId: Long? = null

    // Pending file picked state (for confirm & save dialog)
    private val _pendingNoteInfo = MutableStateFlow<NoteFileInfo?>(null)
    val pendingNoteInfo: StateFlow<NoteFileInfo?> = _pendingNoteInfo.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.SubjectDetail) {
            _selectedSubjectId.value = screen.subjectId
            subjectTab.value = screen.initialTab
            notesSearchQuery.value = ""
            // Expand all chapters initially for smooth viewing
            viewModelScope.launch {
                val chapters = repository.getChaptersForSubject(screen.subjectId).first()
                expandedChapterIds.value = chapters.map { it.id }.toSet()
            }
        }
    }

    fun navigateBack() {
        when (_currentScreen.value) {
            is Screen.SubjectDetail -> _currentScreen.value = Screen.Subjects
            Screen.Home -> { /* already home */ }
            else -> _currentScreen.value = Screen.Home
        }
    }

    fun toggleChapterExpanded(chapterId: Long) {
        val current = expandedChapterIds.value.toMutableSet()
        if (current.contains(chapterId)) {
            current.remove(chapterId)
        } else {
            current.add(chapterId)
        }
        expandedChapterIds.value = current
    }

    // --- Subject Operations ---
    fun addSubject(name: String, code: String, professor: String, room: String, colorHex: String, schedule: String) {
        viewModelScope.launch {
            val id = repository.insertSubject(
                Subject(
                    name = name.trim(),
                    code = code.trim(),
                    professor = professor.trim(),
                    room = room.trim(),
                    colorHex = colorHex,
                    schedule = schedule.trim()
                )
            )
            // Add default Chapter 1 automatically for convenience
            repository.insertChapter(
                Chapter(
                    subjectId = id,
                    title = "Chapter 1 — Introduction",
                    orderIndex = 1
                )
            )
            _message.emit("Subject created with Chapter 1")
        }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch {
            repository.updateSubject(subject)
            _message.emit("Subject updated")
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            _message.emit("Subject '${subject.name}' deleted")
            if (_selectedSubjectId.value == subject.id) {
                navigateBack()
            }
        }
    }

    // --- Chapter Operations ---
    fun addChapter(subjectId: Long, title: String) {
        viewModelScope.launch {
            val count = currentChapters.value.size
            val newId = repository.insertChapter(
                Chapter(
                    subjectId = subjectId,
                    title = title.trim(),
                    orderIndex = count + 1
                )
            )
            // Expand new chapter
            expandedChapterIds.value = expandedChapterIds.value + newId
            _message.emit("Chapter added")
        }
    }

    fun renameChapter(chapter: Chapter, newTitle: String) {
        viewModelScope.launch {
            repository.updateChapter(chapter.copy(title = newTitle.trim()))
            _message.emit("Chapter renamed")
        }
    }

    fun deleteChapter(chapter: Chapter, deleteNotes: Boolean) {
        viewModelScope.launch {
            repository.deleteChapter(chapter, deleteNotes)
            _message.emit("Chapter '${chapter.title}' deleted")
        }
    }

    // --- Class Note Operations ---
    fun prepareCameraCapture(chapterId: Long?): Pair<Uri, File> {
        val (uri, file) = FileUtils.getTempCameraUri(getApplication())
        pendingCameraFile = file
        pendingTargetChapterId = chapterId
        return Pair(uri, file)
    }

    fun onCameraCaptureSuccess(userTitle: String?, targetChapterId: Long, description: String = "") {
        val tempFile = pendingCameraFile ?: return
        val subjectId = _selectedSubjectId.value ?: return
        viewModelScope.launch {
            val noteInfo = FileUtils.saveCapturedImage(getApplication(), tempFile, userTitle)
            pendingCameraFile = null
            if (noteInfo != null) {
                val finalTitle = if (userTitle.isNullOrBlank()) {
                    "Class Note - ${DateUtils.getCurrentDateFormatted()}.jpg"
                } else {
                    userTitle.trim()
                }

                repository.insertClassNote(
                    ClassNote(
                        subjectId = subjectId,
                        chapterId = targetChapterId,
                        title = finalTitle,
                        fileName = noteInfo.fileName,
                        fileType = "image/jpeg",
                        uploadedDate = DateUtils.getCurrentDateFormatted(),
                        localPath = noteInfo.file.absolutePath,
                        fileSizeBytes = noteInfo.sizeBytes,
                        description = description.trim()
                    )
                )
                _message.emit("Photo note saved successfully")
            } else {
                _message.emit("Failed to process captured image")
            }
        }
    }

    fun preparePickedFile(uri: Uri, preselectedChapterId: Long?) {
        viewModelScope.launch {
            val info = FileUtils.copyUriToInternalStorage(getApplication(), uri)
            if (info != null) {
                pendingTargetChapterId = preselectedChapterId
                _pendingNoteInfo.value = info
            } else {
                _message.emit("Failed to import file")
            }
        }
    }

    fun cancelPendingNote() {
        val info = _pendingNoteInfo.value
        if (info != null) {
            FileUtils.deleteFileQuietly(info.file.absolutePath)
            _pendingNoteInfo.value = null
        }
    }

    fun savePendingNote(title: String, chapterId: Long, description: String = "") {
        val info = _pendingNoteInfo.value ?: return
        val subjectId = _selectedSubjectId.value ?: return
        viewModelScope.launch {
            val finalTitle = if (title.isBlank()) info.originalName else title.trim()
            repository.insertClassNote(
                ClassNote(
                    subjectId = subjectId,
                    chapterId = chapterId,
                    title = finalTitle,
                    fileName = info.fileName,
                    fileType = info.mimeType,
                    uploadedDate = DateUtils.getCurrentDateFormatted(),
                    localPath = info.file.absolutePath,
                    fileSizeBytes = info.sizeBytes,
                    description = description.trim()
                )
            )
            _pendingNoteInfo.value = null
            _message.emit("Note added to chapter")
        }
    }

    fun renameNote(note: ClassNote, newTitle: String, newDescription: String) {
        viewModelScope.launch {
            repository.renameClassNote(note.id, newTitle.trim(), newDescription.trim())
            _message.emit("Note updated")
        }
    }

    fun moveNote(note: ClassNote, newChapterId: Long) {
        viewModelScope.launch {
            repository.moveClassNoteToChapter(note.id, newChapterId)
            _message.emit("Note moved to selected chapter")
        }
    }

    fun deleteNote(note: ClassNote) {
        viewModelScope.launch {
            repository.deleteClassNote(note)
            _message.emit("Note '${note.title}' deleted")
        }
    }

    fun openNote(note: ClassNote) {
        viewModelScope.launch {
            val result = FileUtils.openNoteFile(
                context = getApplication(),
                filePath = note.localPath,
                mimeType = note.fileType
            )
            result.onFailure { error ->
                _message.emit(error.message ?: "Unable to open file")
            }
        }
    }

    // --- Notices Operations ---
    fun addNotice(subjectId: Long, title: String, content: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.insertNotice(
                Notice(
                    subjectId = subjectId,
                    title = title.trim(),
                    content = content.trim(),
                    date = DateUtils.getCurrentIsoDate(),
                    isPinned = isPinned
                )
            )
            _message.emit("Notice posted")
        }
    }

    fun deleteNotice(notice: Notice) {
        viewModelScope.launch {
            repository.deleteNotice(notice)
            _message.emit("Notice deleted")
        }
    }

    // --- Alerts Operations ---
    fun addAlert(title: String, subjectName: String, dueDate: String, dueTime: String, type: String, priority: String, notes: String) {
        viewModelScope.launch {
            repository.insertAlert(
                Alert(
                    title = title.trim(),
                    subjectName = subjectName.trim(),
                    dueDate = dueDate.trim(),
                    dueTime = dueTime.trim(),
                    type = type,
                    priority = priority,
                    notes = notes.trim()
                )
            )
            _message.emit("Alert / Deadline saved")
        }
    }

    fun toggleAlert(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleAlertCompleted(id, completed)
        }
    }

    fun deleteAlert(alert: Alert) {
        viewModelScope.launch {
            repository.deleteAlert(alert)
            _message.emit("Alert removed")
        }
    }

    // --- Calendar Event Operations ---
    fun addEvent(title: String, targetDate: String, category: String, notes: String) {
        viewModelScope.launch {
            repository.insertCalendarEvent(
                CalendarEvent(
                    title = title.trim(),
                    targetDate = targetDate.trim(),
                    category = category,
                    notes = notes.trim()
                )
            )
            _message.emit("Countdown event added")
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(event)
            _message.emit("Event removed")
        }
    }

    // --- Ideas & Problems Operations ---
    fun addIdea(title: String, description: String, subjectName: String, type: String) {
        viewModelScope.launch {
            repository.insertIdea(
                IdeaProblem(
                    title = title.trim(),
                    description = description.trim(),
                    subjectName = subjectName.trim(),
                    type = type
                )
            )
            _message.emit("$type logged")
        }
    }

    fun toggleIdeaResolved(id: Long, resolved: Boolean) {
        viewModelScope.launch {
            repository.toggleIdeaResolved(id, resolved)
        }
    }

    fun deleteIdea(idea: IdeaProblem) {
        viewModelScope.launch {
            repository.deleteIdea(idea)
            _message.emit("${idea.type} removed")
        }
    }

    // --- Regular To-Do Task Operations ---
    fun addTodoTask(title: String, priority: String, dueDate: String, dueTime: String, category: String, notes: String) {
        viewModelScope.launch {
            repository.insertTodoTask(
                TodoTask(
                    title = title.trim(),
                    priority = priority,
                    dueDate = dueDate.trim(),
                    dueTime = dueTime.trim(),
                    category = category.trim(),
                    notes = notes.trim()
                )
            )
            val dueInfo = if (dueDate.isNotBlank()) " (Due: $dueDate)" else " (Due date to be added later)"
            _message.emit("Task added with $priority priority$dueInfo")
        }
    }

    fun toggleTodoTask(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleTodoTask(id, completed)
        }
    }

    fun updateTodoTaskDueDate(id: Long, dueDate: String, dueTime: String) {
        viewModelScope.launch {
            repository.updateTodoTaskDueDate(id, dueDate.trim(), dueTime.trim())
            _message.emit("Due date updated")
        }
    }

    fun deleteTodoTask(task: TodoTask) {
        viewModelScope.launch {
            repository.deleteTodoTask(task)
            _message.emit("Task '${task.title}' deleted")
        }
    }

    // --- Chapter-Specific Task Operations ---
    fun addChapterTask(subjectId: Long, chapterId: Long, title: String, priority: String, dueDate: String, notes: String = "") {
        viewModelScope.launch {
            repository.insertChapterTask(
                ChapterTask(
                    subjectId = subjectId,
                    chapterId = chapterId,
                    title = title.trim(),
                    priority = priority,
                    dueDate = dueDate.trim(),
                    notes = notes.trim()
                )
            )
            _message.emit("Chapter task added")
        }
    }

    fun toggleChapterTask(id: Long, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleChapterTask(id, completed)
        }
    }

    fun updateChapterTaskDueDate(id: Long, dueDate: String) {
        viewModelScope.launch {
            repository.updateChapterTaskDueDate(id, dueDate.trim())
            _message.emit("Chapter task due date updated")
        }
    }

    fun deleteChapterTask(task: ChapterTask) {
        viewModelScope.launch {
            repository.deleteChapterTask(task)
            _message.emit("Chapter task removed")
        }
    }

    // --- Backup & Restore ---
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _message.emit("Preparing full backup (including notes & photos)...")
            val result = backupManager.exportBackup(uri)
            result.onSuccess { count ->
                _message.emit("Export completed! Packaged $count class note files.")
            }.onFailure { error ->
                _message.emit("Export failed: ${error.localizedMessage}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _message.emit("Importing CampusMate backup archive...")
            val result = backupManager.importBackup(uri)
            result.onSuccess { msg ->
                _message.emit(msg)
            }.onFailure { error ->
                _message.emit("Import failed: ${error.localizedMessage}")
            }
        }
    }
}
