package com.gorden.dayexam.parser.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.executor.AppExecutors
import com.jakewharton.disklrucache.DiskLruCache
import java.io.*
import java.lang.Exception

object ImageCacheManager {

    const val PARSED_IMAGE_FOLDER_NAME = "parsed_image"
    private const val MAX_CACHE_SIZE = 10 * 1024 * 1024 * 1024L
    private const val VALUE_COUNT = 1
    private val CACHE_PARENT_FOLDER = ContextHolder.application.cacheDir.path + File.separator + PARSED_IMAGE_FOLDER_NAME

    private var diskLruCache: DiskLruCache? = null
    private var curCacheFolder: String = ""


    init {
        if (!File(CACHE_PARENT_FOLDER).exists()) {
            File(CACHE_PARENT_FOLDER).mkdir()
        }
    }

    // 试卷切换的时候需要更新
    fun setCacheFolder(bookId: String) {
        curCacheFolder = CACHE_PARENT_FOLDER + File.separator + bookId
        diskLruCache?.close()
        diskLruCache = DiskLruCache.open(
            File(curCacheFolder),
            0,
            VALUE_COUNT,
            MAX_CACHE_SIZE)
    }

    private fun getBitmap(imageUrl: String): Bitmap? {
        return performGet(imageUrl)
    }

    fun getImageFile(imageUrl: String): File {
        val imagePath = curCacheFolder + File.separator + imageUrl + ".0"
        return File(imagePath)
    }

    fun save(fileName: String, data: ByteArray) {
        if (diskLruCache?.get(fileName) != null) {
            return
        }
        performSave(fileName, data)
    }

    fun delete(fileName: String) {
        if (fileName.isNullOrEmpty()) {
            return
        }
        try {
            diskLruCache?.remove(fileName)
        } catch (e: IOException) {

        }
    }

    fun getAsync(url: String, callback: ImageLoaderCallback) {
        AppExecutors.diskIO().execute {
            val bitmap = getBitmap(url)
            AppExecutors.mainThread().execute {
                callback.onLoad(bitmap)
            }
        }
    }

    private fun performGet(imageUrl: String): Bitmap? {
        var inputStream: InputStream? = null
        try {
            inputStream = diskLruCache?.get(imageUrl)
                ?.getInputStream(0)
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            // TODO 异常处理
        } finally {
            inputStream?.close()
        }
        return null
    }

    private fun performSave(key: String, data: ByteArray) {
        var editor: DiskLruCache.Editor? = null
        var bufferOut: OutputStream? = null
        try {
            editor = diskLruCache?.edit(key)
            bufferOut = editor?.newOutputStream(0)
            bufferOut?.write(data)
            // 输出流写完后, 要执行提交操作
            editor?.commit()
        } catch (e: IOException) {
            // 写出失败时, 要回滚
            editor?.abort()
        } finally {
            bufferOut?.close()
            try {
                diskLruCache?.flush()
            } catch (e: Exception) {
            }
        }
    }
}

interface ImageLoaderCallback {
    fun onLoad(bitmap: Bitmap?)
}