package com.gorden.dayexam.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gorden.dayexam.db.entity.Book

@Dao
interface BookDao {

    @Insert
    fun insert(book: Book): Long

    @Insert
    fun insert(books: List<Book>)

    @Query("SELECT * FROM book WHERE id = :bookId")
    fun getById(bookId: Int): LiveData<Book>

    @Query("SELECT * FROM book WHERE id = :bookId")
    fun getEntity(bookId: Int): Book?

    @Query ("SELECT * FROM book WHERE courseId = :courseId ORDER BY position ASC")
    fun getBookEntitiesByCourseId(courseId: Int): List<Book>

    @Query ("SELECT * FROM book WHERE courseId = :courseId ORDER BY position ASC")
    fun getBookByCourseId(courseId: Int): LiveData<List<Book>>

    @Update
    fun update(books: List<Book>)

    @Update
    fun update(book: Book)

    @Query("SELECT position FROM book WHERE courseId = :courseId ORDER BY position DESC LIMIT 1")
    fun getMaxOrder(courseId: Int): Int

    @Query("DELETE FROM book WHERE id = :id")
    fun delete(id: Int)

    @Delete
    fun delete(books: List<Book>)

    @Delete
    fun delete(book: Book)
}