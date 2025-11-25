package com.gorden.dayexam.db.entity.question

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "content")
data class Content(
    val questionId: Int,
    val contentType: Int
) {
    companion object {
        const val BODY_TYPE = 1
        const val OPTION_TYPE = 2
        const val ANSWER_TYPE = 3
    }
    @PrimaryKey(autoGenerate = true) var contentId: Int = 0
    var createTime = Date()
    var editTime = Date()
}