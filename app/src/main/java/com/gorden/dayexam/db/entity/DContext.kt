package com.gorden.dayexam.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "d_context")
data class DContext(
    var curPaperId: Int,
    var curQuestionId: Int,
)