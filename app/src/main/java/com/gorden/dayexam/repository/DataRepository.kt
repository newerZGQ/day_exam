package com.gorden.dayexam.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.sqlite.db.SimpleSQLiteQuery
import com.gorden.dayexam.db.AppDatabase
import com.gorden.dayexam.db.DefaultDataGenerator
import com.gorden.dayexam.db.converter.DateConverter
import com.gorden.dayexam.db.dao.CourseWithChildren
import com.gorden.dayexam.db.dao.ElementWithContentAncestors
import com.gorden.dayexam.db.dao.QuestionWithContent
import com.gorden.dayexam.db.entity.*
import com.gorden.dayexam.db.entity.Paper
import com.gorden.dayexam.db.entity.question.*
import com.gorden.dayexam.db.entity.question.Content.Companion.ANSWER_TYPE
import com.gorden.dayexam.db.entity.question.Content.Companion.BODY_TYPE
import com.gorden.dayexam.db.entity.question.Content.Companion.OPTION_TYPE
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.parser.model.PQuestion
import com.gorden.dayexam.repository.RepositoryConstants.Companion.DefaultBookName
import com.gorden.dayexam.repository.RepositoryConstants.Companion.DefaultPaperName
import com.gorden.dayexam.repository.model.*
import com.gorden.dayexam.ui.book.BookDetail
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

    /**
     * Course 相关
     */
    fun insertCourse(title: String, desc: String) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val courseMaxOrder = mDatabase.courseDao().getMaxOrder()
                val course = Course(title, desc, courseMaxOrder + 1)
                val id = mDatabase.courseDao().insert(course)
                val bookId = mDatabase.bookDao().insert(Book(DefaultBookName, "", id.toInt(), 1))
                val courseStatus = StudyStatus(CourseStatus, id.toInt(), bookId.toInt())
                mDatabase.studyStatusDao().insert(courseStatus)
                val paper = Paper(DefaultPaperName, "", bookId.toInt(), 1)
                val paperId = mDatabase.paperDao().insert(paper)
                val bookStatus = StudyStatus(BookStatus, bookId.toInt(), paperId.toInt())
                mDatabase.studyStatusDao().insert(bookStatus)
                val paperStatus = StudyStatus(PaperStatus, paperId.toInt(), 0)
                mDatabase.studyStatusDao().insert(paperStatus)
            }
        }
    }

    fun updateCourse(id: Int, title: String, desc: String) {
        AppExecutors.diskIO().execute {
            val course = mDatabase.courseDao().getCourseEntity(id)
            course?.let {
                it.title = title
                it.description = desc
                it.editTime = Date()
                mDatabase.courseDao().update(it)
            }
        }
    }

    fun deleteCourse(id: Int) {
        AppExecutors.diskIO().execute {
            mDatabase.courseDao().getCourseEntity(id)?.let { course ->
                val books = mDatabase.bookDao().getBookEntitiesByCourseId(course.id)
                deleteBooks(books)
                mDatabase.courseDao().delete(course)
                mDatabase.studyStatusDao().delete(CourseStatus, course.id)
            }
        }
    }

    fun getAllCourseExcludeRecycleBin(courses: MutableLiveData<List<Course>>) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val courseEntities = mDatabase.courseDao().getAllCourseEntity()
                val recycleBinId = mDatabase.dContextDao().getDContextEntity().recycleBinId
                val result = courseEntities.filter {
                    it.id != recycleBinId
                }
                AppExecutors.mainThread().execute {
                    courses.value = result
                }
            }
        }
    }

    fun getAllCourse():LiveData<List<Course>> {
        return mDatabase.courseDao().getAllCourse()
    }

    fun currentCourse(): LiveData<Course> {
        val dContext = mDatabase.dContextDao().getDContext()
        return Transformations.switchMap(dContext) {
            dContext.value?.let {
                mDatabase.courseDao().getCourse(it.curCourseId)
            }
        }
    }

    private fun deletePapers(papers: List<Paper>) {
        val dContext = mDatabase.dContextDao().getDContextEntity()
        val recycleMaxPaperPosition = mDatabase.paperDao().getMaxPosition(dContext.recycleBookId)
        papers.forEachIndexed { index, paper ->
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

    private fun deleteBooks(books: List<Book>) {
        books.forEach { book ->
            val papers = mDatabase.paperDao().getEntityByBookIdOrderByPosition(book.id)
            deletePapers(papers)
            mDatabase.bookDao().delete(book)
            mDatabase.studyStatusDao().delete(BookStatus, book.id)
        }
    }

    fun getAllCourseWithChildren(): List<CourseWithChildren> {
        return mDatabase.courseDao().getAllCourseWithChildren()
    }

    /**
     * 将书籍的状态更新为当前试卷为bookId, 当前书籍为courseId
     * courseId: 要变更的试卷组
     * bookId;
     */
    fun updateCourseStatus(courseId: Int, bookId: Int, paperId: Int) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val courseStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(
                    CourseStatus, courseId)
                courseStatus.curChild = bookId
                mDatabase.studyStatusDao().update(courseStatus)
                val bookStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(
                    BookStatus, bookId)
                bookStatus.curChild = paperId
                mDatabase.studyStatusDao().update(bookStatus)
                val paperStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(
                    PaperStatus, paperId
                )
                val dContext = mDatabase.dContextDao().getDContextEntity()
                dContext.curBookId = bookId
                dContext.curPaperId = paperId
                dContext.curQuestionId = paperStatus.curChild
                mDatabase.dContextDao().update(dContext)
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
     * book相关
     */

    fun insertBook(title: String, courseId: Int) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val maxBookOrder = mDatabase.bookDao().getMaxOrder(courseId)
                val bookId = mDatabase.bookDao().insert(Book(title, "", courseId, maxBookOrder + 1)).toInt()
                val paper = Paper(DefaultPaperName, "", bookId, 1)
                val paperId = mDatabase.paperDao().insert(paper).toInt()
                val bookStatus = StudyStatus(BookStatus, bookId, paperId)
                mDatabase.studyStatusDao().insert(bookStatus)
                val paperStatus = StudyStatus(PaperStatus, paperId, 0)
                mDatabase.studyStatusDao().insert(paperStatus)
            }
        }
    }

    fun deleteBook(book: Book) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val dContext = mDatabase.dContextDao().getDContextEntity()
                val courseStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(
                    CourseStatus, book.courseId)
                if (courseStatus.curChild != book.id) {
                    innerDeleteBook(book)
                    return@runInTransaction
                } else {
                    val books = mDatabase.bookDao().getBookEntitiesByCourseId(book.courseId)
                    if (books.size == 1 ) {
                        courseStatus.curChild = 0
                        mDatabase.studyStatusDao().update(courseStatus)

                        if (dContext.curBookId == book.id) {
                            dContext.curBookId = 0
                            mDatabase.dContextDao().update(dContext)
                        }

                        innerDeleteBook(book)
                    } else if (books.size > 1) {
                        val newCurBook = books.filter {
                            it.id != book.id
                        }[0]

                        courseStatus.curChild = newCurBook.id
                        mDatabase.studyStatusDao().update(courseStatus)

                        if (dContext.curBookId == book.id) {
                            dContext.curBookId = newCurBook.id
                            mDatabase.dContextDao().update(dContext)
                        }

                        innerDeleteBook(book)
                    }
                }
            }
        }
    }

    private fun innerDeleteBook(book: Book) {
        deletePapers(mDatabase.paperDao().getEntityByBookIdOrderByPosition(book.id))
        mDatabase.bookDao().delete(book)
    }

    fun updateBooks(books: List<Book>) {
        AppExecutors.diskIO().execute {
            mDatabase.bookDao().update(books)
        }
    }

    fun updateBook(book: Book) {
        AppExecutors.diskIO().execute {
            mDatabase.bookDao().update(book)
        }
    }

    // 获取当前需要显示的所有书籍（可能是废纸篓里的），并包含详细的试卷以及每个试卷当前试题的信息
    fun currentBookDetail(bookDetail: MutableLiveData<BookDetail>) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val curBookDetail = innerGetBookDetail()
                AppExecutors.mainThread().execute {
                    bookDetail.value = curBookDetail
                }
            }
        }
    }

    // 获取当前课程的所有书籍，并包含详细的试卷以及每个试卷当前试题的信息
    private fun innerGetBookDetail(): BookDetail {
        val dContext = mDatabase.dContextDao().getDContextEntity()
        val course = mDatabase.courseDao().getCourseEntity(dContext.curCourseId)
            ?: return BookDetail(0, "", 0, 0, false, listOf())
        val isRecycleBin = course.id == dContext.recycleBinId
        val curBookId = mDatabase.bookDao().getEntity(dContext.curBookId)?.id?: 0
        val curPaperId = mDatabase.paperDao().getEntityById(dContext.curPaperId)?.id?: 0

        var books = mDatabase.bookDao().getBookEntitiesByCourseId(course.id)
        val bookWithPapers = mutableListOf<BookWithPaper>()
        books.forEach { book ->
            val papers = if (isRecycleBin){
                mDatabase.paperDao().getEntityByBookIdOrderByEditTime(book.id)
            } else {
                mDatabase.paperDao().getEntityByBookIdOrderByPosition(book.id)
            }
            val paperWithQuestions = mutableListOf<PaperWithQuestion>()
            papers.forEach { paper ->
                val paperStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(PaperStatus, paper.id)
                val question = mDatabase.questionDao().getEntityWithContentById(paperStatus.curChild)
                val questionCount = mDatabase.questionDao().getCountWithPaperId(paper.id)
                val questionWithElement = toQuestionElement(question)
                val studyInfo = getStudyRecordInfo(paper.id)
                paperWithQuestions.add(PaperWithQuestion(paper, paperStatus.curChild,
                    questionCount.toInt(), studyInfo, questionWithElement))
            }
            val bookWithPaper = BookWithPaper(book, paperWithQuestions)
            bookWithPapers.add(bookWithPaper)
        }
        return BookDetail(
            course.id,
            course.title,
            curBookId,
            curPaperId,
            isRecycleBin,
            bookWithPapers)
    }

    fun getAllBooks(courseId: Int): LiveData<List<Book>> {
        return mDatabase.bookDao().getBookByCourseId(courseId)
    }

    /**
     * paper相关
     */
    fun insertPaper(title: String, desc: String, bookId: Int) {
        AppExecutors.diskIO().execute {
            val maxOrder = mDatabase.paperDao().getMaxPosition(bookId)
            val paper = Paper(title, desc, bookId, maxOrder + 1)
            val paperId = mDatabase.paperDao().insert(paper).toInt()
            val paperStatus = StudyStatus(PaperStatus, paperId, 0)
            mDatabase.studyStatusDao().insert(paperStatus)
        }
    }

    fun currentPaper(): LiveData<Paper> {
        val dContext = mDatabase.dContextDao().getDContext()
        return Transformations.switchMap(dContext) {
            dContext.value?.let {
                mDatabase.paperDao().getById(it.curPaperId)
            }
        }
    }

    fun updatePapers(papers: List<Paper>) {
        AppExecutors.diskIO().execute {
            mDatabase.paperDao().update(papers)
        }
    }

    fun updatePaper(paper: Paper) {
        AppExecutors.diskIO().execute {
            mDatabase.paperDao().update(paper)
        }
    }

    fun deletePaper(paper: Paper, clearUp: Boolean) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val dContext = mDatabase.dContextDao().getDContextEntity()
                val recycleBookId = dContext.recycleBookId
                val bookStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(BookStatus, paper.bookId)
                if (bookStatus.curChild == paper.id) {
                    val papers = mDatabase.paperDao().getEntityByBookIdOrderByPosition(paper.bookId)
                    if (papers.size == 1 ) {
                        bookStatus.curChild = 0
                        mDatabase.studyStatusDao().update(bookStatus)

                        if (dContext.curPaperId == paper.id) {
                            dContext.curPaperId = 0
                            dContext.curQuestionId = 0
                            mDatabase.dContextDao().update(dContext)
                        }
                    } else if (papers.size > 1) {
                        val newCurPaper = papers.filter {
                            it.id != paper.id
                        }[0]

                        bookStatus.curChild = newCurPaper.id
                        mDatabase.studyStatusDao().update(bookStatus)

                        if (dContext.curPaperId == paper.id) {
                            dContext.curPaperId = newCurPaper.id
                            mDatabase.dContextDao().update(dContext)
                        }
                    }
                }
                if (clearUp) {
                    clearUpPaper(paper)
                } else {
                    paper.bookId = recycleBookId
                    paper.title = "从" + paper.title + "删除"
                    paper.editTime = Date()
                    mDatabase.paperDao().update(paper)
                }
            }
        }
    }

    private fun clearUpPaper(paper: Paper) {
        val questions = mDatabase.questionDao().getEntityByPaperId(paper.id)
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
        mDatabase.studyStatusDao().delete(PaperStatus, paper.id)
        mDatabase.paperDao().delete(paper)
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

    fun isInRecycleBin(paper: Paper, callback: IsInRecycleBinCallback) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val dContext = mDatabase.dContextDao().getDContextEntity()
                val isInRecycleBin = paper.bookId == dContext.recycleBookId
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
                paper.title, paper.id, dContext.curQuestionId, questionWithElements as List<QuestionWithElement>
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
                paper.title, paper.id, paperStatus.curChild, questionWithElements as List<QuestionWithElement>
            )
            AppExecutors.mainThread().execute {
                questionDetail.value = questionDetailEntity
            }
        }
    }

    private fun toQuestionElement(question: QuestionWithContent): QuestionWithElement? {
        if (question == null) {
            return null
        }
        val realQuestion = question.question
        var bodyWithElement = BodyWithElement(realQuestion.id, listOf())
        var optionWithElement: MutableList<OptionItemWithElement> = mutableListOf()
        var answerWithElement = AnswerWithElement(realQuestion.id, listOf())

        question.contents.forEach { content ->
            content.elements = content.elements.sortedBy { it.position }
            if (content.content.contentType == BODY_TYPE) {
                bodyWithElement = BodyWithElement(content.content.contentId, content.elements)
            } else if (content.content.contentType == OPTION_TYPE) {
                optionWithElement.add(OptionItemWithElement(content.content.contentId, content.elements))
            } else if (content.content.contentType == ANSWER_TYPE) {
                answerWithElement = AnswerWithElement(content.content.contentId, content.elements)
            }
        }
        return QuestionWithElement(question.question.id, question.question.questionType, bodyWithElement, optionWithElement, answerWithElement)
    }

    fun deleteCurrentQuestion(callback: DeleteQuestionCallback) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                val dContext = mDatabase.dContextDao().getDContextEntity()
                val question = mDatabase.questionDao().getEntityById(dContext.curQuestionId)
                if (question == null) {
                    AppExecutors.mainThread().execute {
                        callback.onFinished(false, "当前没有试题")
                    }
                    return@runInTransaction
                }
                val paperId = question.paperId
                val position = question.questionPosition
                val paperStatus = mDatabase.studyStatusDao().queryEntityByTypeAndContentId(
                    PaperStatus, question.paperId)
                if (question.id == dContext.curQuestionId) {
                    var nextStudyQuestion = mDatabase.questionDao().getPrePositionQuestion(paperId, position)
                    if (nextStudyQuestion == null) {
                        nextStudyQuestion = mDatabase.questionDao().getNextPositionQuestion(paperId, position)
                    }
                    if (nextStudyQuestion == null) {
                        dContext.curQuestionId = 0
                        mDatabase.dContextDao().update(dContext)
                        paperStatus.curChild = 0
                        mDatabase.studyStatusDao().update(paperStatus)
                    } else {
                        dContext.curQuestionId = nextStudyQuestion.id
                        mDatabase.dContextDao().update(dContext)
                        paperStatus.curChild = nextStudyQuestion.id
                        mDatabase.studyStatusDao().update(paperStatus)
                    }
                }
                val paper = mDatabase.paperDao().getEntityById(question.paperId)
                val maxPosition = mDatabase.paperDao().getMaxPosition(dContext.recycleBookId)
                val recyclePaper = Paper("从" + paper?.title + "删除", "", dContext.recycleBookId, maxPosition + 1)
                val recyclePaperId = mDatabase.paperDao().insert(recyclePaper).toInt()
                question.editTime = Date()
                question.paperId = recyclePaperId
                mDatabase.questionDao().update(question)
                val recyclePaperStatus = StudyStatus(PaperStatus, recyclePaperId, question.id)
                mDatabase.studyStatusDao().insert(recyclePaperStatus)
                AppExecutors.mainThread().execute {
                    callback.onFinished(true, "已放入废纸篓")
                }
            }
        }
    }

    fun getPapersByBookId(bookId: Int): LiveData<List<Paper>> {
        return mDatabase.paperDao().getByBookId(bookId)
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
     * element相关
     */

    fun updateElementsByContentId(contentId: Int, elements: List<Element>) {
        AppExecutors.diskIO().execute {
            mDatabase.runInTransaction {
                mDatabase.elementDao().deleteByContentId(contentId)
                mDatabase.elementDao().insert(elements)
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

