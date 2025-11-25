package com.gorden.dayexam.db

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gorden.dayexam.db.converter.*
import com.gorden.dayexam.db.dao.*
import com.gorden.dayexam.db.entity.*
import com.gorden.dayexam.db.entity.Paper
import com.gorden.dayexam.db.entity.question.*
import com.gorden.dayexam.executor.AppExecutors

@Database(
    entities = [DContext::class, Course::class, Book::class, Paper::class, Question::class,
        StudyStatus::class, Content::class, Element::class,
               StudyRecord::class, Config::class],
    version = 1
)
@TypeConverters(
    DateConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dContextDao(): DContextDao
    abstract fun courseDao(): CourseDao
    abstract fun bookDao(): BookDao
    abstract fun paperDao(): PaperDao
    abstract fun questionDao(): QuestionDao
    abstract fun studyStatusDao(): StudyStatusDao
    abstract fun elementDao(): ElementDao
    abstract fun contentDao(): ContentDao
    abstract fun studyRecordDao(): StudyRecordDao
    abstract fun configDao(): ConfigDao

    val isDatabaseCreated = MutableLiveData<Boolean>()
    val isDatabaseOpened = MutableLiveData<Boolean>()


    private fun setDatabaseCreated() {
        isDatabaseCreated.postValue(true)
    }

    private fun setDatabaseOpened() {
        isDatabaseOpened.postValue(true)
    }

    companion object {
        const val DATABASE_NAME = "exam-db"

        private var sInstance: AppDatabase? = null
        fun getInstance(
            context: Context,
            executors: AppExecutors
        ): AppDatabase {
            if (sInstance == null) {
                synchronized(AppDatabase::class.java) {
                    if (sInstance == null) {
                        sInstance = buildDatabase(context.applicationContext, executors)
                    }
                }
            }
            return sInstance!!
        }

        /**
         * Build the database. [Builder.build] only sets up the database configuration and
         * creates a new instance of the database.
         * The SQLite database is only created when it's accessed for the first time.
         */
        private fun buildDatabase(
            appContext: Context,
            executors: AppExecutors
        ): AppDatabase {
            return Room.databaseBuilder(appContext, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        getInstance(appContext, executors).setDatabaseCreated()
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        getInstance(appContext, executors).setDatabaseOpened()
                    }
                })
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }
    }
}