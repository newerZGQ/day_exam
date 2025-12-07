package com.gorden.dayexam.parser

import androidx.preference.PreferenceManager
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.AiRepository
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.utils.SharedPreferenceUtil
import com.google.gson.Gson
import com.gorden.dayexam.repository.AiNoApiKeyException
import com.gorden.dayexam.utils.FileUtils
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream

object AiPaperParser {

    /**
     * Check if a paper already exists in the database by its file hash.
     * 
     * @param filePath The absolute path to the document file to check
     * @return true if the paper already exists, false otherwise
     */
    fun checkExist(filePath: String): Boolean {
        val fileHash = FileUtils.generateHash(filePath)
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
    private const val MAX_CHUNK_SIZE = 4000

    suspend fun parseFromFile(filePath: String): Result<Unit> {
        val file = File(filePath)
        if (!file.exists()) {
            return Result.failure(IllegalArgumentException("File does not exist: $filePath"))
        }

        // Generate hash from file path
        val fileHash = FileUtils.generateHash(filePath)
        ParserContext.prepare(fileHash)

        // Extract text content from document
        val documentText = try {
            extractTextFromDocument(filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }

        // Get API keys and selected model
        val context = ContextHolder.application

        // CustomListPreference设置的值保存在PreferenceManager.getDefaultSharedPreferences(context)中
        val selectedModel = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.ai_model_key), "deepseek")

        // Split text into overlapping chunks
        val chunks = splitTextIntoOverlappingChunks(documentText)
        val allQuestions = mutableListOf<QuestionDetail>()
        var lastError: Throwable? = null

        for (chunk in chunks) {
            val result = when (selectedModel) {
                "gemini" -> {
                    val geminiKey = SharedPreferenceUtil.getString(context.getString(R.string.gemini_api_key))
                    if (geminiKey.isNotEmpty()) {
                        AiRepository.callGeminiApi(geminiKey, chunk)
                    } else {
                        Result.failure(AiNoApiKeyException("Gemini API Key is missing. Please set it in Settings."))
                    }
                }
                else -> {
                    val deepseekKey = SharedPreferenceUtil.getString(context.getString(R.string.deepseek_api_key))
                    if (deepseekKey.isNotEmpty()) {
                        AiRepository.callDeepseekApi(deepseekKey, chunk)
                    } else {
                        Result.failure(AiNoApiKeyException("DeepSeek API Key is missing. Please set it in Settings."))
                    }
                }
            }

            result.fold(
                onSuccess = { questions ->
                    if (questions.isNotEmpty()) {
                        allQuestions.addAll(questions)
                    }
                },
                onFailure = { e ->
                    lastError = e
                    e.printStackTrace()
                    // Continue to next chunk even if one fails
                }
            )
        }

        if (allQuestions.isEmpty()) {
            val error = lastError ?: RuntimeException(ContextHolder.application.getString(R.string.ai_parse_failed_no_questions))
            return Result.failure(error)
        }

        // Deduplicate questions
        val uniqueQuestions = deduplicateQuestions(allQuestions)

        // Save all data after parsing is complete
        DataRepository.insertPaperWithHash(
            title = file.nameWithoutExtension,
            path = filePath,
            hash = fileHash,
            questionCount = uniqueQuestions.size
        )
        // 2. Save questions to JSON file
        saveQuestionsToCache(uniqueQuestions)
        return Result.success(Unit)
    }

    private const val CHUNK_LINES = 80
    private const val OVERLAP_LINES = 20

    private fun splitTextIntoOverlappingChunks(text: String): List<String> {
        val lines = text.lines()
        val chunks = mutableListOf<String>()
        var startLine = 0

        while (startLine < lines.size) {
            val endLine = minOf(startLine + CHUNK_LINES, lines.size)
            val chunkLines = lines.subList(startLine, endLine)
            val chunkText = chunkLines.joinToString("\n")
            
            if (chunkText.isNotBlank()) {
                chunks.add(chunkText)
            }

            if (endLine == lines.size) {
                break
            }
            startLine += (CHUNK_LINES - OVERLAP_LINES)
        }
        return chunks
    }

    private fun deduplicateQuestions(questions: List<QuestionDetail>): List<QuestionDetail> {
        val uniqueList = mutableListOf<QuestionDetail>()
        for (question in questions) {
            if (uniqueList.none { isSameQuestion(it, question) }) {
                uniqueList.add(question)
            }
        }
        return uniqueList
    }

    private fun isSameQuestion(q1: QuestionDetail, q2: QuestionDetail): Boolean {
        if (q1.type != q2.type) return false
        
        // Compare body content (text only for simplicity, or full structure)
        if (!elementsEquals(q1.body, q2.body)) return false
        
        // Compare options
        if (q1.options.size != q2.options.size) return false
        for (i in q1.options.indices) {
             if (!elementsEquals(q1.options[i].element, q2.options[i].element)) return false
        }
        
        // Compare answer
        if (!answerEquals(q1.answer, q2.answer)) return false

        return true
    }

    private fun elementsEquals(list1: List<com.gorden.dayexam.repository.model.Element>, list2: List<com.gorden.dayexam.repository.model.Element>): Boolean {
        if (list1.size != list2.size) return false
        for (i in list1.indices) {
            val e1 = list1[i]
            val e2 = list2[i]
            if (e1.elementType != e2.elementType) return false
            if (e1.content != e2.content) return false
        }
        return true
    }
    
    private fun answerEquals(a1: com.gorden.dayexam.repository.model.Answer, a2: com.gorden.dayexam.repository.model.Answer): Boolean {
        if (a1.tfAnswer != a2.tfAnswer) return false
        if (a1.optionAnswer != a2.optionAnswer) return false
        if (!elementsEquals(a1.commonAnswer, a2.commonAnswer)) return false
        return true
    }

    /**
     * Extract text content from a Word document
     */
    private fun extractTextFromDocument(filePath: String): String {
        return try {
            val file = File(filePath)
            if (filePath.endsWith(".txt", ignoreCase = true)) {
                 file.readText()
            } else {
                val textBuilder = StringBuilder()
                FileInputStream(file).use { inputStream ->
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
                textBuilder.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
             throw RuntimeException(ContextHolder.application.getString(R.string.ai_extract_text_failed) + e.message, e)
        }
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