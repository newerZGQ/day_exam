package com.gorden.dayexam.ui.sheet.shortcut

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.gorden.dayexam.db.entity.Config
import com.gorden.dayexam.repository.DataRepository

class ShortCutViewModel(application: Application): AndroidViewModel(application) {

    fun getConfig(): LiveData<Config> {
        return DataRepository.getConfig()
    }

    fun getCurPaperId(): LiveData<Int> {
        return DataRepository.getCurPaperId()
    }

    fun getCurQuestionId(): LiveData<Int> {
        return DataRepository.getCurQuestionId()
    }
}