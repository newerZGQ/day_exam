package com.gorden.dayexam.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.gorden.dayexam.db.entity.DContext
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.QuestionDetail

class HomeViewModel : ViewModel() {

    private val dContext = DataRepository.getDContext()
    private var questionDetail = MutableLiveData<QuestionDetail>()
    private var courseId = 0
    private var bookId = 0
    private var paperId = 0
    private var version: Int = 0

    private val currentQuestionDetail = Transformations.switchMap(dContext) {
        it?.let {
            if (isQuestionsChanged(it) || isFirstTimeIn() || versionChanged(it)) {
                DataRepository.currentQuestionDetail(questionDetail)
            }
            courseId = it.curCourseId
            bookId = it.curBookId
            paperId = it.curPaperId
            version = it.version
            questionDetail
        }
    }

    private fun isQuestionsChanged(dContext: DContext): Boolean {
        if ((courseId != 0 && courseId != dContext.curCourseId) ||
            (bookId != 0 && bookId != dContext.curBookId) ||
            (paperId != 0 && paperId != dContext.curPaperId)) {
            return true
        }
        return false
    }

    private fun isFirstTimeIn(): Boolean {
        if (courseId == 0 || bookId == 0 || paperId == 0) {
            return true
        }
        return false
    }

    private fun versionChanged(dContext: DContext): Boolean {
        return version != dContext.version
    }

    fun currentQuestionDetail(): LiveData<QuestionDetail> {
        return currentQuestionDetail
    }

}
