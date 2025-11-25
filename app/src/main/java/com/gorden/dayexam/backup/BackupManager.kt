@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.gorden.dayexam.backup

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.gorden.dayexam.BuildConfig
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.R
import com.gorden.dayexam.db.AppDatabase
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.parser.image.ImageCacheManager
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.utils.FileUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import net.lingala.zip4j.ZipFile
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object BackupManager {


    private const val SDCARD_BACKUP_FOLDER_NAME = "backup"
    private const val SDCARD_BACKUP_FOR_RECOVER_NAME = "recover_temp"
    private val CACHE_IMAGE_FOLDER =
        ContextHolder.application.cacheDir.path + File.separator + ImageCacheManager.PARSED_IMAGE_FOLDER_NAME
    private val EXAM_DATABASE_PATH =
        ContextHolder.application.getDatabasePath(AppDatabase.DATABASE_NAME).absolutePath
    private val DATABASE_FOLDER =
        ContextHolder.application.dataDir.absolutePath + File.separator + "databases"
    private val BACKUP_TEMP_FOLDER =
        ContextHolder.application.cacheDir.path + File.separator + "backup_temp_extract"
    private val TEMP_BACKUP_INFO_PATH =
        ContextHolder.application.cacheDir.path + File.separator + "backup_info.json"

    fun backup() {
        //动态申请权限
        ContextHolder.currentActivity()?.let {
            if (ContextCompat.checkSelfPermission(
                    it, Manifest.permission
                        .WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
                )
            } else {
                AppExecutors.diskIO().execute {
                    LiveEventBus.get(EventKey.START_PROGRESS_BAR, Int::class.java).post(0)
                    backupTo(SDCARD_BACKUP_FOLDER_NAME)
                    AppExecutors.mainThread().execute {
                        val context = ContextHolder.application
                        LiveEventBus.get(EventKey.END_PROGRESS_BAR, Int::class.java).post(0)
                        Toast.makeText(context,
                            context.resources.getString(R.string.backup_success),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun recover(backupFile: File, listener: BackupListener) {
        backupCurrent()
        AppExecutors.diskIO().execute {
            if (backupFile.exists() && backupFile.name.endsWith(".zip")) {
                try {
                    checkBackupTemp()
                    val zipFile = ZipFile(backupFile.absolutePath)
                    zipFile.extractAll(BACKUP_TEMP_FOLDER)
                    val tempFile = File(BACKUP_TEMP_FOLDER)
                    tempFile.listFiles().forEach {
                        if (it.name.equals(AppDatabase.DATABASE_NAME)) {
                            val success = it.renameTo(File(EXAM_DATABASE_PATH))
                            if (success) {
                                val shmFile =
                                    File(DATABASE_FOLDER + File.separator + AppDatabase.DATABASE_NAME + "-shm")
                                if (shmFile.exists()) {
                                    shmFile.delete()
                                }
                                val walFile =
                                    File(DATABASE_FOLDER + File.separator + AppDatabase.DATABASE_NAME + "-wal")
                                if (walFile.exists()) {
                                    walFile.delete()
                                }
                            } else {
                                AppExecutors.mainThread().execute {
                                    listener.onRecoverEnd(false, "恢复数据库异常")
                                }
                            }
                        } else if (it.name.equals(ImageCacheManager.PARSED_IMAGE_FOLDER_NAME) && it.isDirectory) {
                            val imageCache = File(CACHE_IMAGE_FOLDER)
                            if (imageCache.exists()) {
                                FileUtils.deleteDir(imageCache)
                            }
                            val success = it.renameTo(File(CACHE_IMAGE_FOLDER))
                            if (!success) {
                                AppExecutors.mainThread().execute {
                                    listener.onRecoverEnd(false, "恢复图片异常")
                                }
                            }
                        }
                    }
                    AppExecutors.mainThread().execute {
                        listener.onRecoverEnd(true, "")
                    }
                } catch (e: java.lang.Exception) {
                    AppExecutors.mainThread().execute {
                        listener.onRecoverEnd(false, "解析过程抛出异常")
                    }
                }

            } else {
                AppExecutors.mainThread().execute {
                    listener.onRecoverEnd(false, "文件格式出现问题")
                }
            }
        }
    }

    fun getAllBackupFiles(): List<File> {
        val externalPath = ContextHolder.application.getExternalFilesDir(null)?.absolutePath
        val parentPath = externalPath + File.separator + SDCARD_BACKUP_FOLDER_NAME
        val folder = File(parentPath)
        if (!folder.exists() || !folder.isDirectory) {
            return listOf()
        }
        return folder.listFiles().filter {
            it.name.endsWith(".zip")
        }.sortedBy {
            it.name
        }
    }

    private fun deleteEarliestBackup() {
        try {
            val backups = getAllBackupFiles()
            val maxSize = getMaxBackupSize()
            if (backups.size <= maxSize) {
                return
            }
            if (backups.size > maxSize) {
                for (i in 0..(backups.size - maxSize)) {
                    if (backups[i].exists()) {
                        backups[i].delete()
                    }
                }
            }
            val earliestZipFile = backups[0]
            if (earliestZipFile.exists() && earliestZipFile.isFile) {
                earliestZipFile.delete()
            }
        } catch (e: Exception) {

        }
    }

    private fun getMaxBackupSize(): Int {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(ContextHolder.currentActivity())
        val maxKey = ContextHolder.application.resources.getString(R.string.max_backup_size_key)
        return sharedPreferences.getInt(maxKey, 5)
    }

    private fun checkBackupTemp() {
        val tempFolder = File(BACKUP_TEMP_FOLDER)
        if (tempFolder.exists()) {
            FileUtils.deleteDirContent(tempFolder)
        } else {
            tempFolder.mkdir()
        }
    }

    private fun backupTo(path: String) {
        try {
            DataRepository.checkPoint()
            deleteEarliestBackup()
            val externalPath =
                ContextHolder.application.getExternalFilesDir(null)?.absolutePath
            val parentPath = externalPath + File.separator + path
            val folder = File(parentPath)
            if (!folder.exists() || !folder.isDirectory) {
                folder.mkdir()
            }
            val date = Date()
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val fileName = formatter.format(date)
            val destination = parentPath + File.separator + fileName + ".zip"
            val backInfo = JSONObject()
            backInfo.put("version_code", BuildConfig.VERSION_CODE)
            backInfo.put("version_name", BuildConfig.VERSION_NAME)
            val backInfoString = backInfo.toString()
            FileUtils.writeTo(TEMP_BACKUP_INFO_PATH, backInfoString)
            FileUtils.zip(listOf(EXAM_DATABASE_PATH, CACHE_IMAGE_FOLDER, TEMP_BACKUP_INFO_PATH), destination)
            val backupInfoFile = File(TEMP_BACKUP_INFO_PATH)
            if (backupInfoFile.exists()) {
                backupInfoFile.delete()
            }
        } catch (e: Exception) {

        }
    }

    // 恢复备份前现将当前的内容备份到`SDCARD_BACKUP_FOR_RECOVER_NAME`，以防出错无法恢复
    private fun backupCurrent() {
        AppExecutors.diskIO().execute {
            val externalPath = ContextHolder.application.getExternalFilesDir(null)?.absolutePath
            val recoverTemp = externalPath + File.separator + SDCARD_BACKUP_FOR_RECOVER_NAME
            val file = File(recoverTemp)
            if (file.exists() && file.isDirectory) {
                FileUtils.deleteDirContent(file)
            } else {
                file.mkdir()
            }
            backupTo(SDCARD_BACKUP_FOR_RECOVER_NAME)
        }
    }

    interface BackupListener {
        fun onRecoverEnd(success: Boolean, msg: String)
    }

}