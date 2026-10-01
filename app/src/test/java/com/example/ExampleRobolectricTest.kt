package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Chapter
import com.example.data.model.ClassNote
import com.example.data.model.Subject
import com.example.util.DateUtils
import com.example.util.FileUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun verifyAppName() {
        val appName = context.getString(R.string.app_name)
        assertEquals("CampusMate", appName)
    }

    @Test
    fun testSubjectAndChapterAndNotesHierarchy() = runBlocking {
        // 1. Create Subject: Operating Systems
        val subjectId = db.subjectDao().insert(
            Subject(
                name = "Operating Systems",
                code = "CS-301",
                professor = "Dr. Alan Turing"
            )
        )
        assertTrue(subjectId > 0)

        // 2. Create Chapters
        val ch1Id = db.chapterDao().insert(
            Chapter(subjectId = subjectId, title = "Chapter 1 — Introduction", orderIndex = 1)
        )
        val ch2Id = db.chapterDao().insert(
            Chapter(subjectId = subjectId, title = "Chapter 2 — CPU Scheduling", orderIndex = 2)
        )
        assertTrue(ch1Id > 0)
        assertTrue(ch2Id > 0)

        // 3. Add Notes: PDF and JPG
        val note1Id = db.classNoteDao().insert(
            ClassNote(
                subjectId = subjectId,
                chapterId = ch1Id,
                title = "Lecture 1.pdf",
                fileName = "lecture_1.pdf",
                fileType = "application/pdf",
                uploadedDate = "11 Sep 2026",
                localPath = "/fake/path/lecture_1.pdf",
                fileSizeBytes = 1024 * 500,
                description = "Introduction to OS architectures"
            )
        )

        val note2Id = db.classNoteDao().insert(
            ClassNote(
                subjectId = subjectId,
                chapterId = ch1Id,
                title = "2026-09-11 — Process Management.jpg",
                fileName = "process_diagram.jpg",
                fileType = "image/jpeg",
                uploadedDate = "11 Sep 2026",
                localPath = "/fake/path/process_diagram.jpg",
                fileSizeBytes = 1024 * 300,
                description = "Process State diagram"
            )
        )

        // 4. Verify notes count for chapter 1
        val ch1Notes = db.classNoteDao().getNotesForChapter(ch1Id).first()
        assertEquals(2, ch1Notes.size)

        // 5. Move note2 to Chapter 2
        db.classNoteDao().moveNoteToChapter(note2Id, ch2Id)
        val ch1NotesAfterMove = db.classNoteDao().getNotesForChapter(ch1Id).first()
        val ch2NotesAfterMove = db.classNoteDao().getNotesForChapter(ch2Id).first()
        assertEquals(1, ch1NotesAfterMove.size)
        assertEquals(1, ch2NotesAfterMove.size)
        assertEquals("2026-09-11 — Process Management.jpg", ch2NotesAfterMove[0].title)

        // 6. Rename note
        db.classNoteDao().renameNote(note1Id, "Lecture 1 - Updated.pdf", "Revised lecture notes")
        val updatedNote = db.classNoteDao().getNoteByIdDirect(note1Id)
        assertNotNull(updatedNote)
        assertEquals("Lecture 1 - Updated.pdf", updatedNote?.title)
        assertEquals("Revised lecture notes", updatedNote?.description)

        // 7. Delete note
        db.classNoteDao().deleteById(note1Id)
        val notesAfterDelete = db.classNoteDao().getNotesForChapter(ch1Id).first()
        assertEquals(0, notesAfterDelete.size)

        // 8. Delete chapter with its notes
        db.classNoteDao().deleteNotesByChapter(ch2Id)
        db.chapterDao().deleteById(ch2Id)
        val chaptersRemaining = db.chapterDao().getChaptersForSubject(subjectId).first()
        assertEquals(1, chaptersRemaining.size)
    }

    @Test
    fun testFileUtilsMimeAndFormatting() {
        assertTrue(FileUtils.isPdf("application/pdf", "lecture.pdf"))
        assertTrue(FileUtils.isPdf("unknown/type", "notes.pdf"))
        assertFalse(FileUtils.isPdf("image/jpeg", "diagram.jpg"))

        assertTrue(FileUtils.isImage("image/jpeg", "photo.jpg"))
        assertTrue(FileUtils.isImage("image/png", "screenshot.png"))
        assertFalse(FileUtils.isImage("application/pdf", "document.pdf"))

        assertEquals("application/pdf", FileUtils.getMimeTypeFromFileName("sample.pdf"))
        assertEquals("image/jpeg", FileUtils.getMimeTypeFromFileName("sample.jpg"))
        assertEquals("image/png", FileUtils.getMimeTypeFromFileName("sample.png"))

        assertEquals("500.0 KB", FileUtils.formatFileSize(512000L))
        assertEquals("2.0 MB", FileUtils.formatFileSize(2097152L))
    }

    @Test
    fun testDateUtils() {
        val iso = "2026-09-29"
        val formatted = DateUtils.formatIsoToDisplay(iso)
        assertTrue(formatted.contains("Sep") || formatted.contains("2026"))
    }

    @Test
    fun testTodoTaskOperations() = runBlocking {
        // 1. Insert task with "add later" (blank due date)
        val taskId = db.todoTaskDao().insert(
            com.example.data.model.TodoTask(
                title = "Study for Operating Systems quiz",
                priority = "High",
                dueDate = "" // Add due date later
            )
        )
        assertTrue(taskId > 0)

        // 2. Query all tasks
        val tasks = db.todoTaskDao().getAllTasks().first()
        assertEquals(1, tasks.size)
        assertEquals("Study for Operating Systems quiz", tasks[0].title)
        assertEquals("High", tasks[0].priority)
        assertEquals("", tasks[0].dueDate)

        // 3. Update due date later
        db.todoTaskDao().updateDueDate(taskId, "2026-10-15", "14:00")
        val updatedTasks = db.todoTaskDao().getAllTasks().first()
        assertEquals("2026-10-15", updatedTasks[0].dueDate)
        assertEquals("14:00", updatedTasks[0].dueTime)

        // 4. Toggle completion
        db.todoTaskDao().setCompleted(taskId, true)
        val completedTasks = db.todoTaskDao().getAllTasks().first()
        assertTrue(completedTasks[0].isCompleted)
    }

    @Test
    fun testChapterTaskOperations() = runBlocking {
        val subjectId = db.subjectDao().insert(
            Subject(name = "Computer Architecture", code = "CS-302")
        )
        val chapterId = db.chapterDao().insert(
            Chapter(subjectId = subjectId, title = "Chapter 1 — Pipeline Hazards")
        )

        // Add Chapter Task
        val chTaskId = db.chapterTaskDao().insert(
            com.example.data.model.ChapterTask(
                subjectId = subjectId,
                chapterId = chapterId,
                title = "Solve structural hazard practice exercises",
                priority = "High",
                dueDate = "" // Add later
            )
        )
        assertTrue(chTaskId > 0)

        val chTasks = db.chapterTaskDao().getTasksForChapter(chapterId).first()
        assertEquals(1, chTasks.size)
        assertEquals("Solve structural hazard practice exercises", chTasks[0].title)

        // Update due date
        db.chapterTaskDao().updateDueDate(chTaskId, "2026-10-20")
        val chTasksUpdated = db.chapterTaskDao().getTasksForChapter(chapterId).first()
        assertEquals("2026-10-20", chTasksUpdated[0].dueDate)

        // Toggle completed
        db.chapterTaskDao().setCompleted(chTaskId, true)
        val chTasksCompleted = db.chapterTaskDao().getTasksForChapter(chapterId).first()
        assertTrue(chTasksCompleted[0].isCompleted)
    }
}
