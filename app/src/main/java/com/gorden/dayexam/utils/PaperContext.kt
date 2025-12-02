package com.gorden.dayexam.utils

import com.gorden.dayexam.ContextHolder
import java.io.File
import java.io.FileOutputStream

object PaperContext {
    private const val PARSED_IMAGE_FOLDER_NAME = "parsed_image"

    fun getImageFile(relativePath: String): File {
        val cacheParentFolder = File(ContextHolder.application.cacheDir, PARSED_IMAGE_FOLDER_NAME)
        return File(cacheParentFolder, relativePath)
    }

}
