package com.gorden.dayexam.ui.book

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.repository.DataRepository

class PaperListViewModel(application: Application): AndroidViewModel(application) {

    private val allPapers = DataRepository.getAllPapers()

    fun getAllPapers(): LiveData<List<PaperInfo>> {
        return allPapers
    }

}