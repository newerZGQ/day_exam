package com.gorden.dayexam.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.sqlite.db.SimpleSQLiteQuery
import com.gorden.dayexam.db.AppDatabase
import com.gorden.dayexam.db.DefaultDataGenerator
import com.gorden.dayexam.db.converter.DateConverter
import com.gorden.dayexam.db.dao.ElementWithContentAncestors
import com.gorden.dayexam.db.dao.QuestionWithContent
import com.gorden.dayexam.db.entity.*
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.db.entity.question.*
import com.gorden.dayexam.repository.model.question.Content.Companion.ANSWER_TYPE
import com.gorden.dayexam.repository.model.question.Content.Companion.BODY_TYPE
import com.gorden.dayexam.repository.model.question.Content.Companion.OPTION_TYPE
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.parser.model.PQuestion
import com.gorden.dayexam.repository.model.*
import com.gorden.dayexam.repository.model.question.Content
import com.gorden.dayexam.repository.model.question.Question
import java.util.*

object DataRepository {

    private lateinit var mDatabase: AppDatabase

    fun init(mDatabase: AppDatabase) {
        this.mDatabase = mDatabase
        DefaultDataGenerator.init(mDatabase)
    }

    /**
     * Dao层状态相关
     */
    fun isDatabaseCreated(): LiveData<Boolean> {
        return this.mDatabase.isDatabaseCreated
    }

    fun isDatabaseOpened(): LiveData<Boolean> {
        return this.mDatabase.isDatabaseOpened
    }

