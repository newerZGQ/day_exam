package com.gorden.dayexam.db.entity.tip

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "tip")
data class Tip (
    val parentId: Int,
    val content: String )
{
    @PrimaryKey(autoGenerate = true) var id: Int = 0
    var createTime = Date()
    var editTime = Date()
}