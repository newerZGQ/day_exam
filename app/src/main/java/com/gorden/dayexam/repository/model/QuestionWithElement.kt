package com.gorden.dayexam.repository.model

import com.gorden.dayexam.repository.model.question.Element

class QuestionWithElement (
    val id: Int,
    val type: Int,
    val body: BodyWithElement,
    val options: List<OptionItemWithElement>,
    val answer: AnswerWithElement )
{
    var realAnswer: RealAnswer? = null
    var studyCount = 0
}

data class BodyWithElement(
    val id: Int,
    val element: List<Element>
)

data class AnswerWithElement(
    val id: Int,
    val element: List<Element>
)

data class OptionItemWithElement(
    val id: Int,
    val element: List<Element>
)

data class RealAnswer(
    var answer: String = ""
)
