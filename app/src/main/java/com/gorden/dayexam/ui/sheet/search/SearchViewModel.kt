package com.gorden.dayexam.ui.sheet.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.BookWithPaper
import com.gorden.dayexam.repository.model.QuestionWithElement
import com.gorden.dayexam.repository.model.SearchItem
import com.gorden.dayexam.ui.book.BookDetail

class SearchViewModel: ViewModel() {
    private val searchResult = MutableLiveData<List<SearchItem>>()

    fun search(scope: Int, key: String){
        DataRepository.searchByScopeAndKey(scope, key, searchResult)
    }

    fun searchResult(): LiveData<List<SearchItem>> {
        return searchResult
    }
}