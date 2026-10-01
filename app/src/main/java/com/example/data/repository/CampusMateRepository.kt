package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.util.DateUtils
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class CampusMateRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    val allSubjects: Flow<List<Subject>> = database.subjectDao().getAllSubjects()
    val allAlerts: Flow<List<Alert>> = database.alertDao().getAllAlerts()
    val allEvents: Flow<List<CalendarEvent>> = database.calendarEventDao().getAllEvents()
    val allIdeas: Flow<List<IdeaProblem>> = database.ideaProblemDao().getAllIdeas()
    val allTodoTasks: Flow<List<TodoTask>> = database.todoTaskDao().getAllTasks()

    fun getSubjectById(id: Long): Flow<Subject?> = database.subjectDao().getSubjectById(id)
    fun getChaptersForSubject(subjectId: Long): Flow<List<Chapter>> = database.chapterDao().getChaptersForSubject(subjectId)
    fun getNotesForSubject(subjectId: Long): Flow<List<ClassNote>> = database.classNoteDao().getNotesForSubject(subjectId)
    fun getNotesForChapter(chapterId: Long): Flow<List<ClassNote>> = database.classNoteDao().getNotesForChapter(chapterId)
    fun getNoticesForSubject(subjectId: Long): Flow<List<Notice>> = database.noticeDao().getNoticesForSubject(subjectId)
    fun getChapterTasks(chapterId: Long): Flow<List<ChapterTask>> = database.chapterTaskDao().getTasksForChapter(chapterId)
    fun getChapterTasksForSubject(subjectId: Long): Flow<List<ChapterTask>> = database.chapterTaskDao().getTasksForSubject(subjectId)

    suspend fun insertSubject(subject: Subject): Long = withContext(Dispatchers.IO) {
        database.subjectDao().insert(subject)
    }

    suspend fun updateSubject(subject: Subject) = withContext(Dispatchers.IO) {
        database.subjectDao().update(subject)
    }

    suspend fun deleteSubject(subject: Subject) = withContext(Dispatchers.IO) {
        // Also delete associated note files from disk
        val notes = database.classNoteDao().getNotesForSubject(subject.id).first()
        for (note in notes) {
            FileUtils.deleteFileQuietly(note.localPath)
        }
        database.subjectDao().delete(subject)
    }

    // Chapters
    suspend fun insertChapter(chapter: Chapter): Long = withContext(Dispatchers.IO) {
        database.chapterDao().insert(chapter)
    }

    suspend fun updateChapter(chapter: Chapter) = withContext(Dispatchers.IO) {
        database.chapterDao().update(chapter)
    }

    suspend fun deleteChapter(chapter: Chapter, deleteNotes: Boolean) = withContext(Dispatchers.IO) {
        if (deleteNotes) {
            val notes = database.classNoteDao().getNotesForChapter(chapter.id).first()
            for (note in notes) {
                FileUtils.deleteFileQuietly(note.localPath)
            }
            database.classNoteDao().deleteNotesByChapter(chapter.id)
            database.chapterTaskDao().deleteTasksByChapter(chapter.id)
        }
        database.chapterDao().delete(chapter)
    }

    // Regular To-Do Tasks
    suspend fun insertTodoTask(task: TodoTask): Long = withContext(Dispatchers.IO) {
        database.todoTaskDao().insert(task)
    }

    suspend fun updateTodoTask(task: TodoTask) = withContext(Dispatchers.IO) {
        database.todoTaskDao().update(task)
    }

    suspend fun toggleTodoTask(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        database.todoTaskDao().setCompleted(id, completed)
    }

    suspend fun updateTodoTaskDueDate(id: Long, dueDate: String, dueTime: String) = withContext(Dispatchers.IO) {
        database.todoTaskDao().updateDueDate(id, dueDate, dueTime)
    }

    suspend fun deleteTodoTask(task: TodoTask) = withContext(Dispatchers.IO) {
        database.todoTaskDao().delete(task)
    }

    // Chapter Tasks
    suspend fun insertChapterTask(task: ChapterTask): Long = withContext(Dispatchers.IO) {
        database.chapterTaskDao().insert(task)
    }

    suspend fun updateChapterTask(task: ChapterTask) = withContext(Dispatchers.IO) {
        database.chapterTaskDao().update(task)
    }

    suspend fun toggleChapterTask(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        database.chapterTaskDao().setCompleted(id, completed)
    }

    suspend fun updateChapterTaskDueDate(id: Long, dueDate: String) = withContext(Dispatchers.IO) {
        database.chapterTaskDao().updateDueDate(id, dueDate)
    }

    suspend fun deleteChapterTask(task: ChapterTask) = withContext(Dispatchers.IO) {
        database.chapterTaskDao().delete(task)
    }

    // Class Notes
    suspend fun insertClassNote(note: ClassNote): Long = withContext(Dispatchers.IO) {
        database.classNoteDao().insert(note)
    }

    suspend fun updateClassNote(note: ClassNote) = withContext(Dispatchers.IO) {
        database.classNoteDao().update(note)
    }

    suspend fun renameClassNote(noteId: Long, newTitle: String, newDescription: String) = withContext(Dispatchers.IO) {
        database.classNoteDao().renameNote(noteId, newTitle, newDescription)
    }

    suspend fun moveClassNoteToChapter(noteId: Long, newChapterId: Long) = withContext(Dispatchers.IO) {
        database.classNoteDao().moveNoteToChapter(noteId, newChapterId)
    }

    suspend fun deleteClassNote(note: ClassNote) = withContext(Dispatchers.IO) {
        FileUtils.deleteFileQuietly(note.localPath)
        database.classNoteDao().delete(note)
    }

    // Notices
    suspend fun insertNotice(notice: Notice): Long = withContext(Dispatchers.IO) {
        database.noticeDao().insert(notice)
    }

    suspend fun updateNotice(notice: Notice) = withContext(Dispatchers.IO) {
        database.noticeDao().update(notice)
    }

    suspend fun deleteNotice(notice: Notice) = withContext(Dispatchers.IO) {
        database.noticeDao().delete(notice)
    }

    // Alerts
    suspend fun insertAlert(alert: Alert): Long = withContext(Dispatchers.IO) {
        database.alertDao().insert(alert)
    }

    suspend fun updateAlert(alert: Alert) = withContext(Dispatchers.IO) {
        database.alertDao().update(alert)
    }

    suspend fun toggleAlertCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        database.alertDao().setCompleted(id, completed)
    }

    suspend fun deleteAlert(alert: Alert) = withContext(Dispatchers.IO) {
        database.alertDao().delete(alert)
    }

    // Calendar Events
    suspend fun insertCalendarEvent(event: CalendarEvent): Long = withContext(Dispatchers.IO) {
        database.calendarEventDao().insert(event)
    }

    suspend fun deleteCalendarEvent(event: CalendarEvent) = withContext(Dispatchers.IO) {
        database.calendarEventDao().delete(event)
    }

    // Ideas & Problems
    suspend fun insertIdea(idea: IdeaProblem): Long = withContext(Dispatchers.IO) {
        database.ideaProblemDao().insert(idea)
    }

    suspend fun toggleIdeaResolved(id: Long, resolved: Boolean) = withContext(Dispatchers.IO) {
        database.ideaProblemDao().setResolved(id, resolved)
    }

    suspend fun deleteIdea(idea: IdeaProblem) = withContext(Dispatchers.IO) {
        database.ideaProblemDao().delete(idea)
    }

    /**
     * Seeds initial mock data if app is opened for the first time
     */
    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        val existingSubjects = database.subjectDao().getAllSubjects().first()
        if (existingSubjects.isNotEmpty()) return@withContext

        // Subject 1: Operating Systems
        val osSubjectId = database.subjectDao().insert(
            Subject(
                name = "Operating Systems",
                code = "CS-301",
                professor = "Dr. Alan Turing",
                room = "Hall B-204",
                colorHex = "#2563EB",
                schedule = "Mon/Wed 10:00 AM"
            )
        )

        // Chapters for Operating Systems
        val osCh1Id = database.chapterDao().insert(
            Chapter(subjectId = osSubjectId, title = "Chapter 1 — Introduction", orderIndex = 1)
        )
        val osCh2Id = database.chapterDao().insert(
            Chapter(subjectId = osSubjectId, title = "Chapter 2 — CPU Scheduling", orderIndex = 2)
        )
        val osCh3Id = database.chapterDao().insert(
            Chapter(subjectId = osSubjectId, title = "Chapter 3 — Memory Management", orderIndex = 3)
        )

        // Real sample PDF for Ch 1
        val pdf1File = FileUtils.createSamplePdf(
            context,
            "Lecture_1_OS_Intro.pdf",
            "Operating Systems",
            "Chapter 1 — Introduction"
        )
        database.classNoteDao().insert(
            ClassNote(
                subjectId = osSubjectId,
                chapterId = osCh1Id,
                title = "Lecture 1.pdf",
                fileName = pdf1File.name,
                fileType = "application/pdf",
                uploadedDate = "11 Sep 2026",
                localPath = pdf1File.absolutePath,
                fileSizeBytes = pdf1File.length(),
                description = "Introduction to OS architectures, kernel vs user space"
            )
        )

        // Real sample JPG for Ch 1
        val img1File = FileUtils.createSampleImage(
            context,
            "Process_Management_Diagram.jpg",
            "Process State Transition Diagram",
            "Operating Systems • Chapter 1"
        )
        database.classNoteDao().insert(
            ClassNote(
                subjectId = osSubjectId,
                chapterId = osCh1Id,
                title = "2026-09-11 — Process Management.jpg",
                fileName = img1File.name,
                fileType = "image/jpeg",
                uploadedDate = "11 Sep 2026",
                localPath = img1File.absolutePath,
                fileSizeBytes = img1File.length(),
                description = "State machine transitions: New, Ready, Running, Waiting, Terminated"
            )
        )

        // Real sample PDF for Ch 2
        val pdf2File = FileUtils.createSamplePdf(
            context,
            "Lecture_3_CPU_Scheduling.pdf",
            "Operating Systems",
            "Chapter 2 — CPU Scheduling"
        )
        database.classNoteDao().insert(
            ClassNote(
                subjectId = osSubjectId,
                chapterId = osCh2Id,
                title = "Lecture 3.pdf",
                fileName = pdf2File.name,
                fileType = "application/pdf",
                uploadedDate = "18 Sep 2026",
                localPath = pdf2File.absolutePath,
                fileSizeBytes = pdf2File.length(),
                description = "Round-robin, SJF, and priority scheduling algorithms"
            )
        )

        // Real sample JPG for Ch 2
        val img2File = FileUtils.createSampleImage(
            context,
            "Scheduling_Diagram.jpg",
            "CPU Scheduling Timeline & Gantt Chart",
            "Operating Systems • Chapter 2"
        )
        database.classNoteDao().insert(
            ClassNote(
                subjectId = osSubjectId,
                chapterId = osCh2Id,
                title = "Scheduling Diagram.jpg",
                fileName = img2File.name,
                fileType = "image/jpeg",
                uploadedDate = "20 Sep 2026",
                localPath = img2File.absolutePath,
                fileSizeBytes = img2File.length(),
                description = "Gantt chart comparison for FCFS vs Shortest Job First"
            )
        )

        // Real sample PDF for Ch 3
        val pdf3File = FileUtils.createSamplePdf(
            context,
            "Lecture_5_Memory_Mgmt.pdf",
            "Operating Systems",
            "Chapter 3 — Memory Management"
        )
        database.classNoteDao().insert(
            ClassNote(
                subjectId = osSubjectId,
                chapterId = osCh3Id,
                title = "Lecture 5.pdf",
                fileName = pdf3File.name,
                fileType = "application/pdf",
                uploadedDate = "25 Sep 2026",
                localPath = pdf3File.absolutePath,
                fileSizeBytes = pdf3File.length(),
                description = "Paging, segmentation, TLB, and page replacement policies"
            )
        )

        // Notices for OS
        database.noticeDao().insert(
            Notice(
                subjectId = osSubjectId,
                title = "Midterm Exam Announcement",
                content = "Midterm will cover Chapters 1 and 2. Calculator allowed. Bring physical student ID.",
                date = "2026-09-28",
                isPinned = true
            )
        )
        database.noticeDao().insert(
            Notice(
                subjectId = osSubjectId,
                title = "Lab 3 Deadline Extended",
                content = "Semaphore synchronization lab submission is extended to Friday 11:59 PM.",
                date = "2026-09-24",
                isPinned = false
            )
        )

        // Subject 2: Database Management Systems
        val dbSubjectId = database.subjectDao().insert(
            Subject(
                name = "Database Management System",
                code = "CS-304",
                professor = "Prof. Grace Hopper",
                room = "Lab 3-102",
                colorHex = "#10B981",
                schedule = "Tue/Thu 2:00 PM"
            )
        )

        val dbCh1Id = database.chapterDao().insert(
            Chapter(subjectId = dbSubjectId, title = "Chapter 1 — ER Modeling", orderIndex = 1)
        )
        val dbCh2Id = database.chapterDao().insert(
            Chapter(subjectId = dbSubjectId, title = "Chapter 2 — Relational Algebra", orderIndex = 2)
        )
        val dbCh3Id = database.chapterDao().insert(
            Chapter(subjectId = dbSubjectId, title = "Chapter 3 — SQL", orderIndex = 3)
        )

        val imgDbFile = FileUtils.createSampleImage(
            context,
            "SQL_Queries_Cheat_Sheet.jpg",
            "SQL Joins & Aggregations",
            "Database Systems • Chapter 3"
        )
        database.classNoteDao().insert(
            ClassNote(
                subjectId = dbSubjectId,
                chapterId = dbCh3Id,
                title = "SQL Queries.jpg",
                fileName = imgDbFile.name,
                fileType = "image/jpeg",
                uploadedDate = "11 Sep 2026",
                localPath = imgDbFile.absolutePath,
                fileSizeBytes = imgDbFile.length(),
                description = "Inner, Left, and Full Outer Join syntax examples"
            )
        )

        database.noticeDao().insert(
            Notice(
                subjectId = dbSubjectId,
                title = "SQL Project Specifications",
                content = "Submit your team schema diagram and DDL scripts through the portal.",
                date = "2026-09-22",
                isPinned = true
            )
        )

        // Subject 3: Computer Networks
        val netSubjectId = database.subjectDao().insert(
            Subject(
                name = "Computer Networks",
                code = "CS-308",
                professor = "Dr. Vint Cerf",
                room = "Auditorium 1",
                colorHex = "#8B5CF6",
                schedule = "Fri 9:00 AM"
            )
        )
        database.chapterDao().insert(
            Chapter(subjectId = netSubjectId, title = "Chapter 1 — OSI & TCP/IP Stack", orderIndex = 1)
        )

        // Alerts / Deadlines
        database.alertDao().insert(
            Alert(
                title = "OS Process Scheduler Implementation",
                subjectName = "Operating Systems",
                dueDate = "2026-10-04",
                dueTime = "23:59",
                type = "Assignment",
                priority = "High",
                isCompleted = false,
                notes = "Submit C++ code and test output on GitHub classroom"
            )
        )
        database.alertDao().insert(
            Alert(
                title = "DBMS Quiz 2: Relational Calculus",
                subjectName = "Database Management System",
                dueDate = "2026-10-08",
                dueTime = "14:15",
                type = "Quiz",
                priority = "Medium",
                isCompleted = false,
                notes = "Multiple choice & 2 schema normalization queries"
            )
        )
        database.alertDao().insert(
            Alert(
                title = "Wireshark Packet Trace Lab Report",
                subjectName = "Computer Networks",
                dueDate = "2026-10-12",
                dueTime = "17:00",
                type = "Lab",
                priority = "Low",
                isCompleted = false,
                notes = "Inspect 3-way TCP handshake packets"
            )
        )

        // Calendar Events & Countdowns
        database.calendarEventDao().insert(
            CalendarEvent(
                title = "Midterm Examinations",
                targetDate = "2026-10-19",
                category = "Exam",
                notes = "Central exam hall. All core theory courses."
            )
        )
        database.calendarEventDao().insert(
            CalendarEvent(
                title = "Campus Tech Fest & Hackathon",
                targetDate = "2026-10-30",
                category = "Milestone",
                notes = "48-hour student hackathon in the student center."
            )
        )
        database.calendarEventDao().insert(
            CalendarEvent(
                title = "Fall Semester Finals",
                targetDate = "2026-12-10",
                category = "Exam",
                notes = "Final comprehensive exams."
            )
        )

        // Ideas & Problems
        database.ideaProblemDao().insert(
            IdeaProblem(
                title = "Why does Banker's Algorithm assume maximum resource claim in advance?",
                description = "In modern dynamic environments, processes rarely know peak memory upfront. How do real kernels avoid deadlock without Bankers algorithm?",
                subjectName = "Operating Systems",
                type = "Problem",
                isResolved = false,
                solutionNotes = ""
            )
        )
        database.ideaProblemDao().insert(
            IdeaProblem(
                title = "Study group notes indexer with full-text search",
                description = "Could build an offline mobile indexer for chapter notes using SQLite FTS5.",
                subjectName = "Database Management System",
                type = "Idea",
                isResolved = true,
                solutionNotes = "Implemented in CampusMate local database!"
            )
        )

        // Regular To-Do Tasks (with priorities and coming due dates or "add later")
        database.todoTaskDao().insert(
            TodoTask(
                title = "Borrow Operating Systems 10th Ed. from library",
                priority = "High",
                dueDate = "2026-10-03",
                dueTime = "15:00",
                isCompleted = false,
                category = "Academic",
                notes = "Check main campus library science wing"
            )
        )
        database.todoTaskDao().insert(
            TodoTask(
                title = "Review relational normalization practice set",
                priority = "High",
                dueDate = "", // Add due date later!
                isCompleted = false,
                category = "Academic",
                notes = "Need to check 3NF vs BCNF examples before midterm"
            )
        )
        database.todoTaskDao().insert(
            TodoTask(
                title = "Form 4-person study group for Networks lab",
                priority = "Medium",
                dueDate = "2026-10-07",
                dueTime = "18:00",
                isCompleted = false,
                category = "Project",
                notes = "Email Alice and Bob about Wireshark setup"
            )
        )
        database.todoTaskDao().insert(
            TodoTask(
                title = "Order engineering graph paper and binder tabs",
                priority = "Low",
                dueDate = "", // Add due date later!
                isCompleted = true,
                category = "General",
                notes = "Purchased from campus store"
            )
        )

        // Chapter-Specific To-Do Tasks
        database.chapterTaskDao().insert(
            ChapterTask(
                subjectId = osSubjectId,
                chapterId = osCh1Id,
                title = "Read Chapter 1 textbook pages 1–32",
                priority = "High",
                dueDate = "2026-10-04",
                isCompleted = true,
                notes = "Highlight kernel architectures"
            )
        )
        database.chapterTaskDao().insert(
            ChapterTask(
                subjectId = osSubjectId,
                chapterId = osCh1Id,
                title = "Memorize 5-state process lifecycle diagram",
                priority = "Medium",
                dueDate = "", // Due date to be added later
                isCompleted = false,
                notes = "Focus on transition conditions between Ready and Running"
            )
        )
        database.chapterTaskDao().insert(
            ChapterTask(
                subjectId = osSubjectId,
                chapterId = osCh2Id,
                title = "Solve 4 Gantt chart scheduling problems (FCFS & SJF)",
                priority = "High",
                dueDate = "2026-10-06",
                isCompleted = false,
                notes = "Calculate average turnaround and waiting times"
            )
        )
        database.chapterTaskDao().insert(
            ChapterTask(
                subjectId = dbSubjectId,
                chapterId = dbCh3Id,
                title = "Practice SQL JOIN queries and GROUP BY HAVING clause",
                priority = "High",
                dueDate = "", // Add later
                isCompleted = false,
                notes = "Test queries on sample university database"
            )
        )
    }
}
