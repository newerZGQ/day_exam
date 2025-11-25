package com.gorden.dayexam.db.converter

import android.annotation.SuppressLint
import androidx.room.TypeConverter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class DateConverter {
    @SuppressLint("SimpleDateFormat")
    @TypeConverter
    fun toDate(timestamp: String?): Date? {
        if (timestamp.isNullOrEmpty()) return null
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
        formatter.timeZone = TimeZone.getDefault()
        try {
            return formatter.parse(timestamp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @SuppressLint("SimpleDateFormat")
    @TypeConverter
    fun toTimestamp(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
        return formatter.format(date)
    }

}