package com.gorden.dayexam.repository.model.question

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity (tableName = "question")
data class Question(
    var paperId: Int,
    val questionType: Int,
    var questionPosition: Int
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
    var isFavorite = false
    var createTime = Date()
    var editTime = Date()
}
