package com.gorden.dayexam.ui.home.shortcut

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.SimpleQuestionListWithDetail

class FastSelectViewModel: ViewModel() {

    private var questionDetail = MutableLiveData<SimpleQuestionListWithDetail>()

    fun currentQuestionDetail(paperId: Int): LiveData<SimpleQuestionListWithDetail> {
        DataRepository.simpleQuestionListWithDetail(paperId, questionDetail)
        return questionDetail
    }
}