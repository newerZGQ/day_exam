package com.gorden.dayexam.utils

import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.model.QuestionType

object NameUtils {
    fun getTypeName(type: Int): String {
        val context = ContextHolder.application
        when(type){
            QuestionType.FILL_BLANK -> {
                return context.resources.getString(R.string.fill_blank)
            }
            QuestionType.TRUE_FALSE -> {
                return context.resources.getString(R.string.true_false)
            }
            QuestionType.SINGLE_CHOICE -> {
                return context.resources.getString(R.string.single_choice)
            }
            QuestionType.MULTIPLE_CHOICE -> {
                return context.resources.getString(R.string.multiple_choice)
            }
            QuestionType.ESSAY_QUESTION -> {
                return context.resources.getString(R.string.essay_question)
            }
        }
        return ""
    }

    fun generateImageName(oriName: String, timeStamp: Long): String {
        val res: Long = 71L + timeStamp + oriName.hashCode()
        return res.toString(16)
    }
}