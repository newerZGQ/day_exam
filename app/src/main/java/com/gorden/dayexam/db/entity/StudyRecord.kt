package com.gorden.dayexam.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.lang.StringBuilder
import java.util.*

@Entity(tableName = "study_record")
data class StudyRecord (
    val paperId: Int,
    val content: String,
    val correct: Int) {

    companion object {
        const val IN_CORRECT = 0
        const val CORRECT = 1
        const val NOT_AVAILABLE = -1

        fun parseFromBoolean(isCorrect: Boolean): Int {
            return if (isCorrect) CORRECT else IN_CORRECT
        }
    }

    @PrimaryKey(autoGenerate = true) var id: Int = 0
    var createTime = Date()
    var editTime = Date()
}
