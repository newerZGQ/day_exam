package com.gorden.dayexam.ui.book

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.BookWithPaper

class BookViewModel(application: Application): AndroidViewModel(application) {

    private val dContext = DataRepository.getDContext()
    private val bookDetail = MutableLiveData<BookDetail>()

    private val currentBookDetail = Transformations.switchMap(dContext){
        it?.let {
            DataRepository.currentBookDetail(bookDetail)
            bookDetail
        }
    }

    fun getBookDetail(): LiveData<BookDetail> {
        return currentBookDetail
    }

}

data class BookDetail(
    var courseId: Int,
    var courseTitle: String,
    var bookId: Int,
    var paperId: Int,
    var isRecycleBin: Boolean,
    var books: List<BookWithPaper>
)