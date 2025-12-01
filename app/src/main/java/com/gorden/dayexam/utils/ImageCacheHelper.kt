package com.gorden.dayexam.utils

import com.gorden.dayexam.ContextHolder
import java.io.File
import java.io.FileOutputStream

object ImageCacheHelper {
    private const val PARSED_IMAGE_FOLDER_NAME = "parsed_image"

    fun getImageFile(relativePath: String): File {
        val cacheParentFolder = File(ContextHolder.application.cacheDir, PARSED_IMAGE_FOLDER_NAME)
        return File(cacheParentFolder, relativePath)
    }

    fun save(relativePath: String, data: ByteArray) {
        val file = getImageFile(relativePath)
        if (file.exists()) return
        
        file.parentFile?.mkdirs()
        try {
            FileOutputStream(file).use { it.write(data) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun delete(relativePath: String) {
        val file = getImageFile(relativePath)
        if (file.exists()) {
            file.delete()
        }
    }
}
