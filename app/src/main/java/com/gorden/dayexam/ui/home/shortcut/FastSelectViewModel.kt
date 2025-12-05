package com.gorden.dayexam.ui.home.shortcut

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.QuestionDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FastSelectViewModel: ViewModel() {

    suspend fun currentQuestionDetail(paperId: Int): List<QuestionDetail> {
        return withContext(Dispatchers.IO) {
            DataRepository.getQuestionsByPaperId(paperId)
        }
    }
}