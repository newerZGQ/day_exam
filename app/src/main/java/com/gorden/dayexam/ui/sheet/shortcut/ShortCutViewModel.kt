package com.gorden.dayexam.ui.sheet.shortcut

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.gorden.dayexam.db.entity.Config
import com.gorden.dayexam.db.entity.DContext
import com.gorden.dayexam.repository.DataRepository

class ShortCutViewModel(application: Application): AndroidViewModel(application) {

    fun getConfig(): LiveData<Config> {
        return DataRepository.getConfig()
    }

    fun getDContext(): LiveData<DContext> {
        return DataRepository.getDContext()
    }
}