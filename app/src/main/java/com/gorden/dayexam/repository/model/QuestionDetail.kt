package com.gorden.dayexam.repository.model

class QuestionDetail (
    val type: Int,
    val body: List<Element>,
    val options: List<OptionItems>,
    val answer: Answer,
    var realAnswer: Answer? = null
)

data class OptionItems(
    val element: List<Element>
)

data class Answer(
    val commonAnswer: List<Element> = listOf(),
    val optionAnswer: List<Int> = listOf(),
    val tfAnswer: Boolean = false
)


