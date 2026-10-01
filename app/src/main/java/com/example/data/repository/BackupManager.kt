package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun exportBackup(outputUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val subjects = database.subjectDao().getAllSubjects().first()
            val chapters = database.chapterDao().getAllChapters().first()
            val classNotes = database.classNoteDao().getAllNotes().first()
            val notices = database.noticeDao().getAllNotices().first()
            val alerts = database.alertDao().getAllAlerts().first()
            val calendarEvents = database.calendarEventDao().getAllEvents().first()
            val ideaProblems = database.ideaProblemDao().getAllIdeas().first()
            val todoTasks = database.todoTaskDao().getAllTasks().first()
            val chapterTasks = database.chapterTaskDao().getAllChapterTasks().first()

            val rootJson = JSONObject().apply {
                put("version", 2)
                put("appName", "CampusMate")
                put("exportedAt", System.currentTimeMillis())

                // Subjects
                val subjectsArray = JSONArray()
                for (s in subjects) {
                    subjectsArray.put(JSONObject().apply {
                        put("id", s.id)
                        put("name", s.name)
                        put("code", s.code)
                        put("professor", s.professor)
                        put("room", s.room)
                        put("colorHex", s.colorHex)
                        put("schedule", s.schedule)
                        put("createdAt", s.createdAt)
                    })
                }
                put("subjects", subjectsArray)

                // Chapters
                val chaptersArray = JSONArray()
                for (c in chapters) {
                    chaptersArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("subjectId", c.subjectId)
                        put("title", c.title)
                        put("orderIndex", c.orderIndex)
                        put("createdAt", c.createdAt)
                    })
                }
                put("chapters", chaptersArray)

                // Class Notes
                val notesArray = JSONArray()
                for (n in classNotes) {
                    notesArray.put(JSONObject().apply {
                        put("id", n.id)
                        put("subjectId", n.subjectId)
                        put("chapterId", n.chapterId)
                        put("title", n.title)
                        put("fileName", n.fileName)
                        put("fileType", n.fileType)
                        put("uploadedDate", n.uploadedDate)
                        put("localPath", n.localPath)
                        put("fileSizeBytes", n.fileSizeBytes)
                        put("description", n.description)
                        put("createdAt", n.createdAt)
                    })
                }
                put("classNotes", notesArray)

                // Notices
                val noticesArray = JSONArray()
                for (no in notices) {
                    noticesArray.put(JSONObject().apply {
                        put("id", no.id)
                        put("subjectId", no.subjectId)
                        put("title", no.title)
                        put("content", no.content)
                        put("date", no.date)
                        put("isPinned", no.isPinned)
                        put("createdAt", no.createdAt)
                    })
                }
                put("notices", noticesArray)

                // Alerts
                val alertsArray = JSONArray()
                for (a in alerts) {
                    alertsArray.put(JSONObject().apply {
                        put("id", a.id)
                        put("title", a.title)
                        put("subjectName", a.subjectName)
                        put("dueDate", a.dueDate)
                        put("dueTime", a.dueTime)
                        put("type", a.type)
                        put("priority", a.priority)
                        put("isCompleted", a.isCompleted)
                        put("notes", a.notes)
                        put("createdAt", a.createdAt)
                    })
                }
                put("alerts", alertsArray)

                // Calendar Events
                val eventsArray = JSONArray()
                for (e in calendarEvents) {
                    eventsArray.put(JSONObject().apply {
                        put("id", e.id)
                        put("title", e.title)
                        put("targetDate", e.targetDate)
                        put("category", e.category)
                        put("notes", e.notes)
                        put("createdAt", e.createdAt)
                    })
                }
                put("calendarEvents", eventsArray)

                // Ideas / Problems
                val ideasArray = JSONArray()
                for (i in ideaProblems) {
                    ideasArray.put(JSONObject().apply {
                        put("id", i.id)
                        put("title", i.title)
                        put("description", i.description)
                        put("subjectName", i.subjectName)
                        put("type", i.type)
                        put("isResolved", i.isResolved)
                        put("solutionNotes", i.solutionNotes)
                        put("createdAt", i.createdAt)
                    })
                }
                put("ideaProblems", ideasArray)

                // Regular To-Do Tasks
                val todoTasksArray = JSONArray()
                for (t in todoTasks) {
                    todoTasksArray.put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("priority", t.priority)
                        put("dueDate", t.dueDate)
                        put("dueTime", t.dueTime)
                        put("isCompleted", t.isCompleted)
                        put("category", t.category)
                        put("notes", t.notes)
                        put("createdAt", t.createdAt)
                    })
                }
                put("todoTasks", todoTasksArray)

                // Chapter Tasks
                val chapterTasksArray = JSONArray()
                for (ct in chapterTasks) {
                    chapterTasksArray.put(JSONObject().apply {
                        put("id", ct.id)
                        put("subjectId", ct.subjectId)
                        put("chapterId", ct.chapterId)
                        put("title", ct.title)
                        put("priority", ct.priority)
                        put("dueDate", ct.dueDate)
                        put("isCompleted", ct.isCompleted)
                        put("notes", ct.notes)
                        put("createdAt", ct.createdAt)
                    })
                }
                put("chapterTasks", chapterTasksArray)
            }

            val outputStream = context.contentResolver.openOutputStream(outputUri)
                ?: return@withContext Result.failure(Exception("Cannot open output stream for export."))

            var fileCount = 0
            ZipOutputStream(outputStream).use { zos ->
                // Write backup metadata json
                val jsonEntry = ZipEntry("backup_data.json")
                zos.putNextEntry(jsonEntry)
                zos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Write attachment files
                for (n in classNotes) {
                    val file = File(n.localPath)
                    if (file.exists() && file.isFile) {
                        try {
                            val entry = ZipEntry("files/${n.fileName}")
                            zos.putNextEntry(entry)
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                            fileCount++
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            Result.success(fileCount)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importBackup(inputUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(inputUri)
                ?: return@withContext Result.failure(Exception("Cannot open backup file."))

            // Check if it's a zip or plain json
            val headerBytes = ByteArray(4)
            val isZip = try {
                val checkStream = contentResolver.openInputStream(inputUri)
                val read = checkStream?.read(headerBytes) ?: 0
                checkStream?.close()
                // Zip file magic bytes: PK (0x50, 0x4B)
                read >= 2 && headerBytes[0] == 0x50.toByte() && headerBytes[1] == 0x4B.toByte()
            } catch (e: Exception) {
                false
            }

            var jsonString: String? = null
            val notesDir = FileUtils.getNotesDirectory(context)
            var restoredFiles = 0

            if (isZip) {
                ZipInputStream(contentResolver.openInputStream(inputUri)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            if (entry.name == "backup_data.json" || entry.name.endsWith(".json")) {
                                jsonString = zis.bufferedReader(Charsets.UTF_8).readText()
                            } else if (entry.name.startsWith("files/")) {
                                val cleanFileName = entry.name.removePrefix("files/")
                                if (cleanFileName.isNotBlank()) {
                                    val targetFile = File(notesDir, cleanFileName)
                                    FileOutputStream(targetFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                    restoredFiles++
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } else {
                // Backward compatibility: read as plain JSON
                jsonString = inputStream.bufferedReader(Charsets.UTF_8).readText()
                inputStream.close()
            }

            if (jsonString.isNullOrBlank()) {
                return@withContext Result.failure(Exception("No valid backup data found in file."))
            }

            val rootJson = JSONObject(jsonString)
            parseAndRestoreData(rootJson, notesDir)

            Result.success("Restored successfully ($restoredFiles files imported)")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun parseAndRestoreData(rootJson: JSONObject, notesDir: File) {
        // ID mapping to prevent collisions if required
        val oldSubjectIdToNew = mutableMapOf<Long, Long>()
        val oldChapterIdToNew = mutableMapOf<Long, Long>()

        // 1. Subjects
        if (rootJson.has("subjects")) {
            val array = rootJson.getJSONArray("subjects")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val oldId = obj.optLong("id", 0L)
                val subject = Subject(
                    name = obj.getString("name"),
                    code = obj.optString("code", ""),
                    professor = obj.optString("professor", ""),
                    room = obj.optString("room", ""),
                    colorHex = obj.optString("colorHex", "#3B82F6"),
                    schedule = obj.optString("schedule", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                val newId = database.subjectDao().insert(subject)
                if (oldId > 0) {
                    oldSubjectIdToNew[oldId] = newId
                }
            }
        }

        // 2. Chapters
        if (rootJson.has("chapters")) {
            val array = rootJson.getJSONArray("chapters")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val oldId = obj.optLong("id", 0L)
                val oldSubjectId = obj.optLong("subjectId", 0L)
                val newSubjectId = oldSubjectIdToNew[oldSubjectId] ?: oldSubjectId

                val chapter = Chapter(
                    subjectId = newSubjectId,
                    title = obj.getString("title"),
                    orderIndex = obj.optInt("orderIndex", 0),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                val newId = database.chapterDao().insert(chapter)
                if (oldId > 0) {
                    oldChapterIdToNew[oldId] = newId
                }
            }
        }

        // 3. Class Notes
        if (rootJson.has("classNotes")) {
            val array = rootJson.getJSONArray("classNotes")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val oldSubjectId = obj.optLong("subjectId", 0L)
                val oldChapterId = obj.optLong("chapterId", 0L)
                val newSubjectId = oldSubjectIdToNew[oldSubjectId] ?: oldSubjectId
                val newChapterId = oldChapterIdToNew[oldChapterId] ?: oldChapterId

                val fileName = obj.optString("fileName", "")
                val localFile = File(notesDir, fileName)
                val resolvedPath = if (localFile.exists()) localFile.absolutePath else obj.optString("localPath", "")

                val note = ClassNote(
                    subjectId = newSubjectId,
                    chapterId = newChapterId,
                    title = obj.getString("title"),
                    fileName = fileName,
                    fileType = obj.optString("fileType", "application/pdf"),
                    uploadedDate = obj.optString("uploadedDate", ""),
                    localPath = resolvedPath,
                    fileSizeBytes = obj.optLong("fileSizeBytes", localFile.length()),
                    description = obj.optString("description", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.classNoteDao().insert(note)
            }
        }

        // 4. Notices
        if (rootJson.has("notices")) {
            val array = rootJson.getJSONArray("notices")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val oldSubjectId = obj.optLong("subjectId", 0L)
                val newSubjectId = oldSubjectIdToNew[oldSubjectId] ?: oldSubjectId

                val notice = Notice(
                    subjectId = newSubjectId,
                    title = obj.getString("title"),
                    content = obj.optString("content", ""),
                    date = obj.optString("date", ""),
                    isPinned = obj.optBoolean("isPinned", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.noticeDao().insert(notice)
            }
        }

        // 5. Alerts
        if (rootJson.has("alerts")) {
            val array = rootJson.getJSONArray("alerts")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val alert = Alert(
                    title = obj.getString("title"),
                    subjectName = obj.optString("subjectName", ""),
                    dueDate = obj.optString("dueDate", ""),
                    dueTime = obj.optString("dueTime", "23:59"),
                    type = obj.optString("type", "Assignment"),
                    priority = obj.optString("priority", "Medium"),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.alertDao().insert(alert)
            }
        }

        // 6. Calendar Events
        if (rootJson.has("calendarEvents")) {
            val array = rootJson.getJSONArray("calendarEvents")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val event = CalendarEvent(
                    title = obj.getString("title"),
                    targetDate = obj.optString("targetDate", ""),
                    category = obj.optString("category", "Exam"),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.calendarEventDao().insert(event)
            }
        }

        // 7. Idea Problems
        if (rootJson.has("ideaProblems")) {
            val array = rootJson.getJSONArray("ideaProblems")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val idea = IdeaProblem(
                    title = obj.getString("title"),
                    description = obj.optString("description", ""),
                    subjectName = obj.optString("subjectName", ""),
                    type = obj.optString("type", "Problem"),
                    isResolved = obj.optBoolean("isResolved", false),
                    solutionNotes = obj.optString("solutionNotes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.ideaProblemDao().insert(idea)
            }
        }

        // 8. Regular To-Do Tasks
        if (rootJson.has("todoTasks")) {
            val array = rootJson.getJSONArray("todoTasks")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val task = TodoTask(
                    title = obj.getString("title"),
                    priority = obj.optString("priority", "Medium"),
                    dueDate = obj.optString("dueDate", ""),
                    dueTime = obj.optString("dueTime", ""),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    category = obj.optString("category", "General"),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.todoTaskDao().insert(task)
            }
        }

        // 9. Chapter Tasks
        if (rootJson.has("chapterTasks")) {
            val array = rootJson.getJSONArray("chapterTasks")
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val oldSubjectId = obj.optLong("subjectId", 0L)
                val oldChapterId = obj.optLong("chapterId", 0L)
                val newSubjectId = oldSubjectIdToNew[oldSubjectId] ?: oldSubjectId
                val newChapterId = oldChapterIdToNew[oldChapterId] ?: oldChapterId

                val chapterTask = ChapterTask(
                    subjectId = newSubjectId,
                    chapterId = newChapterId,
                    title = obj.getString("title"),
                    priority = obj.optString("priority", "Medium"),
                    dueDate = obj.optString("dueDate", ""),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.chapterTaskDao().insert(chapterTask)
            }
        }
    }
}
