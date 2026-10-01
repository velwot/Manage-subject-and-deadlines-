package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        Subject::class,
        Chapter::class,
        ClassNote::class,
        Notice::class,
        Alert::class,
        CalendarEvent::class,
        IdeaProblem::class,
        TodoTask::class,
        ChapterTask::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun classNoteDao(): ClassNoteDao
    abstract fun noticeDao(): NoticeDao
    abstract fun alertDao(): AlertDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun ideaProblemDao(): IdeaProblemDao
    abstract fun todoTaskDao(): TodoTaskDao
    abstract fun chapterTaskDao(): ChapterTaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "campusmate_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
