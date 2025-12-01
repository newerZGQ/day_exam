package com.gorden.dayexam.repository.model

class QuestionDetail (
    val type: Int,
    val body: List<Element>,
    val options: List<OptionItems>,
    val answer: List<Element>,
    var realAnswer: RealAnswer? = null
)

data class OptionItems(
    val element: List<Element>
)

data class RealAnswer(
    var answer: String = ""
)
