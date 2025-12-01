package com.gorden.dayexam.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gorden.dayexam.db.entity.PaperInfo

@Dao
interface PaperDao {

    @Insert
    fun insert(paperInfos: List<PaperInfo>)

    @Insert
    fun insert(paperInfo: PaperInfo): Long

    @Update
    fun update(paperInfos: List<PaperInfo>)

    @Delete
    fun delete(paperInfos: List<PaperInfo>)

    @Update
    fun update(paperInfo: PaperInfo)

    @Delete
    fun delete(paperInfo: PaperInfo)

    @Query("DELETE FROM paper WHERE id = :id")
    fun delete(id: Int)

    @Query("SELECT * FROM paper WHERE id = :id")
    fun getById(id: Int): LiveData<PaperInfo>

    @Query("SELECT * FROM paper WHERE id = :id")
    fun getEntityById(id: Int): PaperInfo?

    @Query("SELECT * FROM paper ORDER BY position DESC")
    fun getAllPapers(): LiveData<List<PaperInfo>>

    @Query("SELECT position FROM paper ORDER BY position DESC LIMIT 1")
    fun getMaxPosition(): Int
}