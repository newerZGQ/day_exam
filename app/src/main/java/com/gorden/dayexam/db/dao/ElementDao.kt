package com.gorden.dayexam.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gorden.dayexam.db.entity.Book
import com.gorden.dayexam.db.entity.Course
import com.gorden.dayexam.db.entity.Paper
import com.gorden.dayexam.db.entity.question.Content
import com.gorden.dayexam.db.entity.question.Element
import com.gorden.dayexam.db.entity.question.Question

@Dao
interface ElementDao {
    @Insert
    fun insert(element: Element): Long

    @Insert
    fun insert(elements: List<Element>)

    @Query("SELECT * FROM element WHERE parentId = :parentId")
    fun getElementsEntity(parentId: Int): List<Element>

    @Delete
    fun delete(elements: List<Element>)

    @Query("DELETE FROM element WHERE parentId = :parentId")
    fun delete(parentId: Int)

    @Query("SELECT * FROM element WHERE parentId = :contentId")
    fun getByContentId(contentId: Int): LiveData<List<Element>>

    @Query("DELETE FROM element WHERE parentId = :contentId")
    fun deleteByContentId(contentId: Int)

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM element, content, question " +
            "WHERE element.parentId = content.contentId " +
            "AND content.questionId = question.id " +
            "AND question.id = :questionId")
    fun getByQuestionId(questionId: Int): LiveData<List<ElementWithContentAncestors>>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM element, content, question, d_context " +
            "WHERE element.elementType = 0 " +
            "AND element.content LIKE '%' || :key || '%' " +
            "AND element.parentId = content.contentId " +
            "AND content.questionId = question.id " +
            "AND question.paperId = d_context.curPaperId ")
    fun searchInPaper(key: String): List<ElementWithContentAncestors>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM element, content, question, paper, book, d_context " +
            "WHERE element.parentId = content.contentId " +
            "AND content.questionId = question.id " +
            "AND question.paperId = paper.id " +
            "AND paper.bookId = book.id " +
            "AND book.id = d_context.curBookId " +
            "AND element.elementType = 0 " +
            "AND element.content LIKE '%' || :key || '%' ")
    fun searchInBook(key: String): List<ElementWithContentAncestors>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM element, content, question, paper, book, course, d_context " +
            "WHERE element.parentId = content.contentId " +
            "AND content.questionId = question.id " +
            "AND question.paperId = paper.id " +
            "AND paper.bookId = book.id " +
            "AND book.courseId = course.id " +
            "AND course.id = d_context.curCourseId " +
            "AND element.elementType = 0 " +
            "AND element.content LIKE '%' || :key || '%' ")
    fun searchInCourse(key: String): List<ElementWithContentAncestors>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM element, content, question, paper, book, course, d_context " +
            "WHERE element.parentId = content.contentId " +
            "AND content.questionId = question.id " +
            "AND question.paperId = paper.id " +
            "AND paper.bookId = book.id " +
            "AND book.courseId = course.id " +
            "AND course.id = d_context.recycleBinId " +
            "AND element.elementType = 0 " +
            "AND element.content LIKE '%' || :key || '%' ")
    fun searchInRecycleBin(key: String): List<ElementWithContentAncestors>

    @Transaction
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM element, content, question, paper, book, course, d_context " +
            "WHERE element.parentId = content.contentId " +
            "AND content.questionId = question.id " +
            "AND question.paperId = paper.id " +
            "AND paper.bookId = book.id " +
            "AND book.courseId = course.id " +
            "AND element.elementType = 0 " +
            "AND element.content LIKE '%' || :key || '%' ")
    fun searchGlobal(key: String): List<ElementWithContentAncestors>
}

data class BookWithCourse(
    @Embedded val book: Book,
    @Relation(
        entity = Course::class,
        parentColumn = "courseId",
        entityColumn = "id"
    )
    val course: Course
)

data class PaperWithBookAndCourse(
    @Embedded val paper: Paper,
    @Relation(
        entity = Book::class,
        parentColumn = "bookId",
        entityColumn = "id"
    )
    val book: BookWithCourse
)

data class QuestionWithAncestors(
    @Embedded val question: Question,
    @Relation(
        entity = Paper::class,
        parentColumn = "paperId",
        entityColumn = "id"
    )
    val paper: PaperWithBookAndCourse
)

data class ContentWithAncestors(
    @Embedded val content: Content,
    @Relation(
        entity = Question::class,
        parentColumn = "questionId",
        entityColumn = "id"
    )
    val question: QuestionWithAncestors
)

data class ElementWithContentAncestors(
    @Embedded val element: Element,
    @Relation(
        entity = Content::class,
        parentColumn = "parentId",
        entityColumn = "contentId"
    )
    val content: ContentWithAncestors
)