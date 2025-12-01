package com.gorden.dayexam.repository.model.question

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "element")
data class Element (
    val elementType: Int,
    val content: String,
    var parentId: Int,
    var position: Int) {
    companion object {
        const val TEXT = 0
        const val PICTURE = 1
    }
    @PrimaryKey(autoGenerate = true) var id: Int = 0
    var createTime = Date()
    var editTime = Date()
}