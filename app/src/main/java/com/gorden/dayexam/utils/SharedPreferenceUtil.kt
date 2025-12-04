package com.gorden.dayexam.utils

import android.content.Context
import com.gorden.dayexam.ContextHolder

object SharedPreferenceUtil {
    private val sp = ContextHolder.application.getSharedPreferences("dayexam", Context.MODE_PRIVATE)

    fun getString(key: String): String {
        return sp.getString(key, "")!!
    }

    fun setString(key: String, value: String) {
        val editor = sp.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        return sp.getBoolean(key, default)
    }

    fun setBoolean(key: String, value: Boolean) {
        val editor = sp.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getInt(key: String, default: Int): Int {
        return sp.getInt(key, default)
    }

    fun setInt(key: String, value: Int) {
        val editor = sp.edit()
        editor.putInt(key, value)
        editor.apply()
    }
}