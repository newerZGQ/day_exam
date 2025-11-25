package com.gorden.dayexam.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.gorden.dayexam.db.entity.question.Content

@Dao
interface ContentDao {
    @Insert
    fun insert(content: Content): Long

    @Query("SELECT * FROM content WHERE questionId = :questionId AND contentType = 1")
    fun getBodyEntity(questionId: Int): Content

    @Query("SELECT * FROM content WHERE questionId = :questionId AND contentType = 2")
    fun getOptionEntity(questionId: Int): List<Content>

    @Query("SELECT * FROM content WHERE questionId = :questionId AND contentType = 3")
    fun getAnswerEntity(questionId: Int): Content

    @Delete
    fun delete(content: Content)

    @Query("DELETE FROM content WHERE questionId = :questionId")
    fun delete(questionId: Int)
}