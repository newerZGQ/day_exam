package com.gorden.dayexam.utils

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.stream.Stream

object FileUtils {

    /**
     * Generate a hash string from the file content at the given path
     */
    fun generateHash(filePath: String): String {
        val md = MessageDigest.getInstance("MD5")
        return try {
            val file = File(filePath)
            if (!file.exists()) return ""
            FileInputStream(file).use {
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (it.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            val digest = md.digest()
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Generate a hash string from the input string
     */
    fun generateHash(input: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }


    fun zip(files: List<String>, destinationFilePath: String) {
        try {
            val zipFile = ZipFile(destinationFilePath)
            files.forEach {
                val targetFile = File(it)
                if (targetFile.isFile) {
                    zipFile.addFile(targetFile)
                } else if (targetFile.isDirectory) {
                    zipFile.addFolder(targetFile)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unzip(targetZipFilePath: String, destinationFolderPath: String) {
        try {
            val zipFile = ZipFile(targetZipFilePath)
            zipFile.extractAll(destinationFolderPath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 删除文件夹内容，不删除文件夹本身
    fun deleteDirContent(dir: File): Boolean {
        return deleteDirContent(dir, 0)
    }

    private fun deleteDirContent(dir: File, level: Int): Boolean {
        if (dir.isDirectory) {
            val children = dir.list()
            children.forEach {
                val success = deleteDirContent(File(dir, it), level + 1)
                if (!success) {
                    return false
                }
            }
        }
        return if (level == 0) {
            true
        } else {
            dir.delete()
        }
    }

    // 删除文件夹，包括文件夹本身
    fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.list()
            children.forEach {
                val success = deleteDir(File(dir, it))
                if (!success) {
                    return false
                }
            }
        }
        return dir.delete()
    }

    fun writeTo(path: String, content: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            val writer = file.writer()
            writer.write(content)
            writer.close()
        } catch (e: Exception) {

        }
    }
}