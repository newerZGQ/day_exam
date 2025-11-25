package com.gorden.dayexam.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gorden.dayexam.db.entity.StudyStatus

@Dao
interface StudyStatusDao {

    @Insert
    fun insert(studyStatus: StudyStatus)

    @Insert
    fun insert(studyStatuses: List<StudyStatus>)

    @Query("SELECT * FROM study_status WHERE type = :type AND contentId = :contentId")
    fun queryEntityByTypeAndContentId(type: Int, contentId: Int): StudyStatus

    @Query("SELECT * FROM study_status WHERE type = :type AND contentId = :contentId")
    fun queryByTypeAndContentId(type: Int, contentId: Int): LiveData<StudyStatus>

    @Update
    fun update(studyStatus: StudyStatus)

    @Delete
    fun delete(studyStatus: StudyStatus)

    @Query("DELETE FROM study_status WHERE id = :id")
    fun delete(id: Int)

    @Query("DELETE FROM study_status WHERE type = :type AND contentId = :contentId")
    fun delete(type: Int, contentId: Int)
}