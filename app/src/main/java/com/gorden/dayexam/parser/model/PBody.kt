package com.gorden.dayexam.parser.model

import com.gorden.dayexam.repository.model.question.Element

data class PBody (
    val elements: List<Element> )

data class PAnswer (
    val elements: List<Element> )

data class POptionItem (
    val elements: List<Element> )