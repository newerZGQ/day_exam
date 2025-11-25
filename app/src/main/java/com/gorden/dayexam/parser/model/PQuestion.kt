package com.gorden.dayexam.parser.model

data class PQuestion(
    var type: Int,
    var body: PBody,
    var answer: PAnswer,
    var options: MutableList<POptionItem>
)