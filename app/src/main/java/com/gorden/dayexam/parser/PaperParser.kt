package com.gorden.dayexam.parser

import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.repository.model.QuestionType
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.Element
import com.gorden.dayexam.repository.model.OptionItems
import com.gorden.dayexam.repository.model.QuestionDetail
import com.google.gson.Gson
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.StringBuilder
import java.security.MessageDigest

object PaperParser {

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
     * Parse a file from the given path, save PaperInfo to database,
     * and cache questions as JSON with images in a hash-based folder structure.
     * 
     * @param filePath The absolute path to the document file to parse
     */
    fun parseFromFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: $filePath")
        }
        
        // Generate hash from file path
        val fileHash = generateHash(filePath)
        ParserContext.prepare(fileHash)
        // Parse the document to get questions and image data
        val (questionDetails, imageHashToData) = parseDocument(filePath)
        
        // Save all data after parsing is complete
        // 1. Save cached images with proper path structure
        imageHashToData.forEach { (imageHash, bytes) ->
            ParserContext.saveImage(imageHash, bytes)
        }
        
        // 2. Save PaperInfo to database
        DataRepository.insertPaperWithHash(
            title = file.nameWithoutExtension,
            desc = "",
            path = filePath,
            hash = fileHash,
            questionCount = questionDetails.size
        )
        
        // 3. Save questions to JSON file
        saveQuestionsToCache(questionDetails)
    }

    /**
     * Parse a document file and return questions with image data.
     * This is a pure parsing method that doesn't perform any save operations.
     * 
     * @param filePath The absolute path to the document file
     * @return Pair of (questions list, image hash to data map)
     */
    private fun parseDocument(filePath: String): Pair<List<QuestionDetail>, Map<String, ByteArray>> {
        val imageHashToData = mutableMapOf<String, ByteArray>()
        val questionDetails = mutableListOf<QuestionDetail>()
        
        try {
            FileInputStream(File(filePath)).use { inputStream ->
                val document = XWPFDocument(inputStream)
                val splitTitleSeparatorResult = splitByTitleSeparator(document.paragraphs)
                splitTitleSeparatorResult.forEach { questionParas ->
                    val questionUnits = splitBySeparator(questionParas)
                    val question = parseParagraphToQuestion(questionUnits, imageHashToData)
                    question?.let {
                        questionDetails.add(question)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Failed to parse file: ${e.message}", e)
        }
        
        return Pair(questionDetails, imageHashToData)
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
     * Generate a hash string from the input string
     */
    private fun generateHash(input: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input)
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
            throw RuntimeException("Failed to save questions to cache: ${e.message}", e)
        }
    }

    // 以`&&填空 &&判断等`为分界
    private fun splitByTitleSeparator(paras: List<XWPFParagraph>): List<List<XWPFParagraph>> {
        val result = mutableListOf<List<XWPFParagraph>>()
        var curParas = mutableListOf<XWPFParagraph>()
        for (i in paras.indices) {
            val curItem = paras[i]
            if (curItem.isEmpty || curItem.runs.isEmpty()) {
                continue
            }
            if (isQuestionSeparator(curItem.text)) {
                if (curParas.isNotEmpty()) {
                    result.add(curParas)
                    curParas = mutableListOf()
                }
            }
            curParas.add(curItem)
        }
        if (curParas.isNotEmpty()) {
            result.add(curParas)
        }
        return result
    }

    private fun parseParagraphToQuestion(
        paras: List<List<XWPFParagraph>>,
        imageHashToData: MutableMap<String, ByteArray>
    ): QuestionDetail? {
        var body = listOf<Element>()
        var options = mutableListOf<OptionItems>()
        var answer = listOf<Element>()
        var type = QuestionType.ERROR_TYPE
        
        paras.forEach {
            if (it.isNotEmpty()) {
                val text = it[0].text
                when {
                    isQuestionSeparator(text) -> {
                        type = parseType(it)
                        body = parseParagraphToElement(it, imageHashToData)
                    }
                    text.startsWith(ParserConstants.OPTION_SEPARATOR) ||
                            text.lowercase().startsWith(ParserConstants.OPTION_SEPARATOR_EN) -> {
                        options.add(OptionItems(parseParagraphToElement(it, imageHashToData)))
                    }
                    text.startsWith(ParserConstants.ANSWER_SEPARATOR) ||
                            text.lowercase().startsWith(ParserConstants.ANSWER_SEPARATOR_EN) -> {
                        answer = parseParagraphToElement(it, imageHashToData)
                    }
                }
            }
        }
        
        return if (type != QuestionType.ERROR_TYPE) {
            QuestionDetail(
                type = type,
                body = body,
                options = options,
                answer = answer
            )
        } else {
            null
        }
    }

    // 以`&&`为分界
    private fun splitBySeparator(paras: List<XWPFParagraph>): List<List<XWPFParagraph>> {
        val result = mutableListOf<List<XWPFParagraph>>()
        var curParas = mutableListOf<XWPFParagraph>()
        for (item in paras) {
            if (item.isEmpty || item.runs.isEmpty()) {
                continue
            }
            if (item.text.isNotEmpty() && item.text.startsWith(ParserConstants.SEPARATOR)) {
                if (curParas.isNotEmpty()) {
                    result.add(curParas)
                    curParas = mutableListOf()
                }
            }
            curParas.add(item)
        }
        if (curParas.isNotEmpty()) {
            result.add(curParas)
        }
        return result
    }

    private fun parseType(paras: List<XWPFParagraph>): Int {
        if (paras.isEmpty()) {
            return QuestionType.ERROR_TYPE
        }
        val typeText = paras[0].text
        if (typeText.isNotEmpty()) {
            when {
                typeText.lowercase().startsWith(ParserConstants.FILL_BLANK_SEPARATOR) ||
                        typeText.lowercase().startsWith(ParserConstants.FILL_BLANK_SEPARATOR_EN) -> {
                    return QuestionType.FILL_BLANK
                }
                typeText.lowercase().startsWith(ParserConstants.TRUE_FALSE_SEPARATOR) ||
                        typeText.lowercase().startsWith(ParserConstants.TRUE_FALSE_SEPARATOR_EN) -> {
                    return QuestionType.TRUE_FALSE
                }
                typeText.lowercase().startsWith(ParserConstants.SINGLE_CHOICE_SEPARATOR) ||
                        typeText.lowercase().startsWith(ParserConstants.SINGLE_CHOICE_SEPARATOR_EN) -> {
                    return QuestionType.SINGLE_CHOICE
                }
                typeText.lowercase().startsWith(ParserConstants.MULTIPLE_CHOICE_SEPARATOR) ||
                        typeText.lowercase().startsWith(ParserConstants.MULTIPLE_CHOICE_SEPARATOR_EN) -> {
                    return QuestionType.MULTIPLE_CHOICE
                }
                typeText.lowercase().startsWith(ParserConstants.ESSAY_QUESTION_SEPARATOR) ||
                        typeText.lowercase().startsWith(ParserConstants.ESSAY_QUESTION_SEPARATOR_EN) -> {
                    return QuestionType.ESSAY_QUESTION
                }
            }
        }
        return QuestionType.ERROR_TYPE
    }

    private fun parseParagraphToElement(
        paras: List<XWPFParagraph>,
        imageHashToData: MutableMap<String, ByteArray>
    ): List<Element> {
        val result = mutableListOf<Element>()
        paras.filter {
            !it.text.startsWith(ParserConstants.SEPARATOR)
        }.forEach {
            val builder = StringBuilder()
            it.runs.forEachIndexed{ index, run ->
                if (run.text().isNotEmpty()) {
                    builder.append(run.text())
                }
                if (run.embeddedPictures.isNotEmpty()) {
                    if (builder.toString().isNotEmpty()) {
                        val element = Element(Element.TEXT, builder.toString())
                        result.add(element)
                        builder.clear()
                    }
                    run.embeddedPictures.forEach { picture ->
                        val imageHash = generateHash(picture.pictureData.data)
                        imageHashToData[imageHash] = picture.pictureData.data
                        // Store just the hash in content, path will be constructed when saving
                        val element = Element(Element.PICTURE, imageHash)
                        result.add(element)
                    }
                }
                if (index == it.runs.size - 1 && builder.toString().isNotEmpty()) {
                    val element = Element(Element.TEXT, builder.toString())
                    result.add(element)
                }
            }
        }
        return result
    }

    private fun isQuestionSeparator(text: String): Boolean {
        if (text.isEmpty()) return false
        return text.lowercase().startsWith(ParserConstants.FILL_BLANK_SEPARATOR) ||
                text.lowercase().startsWith(ParserConstants.TRUE_FALSE_SEPARATOR) ||
                text.lowercase().startsWith(ParserConstants.SINGLE_CHOICE_SEPARATOR) ||
                text.lowercase().startsWith(ParserConstants.MULTIPLE_CHOICE_SEPARATOR) ||
                text.lowercase().startsWith(ParserConstants.ESSAY_QUESTION_SEPARATOR) ||
                text.lowercase().startsWith(ParserConstants.FILL_BLANK_SEPARATOR_EN) ||
                text.lowercase().startsWith(ParserConstants.TRUE_FALSE_SEPARATOR_EN) ||
                text.lowercase().startsWith(ParserConstants.SINGLE_CHOICE_SEPARATOR_EN) ||
                text.lowercase().startsWith(ParserConstants.MULTIPLE_CHOICE_SEPARATOR_EN) ||
                text.lowercase().startsWith(ParserConstants.ESSAY_QUESTION_SEPARATOR_EN)
    }
}

object ParserContext {
    private const val PARSED_IMAGE_FOLDER = "image"
    private const val PARSED_QUESTIONS_JSON_FILE = "questions.json"

    private var paperHash: String = ""

    fun prepare(paperHash: String) {
        val cachePaperFolder = File(ContextHolder.application.cacheDir, paperHash)
        if (!cachePaperFolder.exists()) {
            cachePaperFolder.mkdirs()
        } else {
            cachePaperFolder.deleteRecursively()
            cachePaperFolder.mkdirs()
        }
        this.paperHash = paperHash
    }

    private fun getImageFile(relativePath: String): File {
        val cacheParentFolder = File(ContextHolder.application.cacheDir, paperHash)
        return File(cacheParentFolder, relativePath)
    }

    fun saveImage(hash: String, data: ByteArray) {
        val imageFolder = File(ContextHolder.application.cacheDir, "$paperHash/$PARSED_IMAGE_FOLDER")
        if (!imageFolder.exists()) {
            imageFolder.mkdirs()
        }
        val imageFile = File(ContextHolder.application.cacheDir, "$paperHash/$PARSED_IMAGE_FOLDER/$hash")
        try {
            FileOutputStream(imageFile).use { it.write(data) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveQuestions(data: String) {
        val jsonFile = File(ContextHolder.application.cacheDir, "$paperHash/$PARSED_QUESTIONS_JSON_FILE")
        try {
            FileOutputStream(jsonFile).use { it.write(data.toByteArray()) }
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

