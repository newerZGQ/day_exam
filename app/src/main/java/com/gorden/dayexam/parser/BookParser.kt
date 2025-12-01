package com.gorden.dayexam.parser

import com.gorden.dayexam.db.entity.question.*
import com.gorden.dayexam.model.QuestionType
import com.gorden.dayexam.parser.image.ImageCacheManager
import com.gorden.dayexam.parser.model.PAnswer
import com.gorden.dayexam.parser.model.PBody
import com.gorden.dayexam.parser.model.POptionItem
import com.gorden.dayexam.parser.model.PQuestion
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.question.Element
import com.gorden.dayexam.utils.BookUtils
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import java.io.InputStream
import java.lang.StringBuilder

object BookParser {

    private var curPaperId: Int = 1
    private var curBookId: Int = 0
    private var timeStamp: Long = 0
    private val cacheImageData = mutableMapOf<String, ByteArray>()

    fun parse(inputStream: InputStream, bookId: Int, paperId: Int): List<PQuestion> {
        setContextValue(paperId, bookId)
        timeStamp = System.currentTimeMillis()
        cacheImageData.clear()
        val result = mutableListOf<PQuestion>()
        try {
            val document = XWPFDocument(inputStream)
            val splitTitleSeparatorResult = splitByTitleSeparator(document.paragraphs)
            splitTitleSeparatorResult.forEach { questionParas ->
                val questionUnits = splitBySeparator(questionParas)
                val question = parseParagraphToQuestion(questionUnits)
                question?.let {
                    result.add(question)
                }
            }
        } catch (e: Exception) {

        }
        DataRepository.insertPQuestion(result, curPaperId)
        DataRepository.increaseContentVersion()
        cacheImageData.forEach { (s, bytes) ->
            ImageCacheManager.save(s, bytes)
        }
        return result
    }

    private fun setContextValue(paperId: Int, bookId: Int) {
        this.curPaperId = paperId
        if (curBookId != bookId) {
            this.curBookId = bookId
            ImageCacheManager.setCacheFolder(curBookId.toString())
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

    private fun parseParagraphToQuestion(paras: List<List<XWPFParagraph>>): PQuestion? {
        var body = PBody(listOf())
        var options = mutableListOf<POptionItem>()
        var answer = PAnswer(listOf())
        var type = QuestionType.ERROR_TYPE
        paras.forEach {
            if (it.isNotEmpty()) {
                val text = it[0].text
                when {
                    isQuestionSeparator(text) -> {
                        type = parseType(it)
                        body = parseBody(it)
                    }
                    text.startsWith(ParserConstants.OPTION_SEPARATOR) ||
                            text.lowercase().startsWith(ParserConstants.OPTION_SEPARATOR_EN) -> {
                        options.add(parseOption(it))
                    }
                    text.startsWith(ParserConstants.ANSWER_SEPARATOR) ||
                            text.lowercase().startsWith(ParserConstants.ANSWER_SEPARATOR_EN) -> {
                        answer = parseAnswer(it)
                    }
                }
            }
        }
        return if (curPaperId != -1) {
            PQuestion(type, body, answer, options)
        } else {
            // TODO 抛异常告警
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

    private fun parseBody(paras: List<XWPFParagraph>): PBody {
        return PBody(parseParagraphToElement(paras))
    }

    private fun parseOption(paras: List<XWPFParagraph>): POptionItem {
        return POptionItem(parseParagraphToElement(paras))
    }

    private fun parseAnswer(paras: List<XWPFParagraph>): PAnswer {
        return PAnswer(parseParagraphToElement(paras))
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
                        cacheImageData[imageName] = picture.pictureData.data
                        val element = Element(Element.PICTURE, imageName, 0, elementPosition++)
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

