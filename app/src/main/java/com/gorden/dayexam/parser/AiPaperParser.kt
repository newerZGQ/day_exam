package com.gorden.dayexam.parser

import android.util.Log
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
    private const val TAG = "AiPaperParser"
    private const val INPUT_TYPE_EXCEL = "excel"
    private const val INPUT_TYPE_DOCUMENT = "document"

    /**
     * Check if a paper already exists in the database by its file hash.
     * 
     * @param filePath The absolute path to the document file to check
     * @return true if the paper already exists, false otherwise
     */
    fun checkExist(filePath: String): Boolean {
        val fileHash = FileUtils.generateHash(filePath)
        if (fileHash.isBlank()) {
            Log.e(TAG, "checkExist failed: blank file hash")
            return false
        }
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
    private const val MAX_CHUNK_SIZE = 500
    private data class AiParseInput(
        val type: String,
        val sourceSize: Int,
        val chunks: List<String>
    )

    suspend fun parseFromFile(filePath: String, progressCallback: (suspend (Int, Int) -> Unit)? = null): Result<Unit> {
        val file = File(filePath)
        if (!file.exists()) {
            return Result.failure(IllegalArgumentException("File does not exist: $filePath"))
        }

        // Generate hash from file path
        val fileHash = FileUtils.generateHash(filePath)
        if (fileHash.isBlank()) {
            Log.e(TAG, "parseFromFile failed: blank file hash")
            return Result.failure(IllegalStateException("Failed to generate file hash"))
        }
        ParserContext.prepare(fileHash)

        // Extract text content from document
        val parseInput = try {
            buildAiParseInput(filePath)
        } catch (e: Exception) {
            Log.e(TAG, "buildAiParseInput failed", e)
            return Result.failure(e)
        }

        // Get API keys and selected model
        val context = ContextHolder.application

        // CustomListPreference设置的值保存在PreferenceManager.getDefaultSharedPreferences(context)中
        val selectedModel = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.ai_model_key), "deepseek")

        val chunks = parseInput.chunks
        Log.d(TAG, "parse start type=${parseInput.type} model=$selectedModel sourceSize=${parseInput.sourceSize} chunks=${chunks.size}")
        val allQuestions = mutableListOf<QuestionDetail>()
        var lastError: Throwable? = null
        // notify initial progress 0/N
        progressCallback?.invoke(0, chunks.size)

        for ((idx, chunk) in chunks.withIndex()) {
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
                    Log.e(TAG, "chunk ${idx + 1}/${chunks.size} failed", e)
                    // Continue to next chunk even if one fails
                }
            )
            // After processing this chunk, report progress using the loop index (idx)
            val processedChunks = idx + 1
            progressCallback?.invoke(processedChunks, chunks.size)
        }

        if (allQuestions.isEmpty()) {
            val error = lastError ?: RuntimeException(ContextHolder.application.getString(R.string.ai_parse_failed_no_questions))
            Log.e(TAG, "parse finished with no questions", error)
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
        Log.d(TAG, "parse success questions=${uniqueQuestions.size} raw=${allQuestions.size}")
        return Result.success(Unit)
    }

    private const val CHUNK_LINES = 20
    private const val OVERLAP_LINES = 5

    private fun buildAiParseInput(filePath: String): AiParseInput {
        return if (isExcelFile(filePath)) {
            buildExcelAiParseInput(filePath)
        } else {
            buildDocumentAiParseInput(filePath)
        }
    }

    private fun buildExcelAiParseInput(filePath: String): AiParseInput {
        val blocks = ExcelTextExtractor.extractQuestionBlocks(filePath)
        val chunks = splitExcelBlocksIntoChunks(blocks)
        val sourceSize = blocks.sumOf { it.length }
        return AiParseInput(
            type = INPUT_TYPE_EXCEL,
            sourceSize = sourceSize,
            chunks = chunks
        )
    }

    private fun buildDocumentAiParseInput(filePath: String): AiParseInput {
        val documentText = extractTextFromDocument(filePath)
        val chunks = splitDocumentTextIntoChunks(documentText)
        return AiParseInput(
            type = INPUT_TYPE_DOCUMENT,
            sourceSize = documentText.length,
            chunks = chunks
        )
    }

    private fun splitDocumentTextIntoChunks(text: String): List<String> {
        val lines = normalizeDocumentLines(text)
        if (lines.isEmpty()) {
            Log.w(TAG, "splitDocumentTextIntoChunks found no content")
            return emptyList()
        }
        val chunks = mutableListOf<String>()
        var startLine = 0

        while (startLine < lines.size) {
            val chunkLines = mutableListOf<String>()
            var currentLength = 0
            var endLine = startLine

            while (endLine < lines.size && chunkLines.size < CHUNK_LINES) {
                val line = lines[endLine]
                val extraLength = if (chunkLines.isEmpty()) line.length else line.length + 1
                if (chunkLines.isNotEmpty() && currentLength + extraLength > MAX_CHUNK_SIZE) {
                    break
                }
                chunkLines.add(line)
                currentLength += extraLength
                endLine++
            }

            if (chunkLines.isEmpty()) {
                // Keep at least one line moving forward even when a single line exceeds MAX_CHUNK_SIZE.
                chunkLines.add(lines[startLine])
                endLine = startLine + 1
            }

            chunks.add(chunkLines.joinToString("\n"))

            if (endLine >= lines.size) {
                break
            }
            startLine = (endLine - OVERLAP_LINES).coerceAtLeast(startLine + 1)
        }
        return chunks
    }

    private fun splitExcelBlocksIntoChunks(blocks: List<String>): List<String> {
        if (blocks.isEmpty()) {
            Log.w(TAG, "splitExcelBlocksIntoChunks found no blocks")
            return emptyList()
        }
        val chunks = mutableListOf<String>()
        var currentBlocks = mutableListOf<String>()
        var currentLength = 0

        fun flush() {
            if (currentBlocks.isNotEmpty()) {
                chunks.add(currentBlocks.joinToString("\n\n"))
                currentBlocks = mutableListOf()
                currentLength = 0
            }
        }

        for (block in blocks) {
            val trimmed = block.trim()
            if (trimmed.isBlank()) {
                continue
            }
            val extraLength = if (currentBlocks.isEmpty()) trimmed.length else trimmed.length + 2
            if (currentBlocks.isNotEmpty() && currentLength + extraLength > MAX_CHUNK_SIZE) {
                flush()
            }

            if (trimmed.length > MAX_CHUNK_SIZE) {
                flush()
                chunks.add(trimmed)
                continue
            }

            currentBlocks.add(trimmed)
            currentLength += if (currentBlocks.size == 1) trimmed.length else extraLength
        }
        flush()
        return chunks
    }

    private fun isExcelFile(filePath: String): Boolean {
        return filePath.endsWith(".xls", ignoreCase = true) ||
            filePath.endsWith(".xlsx", ignoreCase = true)
    }

    private fun normalizeDocumentLines(text: String): List<String> {
        return text.lines()
            .flatMap { splitLongLine(it) }
            .filter { it.isNotBlank() }
    }

    private fun splitLongLine(line: String): List<String> {
        val trimmed = line.trim()
        if (trimmed.isBlank()) {
            return emptyList()
        }
        if (trimmed.length <= MAX_CHUNK_SIZE) {
            return listOf(trimmed)
        }

        val parts = mutableListOf<String>()
        val current = StringBuilder()
        val words = trimmed.split(Regex("\\s+"))

        for (word in words) {
            if (word.length > MAX_CHUNK_SIZE) {
                if (current.isNotEmpty()) {
                    parts.add(current.toString())
                    current.clear()
                }
                parts.addAll(word.chunked(MAX_CHUNK_SIZE))
                continue
            }

            val extraLength = if (current.isEmpty()) word.length else word.length + 1
            if (current.isNotEmpty() && current.length + extraLength > MAX_CHUNK_SIZE) {
                parts.add(current.toString())
                current.clear()
            }
            if (current.isNotEmpty()) {
                current.append(' ')
            }
            current.append(word)
        }

        if (current.isNotEmpty()) {
            parts.add(current.toString())
        }
        return parts
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
            } else if (filePath.endsWith(".xls", ignoreCase = true) || filePath.endsWith(".xlsx", ignoreCase = true)) {
                ExcelTextExtractor.extractText(filePath)
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
            Log.e(TAG, "extractTextFromDocument exception", e)
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
            Log.e(TAG, "saveQuestionsToCache failed", e)
            throw RuntimeException(ContextHolder.application.getString(R.string.ai_save_cache_failed) + e.message, e)
        }
    }
}
