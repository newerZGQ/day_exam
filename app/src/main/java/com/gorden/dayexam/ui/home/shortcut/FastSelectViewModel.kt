package com.gorden.dayexam.ui.home.shortcut

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gorden.dayexam.repository.model.QuestionDetail

class FastSelectViewModel: ViewModel() {

    private var questionDetail = MutableLiveData<List<QuestionDetail>>()

    fun currentQuestionDetail(paperId: Int): LiveData<List<QuestionDetail>> {
        return questionDetail
    }
}