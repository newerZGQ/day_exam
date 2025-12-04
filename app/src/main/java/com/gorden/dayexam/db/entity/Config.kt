package com.gorden.dayexam.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "Config")
data class Config (
    var rememberMode: Boolean,
    var onlyFavorite: Boolean,
    var sortByAccuracy: Boolean)
{
    @PrimaryKey(autoGenerate = true) var id: Int = 0
    var createTime = Date()
    var editTime = Date()
}