package com.gorden.dayexam.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity (tableName = "course")
data class Course(
    var title: String,
    var description: String,
    var position: Int
) {
    @PrimaryKey (autoGenerate = true) var id: Int = 0
    var createTime = Date()
    var editTime = Date()
    var isRecycleBin = false
}
