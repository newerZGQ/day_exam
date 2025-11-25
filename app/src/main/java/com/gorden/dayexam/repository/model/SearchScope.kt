package com.gorden.dayexam.repository.model

class SearchScope {
    companion object {
        const val SEARCH_IN_PAPER = 1
        const val SEARCH_IN_BOOK = 2
        const val SEARCH_IN_COURSE = 3
        const val SEARCH_GLOBAL = 4
        const val SEARCH_RECYCLE_BIN = 5
    }
}

data class SearchItem(
    var courseId: Int,
    var courseTitle: String,
    var bookId: Int,
    var bookTitle: String,
    var paperId: Int,
    var paperTitle: String,
    var questionId: Int,
    var questionType: Int,
    var elementType: Int,
    var elementContent: String
)