package com.gorden.dayexam.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

const val PaperStatus = 3

@Entity(tableName = "study_status")
data class StudyStatus(
    var type: Int,
    // contentId means course's id or Book's id or Paper's id
    var contentId: Int,
    // curChild means Book's id or  Paper's id or Question's id
    var curChild: Int
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
    var createTime = Date()
    var editTime = Date()
}

