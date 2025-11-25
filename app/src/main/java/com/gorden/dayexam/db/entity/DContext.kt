package com.gorden.dayexam.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "d_context")
data class DContext(
    var curCourseId: Int,
    var curBookId: Int,
    var curPaperId: Int,
    var curQuestionId: Int,
    var recycleBinId: Int,
    var recycleBookId: Int,
    var recyclePaperId: Int
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
    // 内容的版本号，当修改引起了试题内容的变更后，可以主动加1来主动触发全局更新，如果认为不需要触发全局更新，可以不管
    var version: Int = 0
}