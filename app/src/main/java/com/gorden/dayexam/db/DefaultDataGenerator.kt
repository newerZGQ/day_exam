package com.gorden.dayexam.db

import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.*
import com.gorden.dayexam.db.entity.question.*
import com.gorden.dayexam.db.entity.question.Content.Companion.ANSWER_TYPE
import com.gorden.dayexam.db.entity.question.Content.Companion.BODY_TYPE
import com.gorden.dayexam.db.entity.question.Content.Companion.OPTION_TYPE
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.model.QuestionType

object DefaultDataGenerator {

    private lateinit var mDatabase: AppDatabase

    fun init(mDatabase: AppDatabase) {
        this.mDatabase = mDatabase
    }

    fun generate() {
        generateCourse(ContextHolder.application.resources.getString(R.string.app_name))
    }

    private fun generateCourse(title: String) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val course = Course(title, "", 1)
                val courseId = mDatabase.courseDao().insert(course)

                val book = Book(ContextHolder.application.getString(R.string.default_book_for_user_name), "", courseId.toInt(), 1)
                val bookId = mDatabase.bookDao().insert(book).toInt()
                val courseStatus = StudyStatus(CourseStatus, courseId.toInt(), bookId)
                mDatabase.studyStatusDao().insert(courseStatus)

                val letterPaper = Paper(ContextHolder.application.getString(R.string.default_user_paper_name), "", bookId, 1)
                val letterPaperId = mDatabase.paperDao().insert(letterPaper).toInt()
                val letterCurQuestionId = insertALetterForUser(letterPaperId, 1)
                val letterPaperStatus = StudyStatus(PaperStatus, letterPaperId, letterCurQuestionId)
                mDatabase.studyStatusDao().insert(letterPaperStatus)

                val demoPaper = Paper(ContextHolder.application.getString(R.string.default_guide_paper_name), "", bookId, 2)
                val demoPaperId = mDatabase.paperDao().insert(demoPaper).toInt()
                val demoCurQuestionId = insertFillInBlankQuestion(demoPaperId, 1)
                insertTrueOrFalseQuestion(demoPaperId, 2)
                insertSingleChoiceQuestion(demoPaperId, 3)
                insertMultiChoiceQuestion(demoPaperId, 4)
                insertEssayQuestion(demoPaperId, 5)
                val demoPaperStatus = StudyStatus(PaperStatus, demoPaperId, demoCurQuestionId)
                mDatabase.studyStatusDao().insert(demoPaperStatus)

                val bookStatus = StudyStatus(BookStatus, bookId, letterPaperId)
                mDatabase.studyStatusDao().insert(bookStatus)

