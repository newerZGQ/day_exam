package com.gorden.dayexam.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gorden.dayexam.db.entity.Paper

@Dao
interface PaperDao {

    @Insert
    fun insert(papers: List<Paper>)

    @Insert
    fun insert(paper: Paper): Long

    @Update
    fun update(papers: List<Paper>)

    @Delete
    fun delete(papers: List<Paper>)

    @Update
    fun update(paper: Paper)

    @Delete
    fun delete(paper: Paper)

    @Query("DELETE FROM paper WHERE id = :id")
    fun delete(id: Int)

    @Query("SELECT * FROM paper WHERE id = :id")
    fun getById(id: Int): LiveData<Paper>

    @Query("SELECT * FROM paper WHERE id = :id")
    fun getEntityById(id: Int): Paper?

    @Query("SELECT * FROM paper WHERE bookId = :bookId ORDER BY position ASC")
    fun getEntityByBookIdOrderByPosition(bookId: Int): List<Paper>

    @Query("SELECT * FROM paper WHERE bookId = :bookId ORDER BY editTime DESC")
    fun getEntityByBookIdOrderByEditTime(bookId: Int): List<Paper>

    @Query("SELECT position FROM paper WHERE bookId = :bookId ORDER BY position DESC LIMIT 1")
    fun getMaxPosition(bookId: Int): Int

    @Query("SELECT * FROM paper WHERE bookId = :bookId ORDER BY position ASC")
    fun getByBookId(bookId: Int): LiveData<List<Paper>>
}