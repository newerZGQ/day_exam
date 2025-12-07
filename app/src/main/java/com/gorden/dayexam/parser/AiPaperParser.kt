package com.gorden.dayexam.parser

import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.AiRepository
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.utils.SharedPreferenceUtil
import com.google.gson.Gson
import com.gorden.dayexam.repository.AiNoApiKeyException
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object AiPaperParser {

    /**
     * Check if a paper already exists in the database by its file hash.
     * 
     * @param filePath The absolute path to the document file to check
     * @return true if the paper already exists, false otherwise
     */
    fun checkExist(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists()) {
            return false
        }
        
        val fileHash = generateHash(filePath)
        val existingPaper = DataRepository.getPaperByHash(fileHash)
        
        return existingPaper != null
    }

    /**
     * Parse a file using AI, save PaperInfo to database,
     * and cache questions as JSON.
     * 
     * @param filePath The absolute path to the document file to parse
     * @throws IllegalArgumentException if file does not exist
     * @throws IllegalStateException if no API key is configured
     * @throws RuntimeException if parsing fails
     */
    suspend fun parseFromFile(filePath: String): Result<Unit> {
        val file = File(filePath)
        if (!file.exists()) {
            return Result.failure(IllegalArgumentException("File does not exist: $filePath"))
        }

        // Generate hash from file path
        val fileHash = generateHash(filePath)
        ParserContext.prepare(fileHash)

        // Extract text content from document
        val documentText = try {
            extractTextFromDocument(filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }

        // Get API keys from SharedPreferences
        val context = ContextHolder.application
        val geminiKey = SharedPreferenceUtil.getString(context.getString(R.string.gemini_api_key))
        val deepseekKey = SharedPreferenceUtil.getString(context.getString(R.string.deepseek_api_key))

        // Call AI API to parse questions
        val result = when {
            geminiKey.isNotEmpty() -> {
                callAiApiWithDocument(geminiKey, documentText, useGemini = true)
            }
            deepseekKey.isNotEmpty() -> {
                callAiApiWithDocument(deepseekKey, documentText, useGemini = false)
            }
            else -> {
                Result.failure(AiNoApiKeyException(ContextHolder.application.getString(R.string.ai_api_key_missing)))
            }
        }

        return result.fold(
            onSuccess = { questionDetails ->
                if (questionDetails.isEmpty()) {
                    return@fold Result.failure(RuntimeException(ContextHolder.application.getString(R.string.ai_parse_failed_no_questions)))
                }
                // Save all data after parsing is complete
                DataRepository.insertPaperWithHash(
                    title = file.nameWithoutExtension,
                    path = filePath,
                    hash = fileHash,
                    questionCount = questionDetails.size
                )
                // 2. Save questions to JSON file
                saveQuestionsToCache(questionDetails)
                Result.success(Unit)
            },
            onFailure = { e ->
                Result.failure(e)
            }
        )
    }

    /**
     * Extract text content from a Word document
     */
    private fun extractTextFromDocument(filePath: String): String {
        val textBuilder = StringBuilder()
        
        try {
            FileInputStream(File(filePath)).use { inputStream ->
                val document = XWPFDocument(inputStream)
                
                // Extract text from all paragraphs
                document.paragraphs.forEach { paragraph ->
                    if (paragraph.text.isNotBlank()) {
                        textBuilder.append(paragraph.text)
                        textBuilder.append("\n")
                    }
                }
                
                // Extract text from tables if any
                document.tables.forEach { table ->
                    table.rows.forEach { row ->
                        row.tableCells.forEach { cell ->
                            if (cell.text.isNotBlank()) {
                                textBuilder.append(cell.text)
                                textBuilder.append("\n")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(ContextHolder.application.getString(R.string.ai_extract_text_failed) + e.message, e)
        }
        
        return textBuilder.toString()
    }

    /**
     * Call AI API with document text to parse questions
     */
    private suspend fun callAiApiWithDocument(
        apiKey: String,
        documentText: String,
        useGemini: Boolean
    ): Result<List<QuestionDetail>> {
        return if (useGemini) {
            AiRepository.callGeminiApi(apiKey, documentText)
        } else {
            AiRepository.callDeepseekApi(apiKey, documentText)
        }
    }

    /**
     * Generate a hash string from the input string
     */
    private fun generateHash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Save questions list to JSON file in cache directory
     */
    private fun saveQuestionsToCache(questions: List<QuestionDetail>) {
        try {
            val gson = Gson()
            ParserContext.saveQuestions(gson.toJson(questions))
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException(ContextHolder.application.getString(R.string.ai_save_cache_failed) + e.message, e)
        }
    }
}