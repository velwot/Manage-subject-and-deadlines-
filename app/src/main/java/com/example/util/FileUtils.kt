package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.UUID

object FileUtils {

    fun getNotesDirectory(context: Context): File {
        val dir = File(context.filesDir, "class_notes")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getTempCameraUri(context: Context): Pair<Uri, File> {
        val tempDir = File(context.cacheDir, "camera")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        val file = File(tempDir, "temp_camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Pair(uri, file)
    }

    fun copyUriToInternalStorage(context: Context, sourceUri: Uri): NoteFileInfo? {
        return try {
            val contentResolver = context.contentResolver
            val originalName = queryFileName(context, sourceUri) ?: "note_${System.currentTimeMillis()}"
            val mimeType = contentResolver.getType(sourceUri) ?: getMimeTypeFromFileName(originalName)

            val extension = getExtensionForMimeType(mimeType) ?: getExtensionFromFileName(originalName) ?: "bin"
            val safeBaseName = originalName.substringBeforeLast(".").replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFileName = "note_${System.currentTimeMillis()}_${safeBaseName}.$extension"

            val targetFile = File(getNotesDirectory(context), targetFileName)
            contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            NoteFileInfo(
                file = targetFile,
                fileName = targetFileName,
                originalName = originalName,
                mimeType = mimeType,
                sizeBytes = targetFile.length()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveCapturedImage(context: Context, tempFile: File, userTitle: String?): NoteFileInfo? {
        return try {
            if (!tempFile.exists() || tempFile.length() == 0L) {
                return null
            }
            val titleClean = (userTitle ?: "Class Note").replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFileName = "note_${System.currentTimeMillis()}_$titleClean.jpg"
            val targetFile = File(getNotesDirectory(context), targetFileName)

            tempFile.inputStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.delete()

            NoteFileInfo(
                file = targetFile,
                fileName = targetFileName,
                originalName = if (userTitle?.endsWith(".jpg") == true) userTitle else "$titleClean.jpg",
                mimeType = "image/jpeg",
                sizeBytes = targetFile.length()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openNoteFile(context: Context, filePath: String, mimeType: String): Result<String> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return Result.failure(Exception("Note file does not exist on device."))
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val resolvedMimeType = if (mimeType.isNotBlank() && mimeType != "*/*") {
                mimeType
            } else {
                getMimeTypeFromFileName(file.name)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, resolvedMimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Result.success("Opening note")
        } catch (e: ActivityNotFoundException) {
            val friendlyMsg = when {
                mimeType.contains("pdf", ignoreCase = true) || filePath.endsWith(".pdf", ignoreCase = true) ->
                    "No PDF viewer application found on device. Please install Google Drive, Files by Google, or a PDF reader."
                mimeType.startsWith("image/", ignoreCase = true) || isImageFile(filePath) ->
                    "No image viewer application found to open this picture."
                else ->
                    "No application installed capable of opening this file type ($mimeType)."
            }
            Result.failure(Exception(friendlyMsg))
        } catch (e: Exception) {
            Result.failure(Exception("Unable to open note: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    fun isPdf(mimeType: String, fileName: String): Boolean {
        return mimeType.equals("application/pdf", ignoreCase = true) || fileName.endsWith(".pdf", ignoreCase = true)
    }

    fun isImage(mimeType: String, fileName: String): Boolean {
        return mimeType.startsWith("image/", ignoreCase = true) || isImageFile(fileName)
    }

    private fun isImageFile(path: String): Boolean {
        val lower = path.lowercase(Locale.ROOT)
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
               lower.endsWith(".webp") || lower.endsWith(".bmp")
    }

    fun getMimeTypeFromFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (extension) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "txt" -> "text/plain"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        }
    }

    private fun getExtensionForMimeType(mimeType: String): String? {
        return when (mimeType) {
            "application/pdf" -> "pdf"
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        }
    }

    private fun getExtensionFromFileName(fileName: String): String? {
        val idx = fileName.lastIndexOf('.')
        return if (idx >= 0 && idx < fileName.length - 1) fileName.substring(idx + 1) else null
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        return cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                // Fallback
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[index])
    }

    fun deleteFileQuietly(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generates a real, valid PDF file using Android's built-in PdfDocument
     */
    fun createSamplePdf(context: Context, fileName: String, subjectName: String, chapterTitle: String): File {
        val dir = getNotesDirectory(context)
        val file = File(dir, fileName)
        if (file.exists()) return file

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Header Background
        paint.color = Color.rgb(37, 99, 235) // Blue 600
        canvas.drawRect(0f, 0f, 595f, 130f, paint)

        // Header Text
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("CampusMate • Lecture Notes", 40f, 55f, paint)

        paint.textSize = 15f
        paint.isFakeBoldText = false
        canvas.drawText("Subject: $subjectName", 40f, 85f, paint)
        canvas.drawText("Topic: $chapterTitle", 40f, 108f, paint)

        // Body Content
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("Key Concepts & Lecture Summary", 40f, 180f, paint)

        paint.textSize = 13f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(71, 85, 105)

        val bulletPoints = listOf(
            "1. Foundational principles, system architecture, and trade-offs.",
            "2. Key algorithms: scheduling, synchronization, and resource management.",
            "3. Concurrency models: threads, race conditions, semaphores, and mutexes.",
            "4. Memory hierarchy: caching, virtual memory, paging, and page replacement.",
            "5. Exam Focus: Remember Gantt chart calculations and turnaround time formulas."
        )

        var yOffset = 220f
        for (point in bulletPoints) {
            canvas.drawText(point, 50f, yOffset, paint)
            yOffset += 32f
        }

        // Decorative callout box
        paint.color = Color.rgb(239, 246, 255)
        canvas.drawRoundRect(40f, 420f, 555f, 530f, 16f, 16f, paint)

        paint.color = Color.rgb(30, 58, 138)
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("💡 Study Tip:", 60f, 455f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(30, 64, 175)
        canvas.drawText("Review past midterm problems and verify solutions with chapter classmates.", 60f, 485f, paint)

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 10f
        canvas.drawText("Generated by CampusMate Local Storage • Offline First", 40f, 800f, paint)

        pdfDocument.finishPage(page)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    /**
     * Generates a real sample JPEG diagram image using Android Canvas & Bitmap
     */
    fun createSampleImage(context: Context, fileName: String, title: String, subtitle: String): File {
        val dir = getNotesDirectory(context)
        val file = File(dir, fileName)
        if (file.exists()) return file

        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val paint = Paint().apply { isAntiAlias = true }
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Top Banner
        paint.color = Color.rgb(16, 185, 129) // Emerald 500
        canvas.drawRect(0f, 0f, width.toFloat(), 90f, paint)

        // Banner Text
        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("CampusMate Study Diagram", 40f, 55f, paint)

        // Subtitle
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText(title, 40f, 140f, paint)

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 15f
        paint.isFakeBoldText = false
        canvas.drawText(subtitle, 40f, 175f, paint)

        // Draw Process State Flow Nodes
        val states = listOf("New", "Ready", "Running", "Waiting", "Terminated")
        val stateColors = listOf(
            Color.rgb(59, 130, 246),
            Color.rgb(16, 185, 129),
            Color.rgb(245, 158, 11),
            Color.rgb(239, 68, 68),
            Color.rgb(107, 114, 128)
        )

        val xs = listOf(80f, 220f, 400f, 400f, 580f)
        val ys = listOf(280f, 280f, 280f, 420f, 280f)

        // Draw node boxes
        for (i in states.indices) {
            val x = xs[i]
            val y = ys[i]

            paint.color = stateColors[i]
            canvas.drawRoundRect(x, y, x + 130f, y + 65f, 14f, 14f, paint)

            paint.color = Color.WHITE
            paint.textSize = 16f
            paint.isFakeBoldText = true
            val textWidth = paint.measureText(states[i])
            canvas.drawText(states[i], x + (130f - textWidth) / 2f, y + 38f, paint)
        }

        // Draw arrows / connection lines
        paint.color = Color.rgb(71, 85, 105)
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        // New -> Ready
        canvas.drawLine(210f, 312f, 220f, 312f, paint)
        // Ready -> Running
        canvas.drawLine(350f, 312f, 400f, 312f, paint)
        // Running -> Terminated
        canvas.drawLine(530f, 312f, 580f, 312f, paint)
        // Running -> Waiting
        canvas.drawLine(465f, 345f, 465f, 420f, paint)
        // Waiting -> Ready
        canvas.drawLine(400f, 452f, 285f, 452f, paint)
        canvas.drawLine(285f, 452f, 285f, 345f, paint)
        paint.style = Paint.Style.FILL

        // Diagram labels
        paint.textSize = 12f
        paint.color = Color.rgb(100, 116, 139)
        paint.isFakeBoldText = false
        canvas.drawText("I/O Request", 480f, 385f, paint)
        canvas.drawText("I/O Done", 320f, 440f, paint)
        canvas.drawText("Scheduler Dispatch", 230f, 265f, paint)

        // Footer
        paint.textSize = 12f
        paint.color = Color.rgb(148, 163, 184)
        canvas.drawText("CampusMate Offline Diagram Notes • 2026", 40f, 560f, paint)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()
        return file
    }
}

data class NoteFileInfo(
    val file: File,
    val fileName: String,
    val originalName: String,
    val mimeType: String,
    val sizeBytes: Long
)
