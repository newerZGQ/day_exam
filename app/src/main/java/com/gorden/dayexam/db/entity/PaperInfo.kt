package com.gorden.dayexam.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity (tableName = "paper")
data class PaperInfo(
    var title: String,
    var description: String,
    var path: String,
    var position: Int,
    val lastStudyPosition: Int = 0,
    val questionCount: Int = 0,
) {
    var createTime = Date()
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}
