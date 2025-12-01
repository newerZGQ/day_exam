package com.gorden.dayexam.db.entity

import androidx.room.Entity
import java.util.*

@Entity (tableName = "paper")
data class PaperInfo(
    var title: String,
    var description: String,
    var path: String,
    var position: Int,
    val lastStudyPosition: Int,
    val questionCount: Int,
) {
    var createTime = Date()
}
