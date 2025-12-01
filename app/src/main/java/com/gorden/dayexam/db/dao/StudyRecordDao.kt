package com.gorden.dayexam.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gorden.dayexam.db.entity.StudyRecord

@Dao
interface StudyRecordDao {

    @Insert
    fun insert(studyRecord: StudyRecord): Long

    @Query("SELECT COUNT(id) FROM study_record WHERE paperId = :paperId")
    fun getPaperStudyCount(paperId: Int): Long

    @Query("SELECT COUNT(id) FROM study_record WHERE createTime > :date")
    fun getStudyCountAfter(date: String): LiveData<Long>
}