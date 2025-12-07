package com.gorden.dayexam.repository.model

data class SearchItem(
    var paperId: Int,
    var paperTitle: String,
    var questionIndex: Int,
    var questionType: Int,
    var elementType: Int,
    var elementContent: String
)