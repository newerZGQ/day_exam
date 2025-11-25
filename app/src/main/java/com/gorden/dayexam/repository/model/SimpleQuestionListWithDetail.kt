package com.gorden.dayexam.repository.model

data class SimpleQuestionListWithDetail (
    var paperTitle: String,
    var paperId: Int,
    var curQuestionId: Int,
    var questions: List<QuestionWithElement>
)