                val recycleBin = generateRecycleBin()
                mDatabase.dContextDao().insert(DContext(
                    courseId.toInt(),
                    bookId,
                    letterPaperId,
                    letterCurQuestionId,
                    recycleBin[0],
                    recycleBin[1],
                    recycleBin[2])
                )
                mDatabase.configDao().insert(
                    Config(
                        rememberMode = false,
                        focusMode = false,
                        onlyFavorite = false,
                        sortByAccuracy = false
                    )
                )
            }
        }
    }

    private fun generateRecycleBin(): IntArray {
        val recycleBin = Course("废纸篓", "找回您删除的内容", Int.MAX_VALUE)
        recycleBin.isRecycleBin = true
        val recycleBinId = mDatabase.courseDao().insert(recycleBin).toInt()
        val book = Book("已删除的试卷", "", recycleBinId, 1)
        val bookId = mDatabase.bookDao().insert(book).toInt()
        val courseStatus = StudyStatus(CourseStatus, recycleBinId, bookId)
        mDatabase.studyStatusDao().insert(courseStatus)
        val paper = Paper("回收站说明", "", bookId, 1)
        val paperId = mDatabase.paperDao().insert(paper).toInt()
        val bookStatus = StudyStatus(BookStatus, bookId, paperId)
        mDatabase.studyStatusDao().insert(bookStatus)
        val questionId = insertRecycleBinFillInBlankQuestion(paperId, 1)
        val paperStatus = StudyStatus(PaperStatus, paperId, questionId)
        mDatabase.studyStatusDao().insert(paperStatus)
        return intArrayOf(recycleBinId, bookId, paperId)
    }

    private fun generatePaper(bookId: Int): List<Paper> {
        return listOf(
            Paper("用户亲启", "", bookId, 1),
            Paper("教程", "", bookId, 2)
        )
    }

    private fun insertALetterForUser(paperId: Int, order: Int): Int {
        val question = Question(paperId, QuestionType.FILL_BLANK, order)
        val questionId = mDatabase.questionDao().insert(question).toInt()
        val body = Content(questionId, BODY_TYPE)
        val bodyId = mDatabase.contentDao().insert(body).toInt()
        val bodyElement = Element(Element.TEXT, ContextHolder.application.resources.getString(R.string.letter_for_users), bodyId, 1)
        mDatabase.elementDao().insert(bodyElement)
        val answer = Content(questionId, ANSWER_TYPE)
        val answerId = mDatabase.contentDao().insert(answer).toInt()
        val answerElement = Element(Element.TEXT, "", answerId, 1)
        mDatabase.elementDao().insert(answerElement)
        return questionId
    }

    private fun insertFillInBlankQuestion(paperId: Int, order: Int): Int {
        val question = Question(paperId, QuestionType.FILL_BLANK, order)
        val questionId = mDatabase.questionDao().insert(question).toInt()
        val body = Content(questionId, BODY_TYPE)
        val bodyId = mDatabase.contentDao().insert(body).toInt()
        val bodyElement = Element(Element.TEXT, DefaultData.FILL_IN_BODY_1, bodyId, 1)
        mDatabase.elementDao().insert(bodyElement)

        val bodyElement1 = Element(Element.PICTURE, DefaultData.FILL_IN_BODY_2, bodyId, 2)
        mDatabase.elementDao().insert(bodyElement1)

        val bodyElement2 = Element(Element.TEXT, DefaultData.FILL_IN_BODY_3, bodyId, 2)
        mDatabase.elementDao().insert(bodyElement2)

        val answer = Content(questionId, ANSWER_TYPE)
        val answerId = mDatabase.contentDao().insert(answer).toInt()
        val answerElement = Element(Element.TEXT, DefaultData.FILL_IN_ANSWER, answerId, 1)
        mDatabase.elementDao().insert(answerElement)
        return questionId
    }

    private fun insertTrueOrFalseQuestion(paperId: Int, order: Int): Int {
        val question = Question(paperId, QuestionType.TRUE_FALSE, order)
        val questionId = mDatabase.questionDao().insert(question).toInt()
        val body = Content(questionId, BODY_TYPE)
        val bodyId = mDatabase.contentDao().insert(body).toInt()
        val bodyElement = Element(Element.TEXT, DefaultData.TRUE_FALSE_BODY_1, bodyId, 1)
        mDatabase.elementDao().insert(bodyElement).toInt()
        val bodyElement1 = Element(Element.PICTURE, DefaultData.TRUE_FALSE_BODY_2, bodyId, 2)
        mDatabase.elementDao().insert(bodyElement1).toInt()
        val bodyElement2 = Element(Element.TEXT, DefaultData.TRUE_FALSE_BODY_3, bodyId, 2)
        mDatabase.elementDao().insert(bodyElement2).toInt()
        val answer = Content(questionId, ANSWER_TYPE)
        val answerId = mDatabase.contentDao().insert(answer).toInt()
        val answerElement = Element(Element.TEXT, DefaultData.TRUE_FALSE_ANSWER, answerId, 1)
        mDatabase.elementDao().insert(answerElement)
        return questionId
    }

    private fun insertSingleChoiceQuestion(paperId: Int, order: Int): Int {
        val question = Question(paperId, QuestionType.SINGLE_CHOICE, order)
        val questionId = mDatabase.questionDao().insert(question).toInt()
        val body = Content(questionId, BODY_TYPE)
        val bodyId = mDatabase.contentDao().insert(body).toInt()
        val bodyElement1 = Element(Element.TEXT, DefaultData.SINGLE_CHOICE_BODY_1, bodyId, 1)
        mDatabase.elementDao().insert(bodyElement1)
        val bodyElement2 = Element(Element.PICTURE, DefaultData.SINGLE_CHOICE_BODY_2, bodyId, 2)
        mDatabase.elementDao().insert(bodyElement2)
        val bodyElement3 = Element(Element.TEXT, DefaultData.SINGLE_CHOICE_BODY_3, bodyId, 3)
        mDatabase.elementDao().insert(bodyElement3)
        val bodyElement4 = Element(Element.TEXT, DefaultData.SINGLE_CHOICE_BODY_4, bodyId, 3)
        mDatabase.elementDao().insert(bodyElement4)

        val optionAId = mDatabase.contentDao().insert(Content(questionId, OPTION_TYPE)).toInt()
        mDatabase.elementDao().insert(Element(Element.TEXT, DefaultData.SINGLE_CHOICE_OPTION_1, optionAId, 1))

        val optionBId = mDatabase.contentDao().insert(Content(questionId, OPTION_TYPE)).toInt()
        mDatabase.elementDao().insert(Element(Element.TEXT, DefaultData.SINGLE_CHOICE_OPTION_2, optionBId, 1))

        val optionCId = mDatabase.contentDao().insert(Content(questionId, OPTION_TYPE)).toInt()
        mDatabase.elementDao().insert(Element(Element.TEXT, DefaultData.SINGLE_CHOICE_OPTION_3, optionCId, 1))
        mDatabase.elementDao().insert(Element(Element.PICTURE, DefaultData.SINGLE_CHOICE_OPTION_3_IMAGE, optionCId, 2))

        val optionDId = mDatabase.contentDao().insert(Content(questionId, OPTION_TYPE)).toInt()
        mDatabase.elementDao().insert(Element(Element.TEXT, DefaultData.SINGLE_CHOICE_OPTION_4, optionDId, 4))

        val answer = Content(questionId, ANSWER_TYPE)
        val answerId = mDatabase.contentDao().insert(answer).toInt()
        val answerElement = Element(Element.TEXT, DefaultData.SINGLE_CHOICE_ANSWER, answerId, 1)
        mDatabase.elementDao().insert(answerElement)
        return questionId
    }

    private fun insertMultiChoiceQuestion(paperId: Int, order: Int): Int {
        val question = Question(paperId, QuestionType.MULTIPLE_CHOICE, order)
        val questionId = mDatabase.questionDao().insert(question).toInt()
        val body = Content(questionId, BODY_TYPE)
        val bodyId = mDatabase.contentDao().insert(body).toInt()
        val bodyElement1 = Element(Element.TEXT, DefaultData.MULTI_CHOICE_BODY_1, bodyId, 1)
        mDatabase.elementDao().insert(bodyElement1)
        val bodyElement2 = Element(Element.PICTURE, DefaultData.MULTI_CHOICE_BODY_2, bodyId, 2)
        mDatabase.elementDao().insert(bodyElement2)
        val bodyElement3 = Element(Element.TEXT, DefaultData.MULTI_CHOICE_BODY_3, bodyId, 3)
        mDatabase.elementDao().insert(bodyElement3)
        val bodyElement4 = Element(Element.TEXT, DefaultData.MULTI_CHOICE_BODY_4, bodyId, 3)
        mDatabase.elementDao().insert(bodyElement4)

        val optionAId = mDatabase.contentDao().insert(Content(questionId, OPTION_TYPE)).toInt()
        mDatabase.elementDao().insert(Element(Element.TEXT, DefaultData.MULTI_CHOICE_OPTION_1, optionAId, 1))

        val optionBId = mDatabase.contentDao().insert(Content(questionId, OPTION_TYPE)).toInt()
        mDatabase.elementDao().insert(Element(Element.TEXT, DefaultData.MULTI_CHOICE_OPTION_2, optionBId, 1))

        val optionCId = mDatabase.contentDao().insert(Content(questionId, OPTION_TYPE)).toInt()
        mDatabase.elementDao().insert(Element(Element.TEXT, DefaultData.MULTI_CHOICE_OPTION_3, optionCId, 1))
        mDatabase.elementDao().insert(Element(Element.PICTURE, DefaultData.MULTI_CHOICE_OPTION_3_IMAGE, optionCId, 2))

        val optionDId = mDatabase.contentDao().insert(Content(questionId, OPTION_TYPE)).toInt()
        mDatabase.elementDao().insert(Element(Element.TEXT, DefaultData.MULTI_CHOICE_OPTION_4, optionDId, 4))

        val answer = Content(questionId, ANSWER_TYPE)
        val answerId = mDatabase.contentDao().insert(answer).toInt()
        val answerElement = Element(Element.TEXT, DefaultData.MULTI_CHOICE_ANSWER, answerId, 1)
        mDatabase.elementDao().insert(answerElement)
        return questionId
    }

    private fun insertEssayQuestion(paperId: Int, order: Int): Int {
        val question = Question(paperId, QuestionType.ESSAY_QUESTION, order)
        val questionId = mDatabase.questionDao().insert(question).toInt()
        val body = Content(questionId, BODY_TYPE)
        val bodyId = mDatabase.contentDao().insert(body).toInt()
        val bodyElement1 = Element(Element.TEXT, DefaultData.ESSAY_BODY_1, bodyId, 1)
        mDatabase.elementDao().insert(bodyElement1).toInt()
        val bodyElement2 = Element(Element.PICTURE, DefaultData.ESSAY_BODY_2, bodyId, 2)
        mDatabase.elementDao().insert(bodyElement2).toInt()
        val bodyElement3 = Element(Element.TEXT, DefaultData.ESSAY_BODY_3, bodyId, 2)
        mDatabase.elementDao().insert(bodyElement3).toInt()
        val answer = Content(questionId, ANSWER_TYPE)
        val answerId = mDatabase.contentDao().insert(answer).toInt()
        val answerElement = Element(Element.TEXT, DefaultData.ESSAY_ANSWER, answerId, 1)
        mDatabase.elementDao().insert(answerElement)
        return questionId
    }

    private fun insertRecycleBinFillInBlankQuestion(paperId: Int, order: Int): Int {
        val question = Question(paperId, QuestionType.FILL_BLANK, order)
        val questionId = mDatabase.questionDao().insert(question).toInt()
        val body = Content(questionId, BODY_TYPE)
        val bodyId = mDatabase.contentDao().insert(body).toInt()
        val bodyElement = Element(Element.TEXT, "废纸篓说明", bodyId, 1)
        mDatabase.elementDao().insert(bodyElement).toInt()
        val answer = Content(questionId, ANSWER_TYPE)
        val answerId = mDatabase.contentDao().insert(answer).toInt()
        val answerElement = Element(Element.TEXT, "“哪里有麻烦，哪里就有她”", answerId, 1)
        mDatabase.elementDao().insert(answerElement)
        return questionId
    }

}