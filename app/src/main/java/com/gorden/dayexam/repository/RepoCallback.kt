package com.gorden.dayexam.repository

interface DeleteQuestionCallback {
    fun onFinished(success: Boolean, msg: String)
}

interface IsInRecycleBinCallback {
    fun onFinished(isInRecycleBin: Boolean)
}