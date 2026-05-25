package com.gorden.dayexam

import android.app.Application
import android.util.Log
import com.gorden.dayexam.db.AppDatabase
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.repository.DataRepository
import com.jeremyliao.liveeventbus.LiveEventBus
import java.io.File

class BasicApp: Application() {

    override fun onCreate() {
        super.onCreate()
        ContextHolder.application = this
        LiveEventBus
            .config()
            .setContext(this)
        DataRepository.init(AppDatabase.getInstance(this, AppExecutors))
        migrateFromCacheToFiles()
    }

    private fun migrateFromCacheToFiles() {
        try {
            val cachePrefix = cacheDir.absolutePath
            val filesPrefix = filesDir.absolutePath

            val oldImported = File(cacheDir, "imported_papers")
            val newImported = File(filesDir, "imported_papers")
            if (oldImported.exists() && !newImported.exists()) {
                oldImported.renameTo(newImported)
            }

            cacheDir.listFiles()
                ?.filter { it.isDirectory && it.name.matches(Regex("[a-f0-9]{32}")) }
                ?.forEach { hashDir ->
                    val dest = File(filesDir, hashDir.name)
                    if (!dest.exists()) {
                        hashDir.renameTo(dest)
                    }
                }

            // Update path in database: replace cache prefix with files prefix
            AppDatabase.getInstance(this, AppExecutors).openHelper.writableDatabase.execSQL(
                "UPDATE paper SET path = REPLACE(path, ?, ?) WHERE path LIKE ?",
                arrayOf(cachePrefix, filesPrefix, "$cachePrefix%")
            )
        } catch (e: Exception) {
            Log.e("BasicApp", "migration from cache to files failed", e)
        }
    }
}