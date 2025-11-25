package com.gorden.dayexam.ui.action

import android.content.Context
import android.widget.Toast
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.DeleteQuestionCallback

class DeleteCurrentQuestionAction(val context: Context): Action {
    override fun start() {
        DataRepository.deleteCurrentQuestion(object : DeleteQuestionCallback {
            override fun onFinished(success: Boolean, msg: String) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                if (success) {
                    DataRepository.increaseContentVersion()
                }
            }
        })
    }
}