package com.gorden.dayexam.parser

import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.model.QuestionType
import com.gorden.dayexam.repository.model.Element
import com.gorden.dayexam.repository.model.OptionItems
import com.gorden.dayexam.repository.model.PaperDetail
import com.gorden.dayexam.repository.model.PaperStudyInfo
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.utils.BookUtils
import com.gorden.dayexam.utils.ImageCacheHelper
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import java.io.File
import java.io.FileInputStream
import java.lang.StringBuilder

object BookParser {

    private var timeStamp: Long = 0
    private val cacheImageData = mutableMapOf<String, ByteArray>()
    private var currentPaperFolder: String = ""

    fun parse(paperInfo: PaperInfo): PaperDetail {
        timeStamp = System.currentTimeMillis()
        cacheImageData.clear()
        val questionDetails = mutableListOf<QuestionDetail>()
        
        // Set up image cache folder name for this paper
        currentPaperFolder = paperInfo.path.hashCode().toString()
        
        try {
            val file = File(paperInfo.path)
            FileInputStream(file).use { inputStream ->
                val document = XWPFDocument(inputStream)
                val splitTitleSeparatorResult = splitByTitleSeparator(document.paragraphs)
                splitTitleSeparatorResult.forEach { questionParas ->
                    val questionUnits = splitBySeparator(questionParas)
                    val question = parseParagraphToQuestion(questionUnits)
                    question?.let {
                        questionDetails.add(question)
                    }
                }
            }
        } catch (e: Exception) {
            // Handle exception - could log or throw
        }
        
        // Save cached images using ImageCacheHelper
        cacheImageData.forEach { (relativePath, bytes) ->
            ImageCacheHelper.save(relativePath, bytes)
        }
        
        // Create PaperStudyInfo with default values
        val studyInfo = PaperStudyInfo(
            studyCount = 0,
            lastStudyDate = null
        )
        
        // Return PaperDetail
        return PaperDetail(
            paperInfo = paperInfo,
            studyInfo = studyInfo,
            question = questionDetails
        )
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

    private fun parseParagraphToQuestion(paras: List<List<XWPFParagraph>>): QuestionDetail? {
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
                        body = parseParagraphToElement(it)
                    }
                    text.startsWith(ParserConstants.OPTION_SEPARATOR) ||
                            text.lowercase().startsWith(ParserConstants.OPTION_SEPARATOR_EN) -> {
                        options.add(OptionItems(parseParagraphToElement(it)))
                    }
                    text.startsWith(ParserConstants.ANSWER_SEPARATOR) ||
                            text.lowercase().startsWith(ParserConstants.ANSWER_SEPARATOR_EN) -> {
                        answer = parseParagraphToElement(it)
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

    private fun parseParagraphToElement(paras: List<XWPFParagraph>): List<Element> {
        val result = mutableListOf<Element>()
        var elementPosition = 1
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
                        val element = Element(Element.TEXT, builder.toString(), 0, elementPosition++)
                        result.add(element)
                        builder.clear()
                    }
                    run.embeddedPictures.forEach { picture ->
                        val imageName = BookUtils.generateImageName(picture.pictureData.fileName, timeStamp)
                        val relativePath = currentPaperFolder + File.separator + imageName
                        cacheImageData[relativePath] = picture.pictureData.data
                        val element = Element(Element.PICTURE, relativePath, 0, elementPosition++)
                        result.add(element)
                    }
                }
                if (index == it.runs.size - 1 && builder.toString().isNotEmpty()) {
                    val element = Element(Element.TEXT, builder.toString(), 0, elementPosition++)
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