    /**
     * DContext 相关
     */
    fun updateDContextCourseId(courseId: Int) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val dContext = mDatabase.dContextDao().getDContextEntity()
                val courseStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(
                    CourseStatus, courseId)
                val bookStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(
                    BookStatus, courseStatus.curChild)
                dContext.curCourseId = courseId
                if (bookStatus == null) {
                    dContext.curBookId = 0
                    dContext.curPaperId = 0
                    dContext.curQuestionId = 0
                    mDatabase.dContextDao().update(dContext)
                    return@runInTransaction
                }
                val paperStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(
                    PaperStatus, bookStatus.curChild)
                dContext.curBookId = bookStatus.contentId
                if (paperStatus != null) {
                    dContext.curPaperId = paperStatus.contentId
                    dContext.curQuestionId = paperStatus.curChild
                } else {
                    dContext.curPaperId = 0
                    dContext.curQuestionId = 0
                }
                mDatabase.dContextDao().update(dContext)
            }
        }
    }

    fun updateDContext(courseId: Int, bookId: Int, paperId: Int, questionId: Int) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val dContext = mDatabase.dContextDao().getDContextEntity()
                dContext.curCourseId = courseId
                dContext.curBookId = bookId
                dContext.curPaperId = paperId
                dContext.curQuestionId = questionId
                mDatabase.dContextDao().update(dContext)
            }
        }
    }

    fun getDContext(): LiveData<DContext> {
        return mDatabase.dContextDao().getDContext()
    }

    fun increaseContentVersion() {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val dContext = mDatabase.dContextDao().getDContextEntity()
                dContext.version = dContext.version + 1
                mDatabase.dContextDao().update(dContext)
            }
        }
    }

    fun checkPoint() {
        mDatabase.dContextDao().checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
    }


    private fun deletePapers(paperInfos: List<PaperInfo>) {
        val dContext = mDatabase.dContextDao().getDContextEntity()
        val recycleMaxPaperPosition = mDatabase.paperDao().getMaxPosition(dContext.recycleBookId)
        paperInfos.forEachIndexed { index, paper ->
            val questionCount = mDatabase.questionDao().getCountWithPaperId(paper.id)
            if (questionCount > 0) {
                paper.bookId = dContext.recycleBookId
                val oldTitle = paper.title
                paper.title = "从" + oldTitle + "删除"
                paper.position = recycleMaxPaperPosition + index + 1
                mDatabase.paperDao().update(paper)
            } else {
                mDatabase.paperDao().delete(paper)
                mDatabase.studyStatusDao().delete(PaperStatus, paper.id)
            }
        }
    }

    /**
     * study status相关
     */

    fun getStudyStatus(type: Int, contentId: Int): LiveData<StudyStatus> {
        return mDatabase.studyStatusDao().queryByTypeAndContentId(type, contentId)
    }

    /**
     * paper相关
     */
    fun insertPaper(title: String, desc: String, bookId: Int) {
        AppExecutors.diskIO().execute {
            val maxOrder = mDatabase.paperDao().getMaxPosition(bookId)
            val paperInfo = PaperInfo(title, desc, bookId, maxOrder + 1)
            val paperId = mDatabase.paperDao().insert(paperInfo).toInt()
            val paperStatus = StudyStatus(PaperStatus, paperId, 0)
            mDatabase.studyStatusDao().insert(paperStatus)
        }
    }

    fun currentPaper(): LiveData<PaperInfo> {
        val dContext = mDatabase.dContextDao().getDContext()
        return Transformations.switchMap(dContext) {
            dContext.value?.let {
                mDatabase.paperDao().getById(it.curPaperId)
            }
        }
    }

    fun updatePapers(paperInfos: List<PaperInfo>) {
        AppExecutors.diskIO().execute {
            mDatabase.paperDao().update(paperInfos)
        }
    }

    fun updatePaper(paperInfo: PaperInfo) {
        AppExecutors.diskIO().execute {
            mDatabase.paperDao().update(paperInfo)
        }
    }

    fun deletePaper(paperInfo: PaperInfo, clearUp: Boolean) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val dContext = mDatabase.dContextDao().getDContextEntity()
                val recycleBookId = dContext.recycleBookId
                val bookStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(BookStatus, paperInfo.bookId)
                if (bookStatus.curChild == paperInfo.id) {
                    val papers = mDatabase.paperDao().getEntityByBookIdOrderByPosition(paperInfo.bookId)
                    if (papers.size == 1 ) {
                        bookStatus.curChild = 0
                        mDatabase.studyStatusDao().update(bookStatus)

                        if (dContext.curPaperId == paperInfo.id) {
                            dContext.curPaperId = 0
                            dContext.curQuestionId = 0
                            mDatabase.dContextDao().update(dContext)
                        }
                    } else if (papers.size > 1) {
                        val newCurPaper = papers.filter {
                            it.id != paperInfo.id
                        }[0]

                        bookStatus.curChild = newCurPaper.id
                        mDatabase.studyStatusDao().update(bookStatus)

                        if (dContext.curPaperId == paperInfo.id) {
                            dContext.curPaperId = newCurPaper.id
                            mDatabase.dContextDao().update(dContext)
                        }
                    }
                }
                if (clearUp) {
                    clearUpPaper(paperInfo)
                } else {
                    paperInfo.bookId = recycleBookId
                    paperInfo.title = "从" + paperInfo.title + "删除"
                    paperInfo.editTime = Date()
                    mDatabase.paperDao().update(paperInfo)
                }
            }
        }
    }

    private fun clearUpPaper(paperInfo: PaperInfo) {
        val questions = mDatabase.questionDao().getEntityByPaperId(paperInfo.id)
        questions.forEach { question ->
            // 删除body
            val body = mDatabase.contentDao().getBodyEntity(question.id)
            mDatabase.contentDao().delete(body)
            mDatabase.elementDao().delete(body.contentId)
            // 删除option
            mDatabase.contentDao().getOptionEntity(question.id).forEach { option ->
                mDatabase.elementDao().delete(option.contentId)
                mDatabase.contentDao().delete(option)
            }
            // 删除answer
            val answer = mDatabase.contentDao().getAnswerEntity(question.id)
            mDatabase.contentDao().delete(answer)
            mDatabase.elementDao().delete(answer.contentId)
        }
        mDatabase.questionDao().delete(questions)
        mDatabase.studyStatusDao().delete(PaperStatus, paperInfo.id)
        mDatabase.paperDao().delete(paperInfo)
    }

    // 将paperFrom的所有试题转移至paperTo, 然后删除paperFrom
    fun movePaper(paperFrom: Int, paperTo: Int) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val questions = mDatabase.questionDao().getEntityByPaperId(paperFrom)
                questions.forEach {
                    it.paperId = paperTo
                }
                mDatabase.questionDao().update(questions)
                mDatabase.paperDao().delete(paperFrom)
            }
        }
    }

    fun isInRecycleBin(paperInfo: PaperInfo, callback: IsInRecycleBinCallback) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val dContext = mDatabase.dContextDao().getDContextEntity()
                val isInRecycleBin = paperInfo.bookId == dContext.recycleBookId
                AppExecutors.mainThread().execute {
                    callback.onFinished(isInRecycleBin)
                }
            }
        }
    }

    /**
     * question相关
     */

    // 更新试卷状态以及dontext的当前question
     fun updatePaperStatus(paperId: Int, questionId: Int) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val dContext = mDatabase.dContextDao().getDContextEntity()
                dContext.curQuestionId = questionId
                mDatabase.dContextDao().update(dContext)
                val paperStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(PaperStatus, paperId)
                paperStatus.curChild = questionId
                mDatabase.studyStatusDao().update(paperStatus)
            }
        }
    }

    // 将解析器的结果存储到数据库
    fun insertPQuestion(questions: List<PQuestion>, paperId: Int) {
        AppExecutors.diskIO().execute {
            val maxOrder = mDatabase.questionDao().getMaxPosition(paperId)
            questions.forEachIndexed { index, pQuestion ->
                val question = Question(paperId, pQuestion.type, maxOrder + index + 1)
                val questionId = mDatabase.questionDao().insert(question).toInt()
                val bodyId = mDatabase.contentDao().insert(Content(questionId, BODY_TYPE)).toInt()
                pQuestion.body.elements.forEach {
                    it.parentId = bodyId
                    mDatabase.elementDao().insert(it)
                }
                val answerId = mDatabase.contentDao().insert(Content(questionId, ANSWER_TYPE)).toInt()
                pQuestion.answer.elements.forEach {
                    it.parentId = answerId
                    mDatabase.elementDao().insert(it)
                }
                pQuestion.options.forEach { pOptionItem ->
                    val optionItemId = mDatabase.contentDao().insert(Content(questionId, OPTION_TYPE)).toInt()
                    pOptionItem.elements.forEach {
                        it.parentId = optionItemId
                        mDatabase.elementDao().insert(it)
                    }
                }
            }
        }
    }

    // 当前正在学习的所有试题，包含试卷信息
    fun currentQuestionDetail(questionDetail: MutableLiveData<QuestionDetail>) {
        AppExecutors.diskIO().execute {
            val dContext = mDatabase.dContextDao().getDContextEntity()
            val course = mDatabase.courseDao().getCourseEntity(dContext.curCourseId)
            val book = mDatabase.bookDao().getEntity(dContext.curBookId)
            val paper = mDatabase.paperDao().getEntityById(dContext.curPaperId)
            if (course == null || book == null || paper == null) {
                val questionDetailEntity = QuestionDetail("", "", 0,
                    "", 0, 0, listOf())
                AppExecutors.mainThread().execute {
                    questionDetail.value = questionDetailEntity
                }
                return@execute
            }
            val questions = mDatabase.questionDao().getEntityWithContentByPaperId(paper.id)
            val questionWithElements = questions.map {
                toQuestionElement(it)
            }
            val questionDetailEntity = QuestionDetail(course.title, book.title, book.id,
                paper.title, paper.id, dContext.curQuestionId, questionWithElements as List<QuestionDetail>
            )
            AppExecutors.mainThread().execute {
                questionDetail.value = questionDetailEntity
            }

        }
    }

    // 获取指定paper的所有试题，包含试卷信息
    fun simpleQuestionListWithDetail(paperId: Int, questionDetail: MutableLiveData<SimpleQuestionListWithDetail>) {
        AppExecutors.diskIO().execute {
            val paper = mDatabase.paperDao().getEntityById(paperId)
            if (paper == null) {
                val questionDetailEntity = SimpleQuestionListWithDetail("", 0, 0, listOf())
                AppExecutors.mainThread().execute {
                    questionDetail.value = questionDetailEntity
                }
                return@execute
            }

            val questions = mDatabase.questionDao().getEntityWithContentByPaperId(paper.id)
            val questionWithElements = questions.map {
                toQuestionElement(it)
            }

            val paperStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(PaperStatus, paper.id)
            val questionDetailEntity = SimpleQuestionListWithDetail(
                paper.title, paper.id, paperStatus.curChild, questionWithElements as List<QuestionDetail>
            )
            AppExecutors.mainThread().execute {
                questionDetail.value = questionDetailEntity
            }
        }
    }

    private fun toQuestionElement(question: QuestionWithContent): QuestionDetail? {
        if (question == null) {
            return null
        }
        val realQuestion = question.question
        var questionBody = QuestionBody(realQuestion.id, listOf())
        var optionWithElement: MutableList<OptionItems> = mutableListOf()
        var answerWithElement = AnswerWithElement(realQuestion.id, listOf())

        question.contents.forEach { content ->
            content.elements = content.elements.sortedBy { it.position }
            if (content.content.contentType == BODY_TYPE) {
                questionBody = QuestionBody(content.content.contentId, content.elements)
            } else if (content.content.contentType == OPTION_TYPE) {
                optionWithElement.add(OptionItems(content.content.contentId, content.elements))
            } else if (content.content.contentType == ANSWER_TYPE) {
                answerWithElement = AnswerWithElement(content.content.contentId, content.elements)
            }
        }
        return QuestionDetail(question.question.id, question.question.questionType, questionBody, optionWithElement, answerWithElement)
    }

    fun getPapersByBookId(bookId: Int): LiveData<List<PaperInfo>> {
        return mDatabase.paperDao().getByBookId(bookId)
    }

    fun getAllPapers(): LiveData<List<PaperInfo>> {
        return mDatabase.paperDao().getAllPapers()
    }

    /**
     * StudyRecord相关
     */
    fun insertStudyRecord(studyRecord: StudyRecord) {
        AppExecutors.diskIO().execute {
            mDatabase.studyRecordDao().insert(studyRecord)
        }
    }

    // 当前试卷的当前试题的最后学习时间作为试卷最后学习时间
    private fun getStudyRecordInfo(paperId: Int): PaperStudyInfo {
        val studyCount = mDatabase.studyRecordDao().getPaperStudyCount(paperId).toInt()
        val paperStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(PaperStatus, paperId)
        val studyRecord = mDatabase.studyRecordDao().getLast(paperStatus.curChild)
        val lastDate = studyRecord?.createTime
        return PaperStudyInfo(studyCount, lastDate)
    }

    fun todayStudyCount(): LiveData<Long> {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        val todayZero = DateConverter().toTimestamp(today.time)
        return mDatabase.studyRecordDao().getStudyCountAfter(todayZero)
    }

    /**
     * Config相关
     */
    fun getConfig(): LiveData<Config> {
        return mDatabase.configDao().get()
    }

    fun updateRememberMode(opened: Boolean) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val config = mDatabase.configDao().getEntity()
                config.rememberMode = opened
                mDatabase.configDao().update(config)
            }
        }
    }

    fun updateFocusMode(opened: Boolean) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val config = mDatabase.configDao().getEntity()
                config.focusMode = opened
                mDatabase.configDao().update(config)
            }
        }
    }

    fun updateOnlyFavoriteMode(opened: Boolean) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val config = mDatabase.configDao().getEntity()
                config.onlyFavorite = opened
                mDatabase.configDao().update(config)
            }
        }
    }

    fun updateSortAccuracyMode(opened: Boolean) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val config = mDatabase.configDao().getEntity()
                config.sortByAccuracy = opened
                mDatabase.configDao().update(config)
            }
        }
    }

    /**
     * 搜索search相关
     */
    fun searchByScopeAndKey(scope: Int, key: String, liveSearchItems: MutableLiveData<List<SearchItem>>) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                var elements = listOf<ElementWithContentAncestors>()
                when (scope) {
                    SearchScope.SEARCH_IN_PAPER -> {
                        elements = mDatabase.elementDao().searchInPaper(key)
                    }
                    SearchScope.SEARCH_IN_BOOK -> {
                        elements = mDatabase.elementDao().searchInBook(key)
                    }
                    SearchScope.SEARCH_IN_COURSE -> {
                        elements = mDatabase.elementDao().searchInCourse(key)
                    }
                    SearchScope.SEARCH_RECYCLE_BIN -> {
                        elements = mDatabase.elementDao().searchInRecycleBin(key)
                    }
                    SearchScope.SEARCH_GLOBAL -> {
                        elements = mDatabase.elementDao().searchGlobal(key)
                    }
                }
                val searchItems = elements.map {
                    val question = it.content.question
                    val paper = question.paper
                    val book = paper.book
                    val course = book.course
                    SearchItem(course.id,
                        course.title,
                        book.book.id,
                        book.book.title,
                        paper.paper.id,
                        paper.paper.title,
                        question.question.id,
                        question.question.questionType,
                        it.content.content.contentType,
                        it.element.content
                    )
                }
                AppExecutors.mainThread().execute {
                    liveSearchItems.value = searchItems
                }
            }
        }

    }

}

