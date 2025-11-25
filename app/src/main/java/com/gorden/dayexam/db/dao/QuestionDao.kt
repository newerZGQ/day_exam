package com.gorden.dayexam.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gorden.dayexam.db.entity.question.Content
import com.gorden.dayexam.db.entity.question.Element
import com.gorden.dayexam.db.entity.question.Question

@Dao
interface QuestionDao {

    @Insert
    fun insert(question: Question): Long

    @Insert
    fun insert(questions: List<Question>)

    @Query ("SELECT * FROM question WHERE paperId = :paperId")
    fun getByPaperId(paperId: Int): LiveData<List<Question>>

    @Query ("SELECT * FROM question WHERE paperId = :paperId")
    fun getEntityByPaperId(paperId: Int): List<Question>

    @Query ("SELECT * FROM question WHERE paperId = :paperId ORDER BY questionPosition")
    fun getEntityWithContentByPaperId(paperId: Int): List<QuestionWithContent>

    @Query ("SELECT * FROM question WHERE id = :id")
    fun getEntityById(id: Int): Question

    @Query ("SELECT * FROM question WHERE id = :id")
    fun getEntityWithContentById(id: Int): QuestionWithContent

    // 找到position的上一个试题
    @Query("SELECT * FROM question WHERE paperId = :paperId AND questionPosition < :position ORDER BY questionPosition DESC LIMIT 1")
    fun getPrePositionQuestion(paperId: Int, position: Int): Question?

    // 找到position的下一个试题
    @Query("SELECT * FROM question WHERE paperId = :paperId AND questionPosition > :position ORDER BY questionPosition ASC LIMIT 1")
    fun getNextPositionQuestion(paperId: Int, position: Int): Question?

    @Query ("SELECT COUNT() FROM question WHERE paperId = :paperId")
    fun getCountWithPaperId(paperId: Int): Long

    @Query("SELECT questionPosition FROM question WHERE paperId = :paperId ORDER BY questionPosition DESC LIMIT 1")
    fun getMaxPosition(paperId: Int): Int

    @Update
    fun update(questions: List<Question>)

    @Update
    fun update(question: Question)

    @Delete
    fun delete(questions: List<Question>)

}

data class QuestionWithContent(
    @Embedded val question: Question,
    @Relation(
        entity = Content::class,
        parentColumn = "id",
        entityColumn = "questionId"
    )
    val contents: List<ContentWithElement>)

data class ContentWithElement(
    @Embedded val content: Content,
    @Relation(
        parentColumn = "contentId",
        entityColumn = "parentId"
    )
    var elements: List<Element>
)