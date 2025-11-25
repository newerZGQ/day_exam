package com.gorden.dayexam.repository.model

data class QuestionDetail (
    var courseTitle: String,
    var bookTitle: String,
    var bookId: Int,
    var paperTitle: String,
    var paperId: Int,
    var curQuestionId: Int,
    var questions: List<QuestionWithElement>
)