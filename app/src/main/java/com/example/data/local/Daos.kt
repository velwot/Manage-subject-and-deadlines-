package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    fun getSubjectById(id: Long): Flow<Subject?>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectByIdDirect(id: Long): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subject: Subject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subjects: List<Subject>)

    @Update
    suspend fun update(subject: Subject)

    @Delete
    suspend fun delete(subject: Subject)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM subjects")
    suspend fun clearAll()
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIndex ASC, id ASC")
    fun getChaptersForSubject(subjectId: Long): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters ORDER BY orderIndex ASC, id ASC")
    fun getAllChapters(): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    fun getChapterById(id: Long): Flow<Chapter?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chapter: Chapter): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<Chapter>)

    @Update
    suspend fun update(chapter: Chapter)

    @Delete
    suspend fun delete(chapter: Chapter)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM chapters")
    suspend fun clearAll()
}

@Dao
interface ClassNoteDao {
    @Query("SELECT * FROM class_notes WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getNotesForSubject(subjectId: Long): Flow<List<ClassNote>>

    @Query("SELECT * FROM class_notes WHERE chapterId = :chapterId ORDER BY createdAt DESC")
    fun getNotesForChapter(chapterId: Long): Flow<List<ClassNote>>

    @Query("SELECT * FROM class_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<ClassNote>>

    @Query("SELECT * FROM class_notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Long): Flow<ClassNote?>

    @Query("SELECT * FROM class_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdDirect(id: Long): ClassNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: ClassNote): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<ClassNote>)

    @Update
    suspend fun update(note: ClassNote)

    @Delete
    suspend fun delete(note: ClassNote)

    @Query("DELETE FROM class_notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM class_notes WHERE chapterId = :chapterId")
    suspend fun deleteNotesByChapter(chapterId: Long)

    @Query("UPDATE class_notes SET chapterId = :newChapterId WHERE id = :noteId")
    suspend fun moveNoteToChapter(noteId: Long, newChapterId: Long)

    @Query("UPDATE class_notes SET title = :newTitle, description = :newDescription WHERE id = :noteId")
    suspend fun renameNote(noteId: Long, newTitle: String, newDescription: String)

    @Query("DELETE FROM class_notes")
    suspend fun clearAll()
}

@Dao
interface NoticeDao {
    @Query("SELECT * FROM notices WHERE subjectId = :subjectId ORDER BY isPinned DESC, createdAt DESC")
    fun getNoticesForSubject(subjectId: Long): Flow<List<Notice>>

    @Query("SELECT * FROM notices ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotices(): Flow<List<Notice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notice: Notice): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notices: List<Notice>)

    @Update
    suspend fun update(notice: Notice)

    @Delete
    suspend fun delete(notice: Notice)

    @Query("DELETE FROM notices WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notices")
    suspend fun clearAll()
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY isCompleted ASC, dueDate ASC, dueTime ASC")
    fun getAllAlerts(): Flow<List<Alert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: Alert): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alerts: List<Alert>)

    @Update
    suspend fun update(alert: Alert)

    @Delete
    suspend fun delete(alert: Alert)

    @Query("UPDATE alerts SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM alerts")
    suspend fun clearAll()
}

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events ORDER BY targetDate ASC")
    fun getAllEvents(): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CalendarEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<CalendarEvent>)

    @Update
    suspend fun update(event: CalendarEvent)

    @Delete
    suspend fun delete(event: CalendarEvent)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM calendar_events")
    suspend fun clearAll()
}

@Dao
interface IdeaProblemDao {
    @Query("SELECT * FROM idea_problems ORDER BY isResolved ASC, createdAt DESC")
    fun getAllIdeas(): Flow<List<IdeaProblem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(idea: IdeaProblem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ideas: List<IdeaProblem>)

    @Update
    suspend fun update(idea: IdeaProblem)

    @Delete
    suspend fun delete(idea: IdeaProblem)

    @Query("UPDATE idea_problems SET isResolved = :resolved WHERE id = :id")
    suspend fun setResolved(id: Long, resolved: Boolean)

    @Query("DELETE FROM idea_problems WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM idea_problems")
    suspend fun clearAll()
}

@Dao
interface TodoTaskDao {
    @Query("SELECT * FROM todo_tasks ORDER BY isCompleted ASC, CASE WHEN priority = 'High' THEN 1 WHEN priority = 'Medium' THEN 2 WHEN priority = 'Low' THEN 3 ELSE 4 END, CASE WHEN dueDate = '' THEN 1 ELSE 0 END, dueDate ASC, createdAt DESC")
    fun getAllTasks(): Flow<List<TodoTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TodoTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TodoTask>)

    @Update
    suspend fun update(task: TodoTask)

    @Delete
    suspend fun delete(task: TodoTask)

    @Query("DELETE FROM todo_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE todo_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("UPDATE todo_tasks SET dueDate = :dueDate, dueTime = :dueTime WHERE id = :id")
    suspend fun updateDueDate(id: Long, dueDate: String, dueTime: String)

    @Query("DELETE FROM todo_tasks")
    suspend fun clearAll()
}

@Dao
interface ChapterTaskDao {
    @Query("SELECT * FROM chapter_tasks WHERE chapterId = :chapterId ORDER BY isCompleted ASC, CASE WHEN priority = 'High' THEN 1 WHEN priority = 'Medium' THEN 2 ELSE 3 END, createdAt ASC")
    fun getTasksForChapter(chapterId: Long): Flow<List<ChapterTask>>

    @Query("SELECT * FROM chapter_tasks WHERE subjectId = :subjectId ORDER BY isCompleted ASC, createdAt ASC")
    fun getTasksForSubject(subjectId: Long): Flow<List<ChapterTask>>

    @Query("SELECT * FROM chapter_tasks ORDER BY createdAt ASC")
    fun getAllChapterTasks(): Flow<List<ChapterTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: ChapterTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<ChapterTask>)

    @Update
    suspend fun update(task: ChapterTask)

    @Delete
    suspend fun delete(task: ChapterTask)

    @Query("DELETE FROM chapter_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM chapter_tasks WHERE chapterId = :chapterId")
    suspend fun deleteTasksByChapter(chapterId: Long)

    @Query("UPDATE chapter_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("UPDATE chapter_tasks SET dueDate = :dueDate WHERE id = :id")
    suspend fun updateDueDate(id: Long, dueDate: String)

    @Query("DELETE FROM chapter_tasks")
    suspend fun clearAll()
}
