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
interface CourseDao {

    @Query("SELECT * FROM course ORDER BY position ASC")
    fun getAllCourse(): LiveData<List<Course>>

    @Query("SELECT * FROM course ORDER BY position ASC")
    fun getAllCourseWithChildren(): List<CourseWithChildren>

    @Query("SELECT * FROM course")
    fun getAllCourseEntity(): List<Course>

    @Insert
    fun insert(course: Course): Long

    @Insert
    fun insert(list: List<Course>)

    @Update
    fun update(course: Course)

    @Delete
    fun delete(course: Course)

    @Query("SELECT * FROM course WHERE id = :id ORDER BY position DESC")
    fun getCourse(id: Int): LiveData<Course>

    @Query("SELECT * FROM course WHERE id = :id")
    fun getCourseEntity(id: Int): Course?

    @Query("SELECT position FROM course WHERE isRecycleBin = 0 ORDER BY position DESC LIMIT 1")
    fun getMaxOrder(): Int
}

data class CourseWithChildren(
    @Embedded val course: Course,
    @Relation(
        entity = Book::class,
        parentColumn = "id",
        entityColumn = "courseId"
    )
    val books: List<BookWithChildren>

)

data class BookWithChildren(
    @Embedded val book: Book,
    @Relation(
        entity = Paper::class,
        parentColumn = "id",
        entityColumn = "bookId"
    )
    val papers: List<PaperWithChildren>
)

data class PaperWithChildren(
    @Embedded val paper: Paper,
    @Relation(
        entity = Question::class,
        parentColumn = "id",
        entityColumn = "paperId"
    )
    val questions: List<QuestionWithContent>
)

data class QuestionWithChildren(
    @Embedded val question: Question,
    @Relation(
        entity = Content::class,
        parentColumn = "id",
        entityColumn = "questionId"
    )
    val contents: List<ContentWithChildren>
)

data class ContentWithChildren(
    @Embedded val content: Content,
    @Relation(
        entity = Element::class,
        parentColumn = "contentId",
        entityColumn = "parentId"
    )
    val elements: List<Element>
)