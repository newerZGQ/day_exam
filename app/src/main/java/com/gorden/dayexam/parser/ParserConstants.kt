package com.gorden.dayexam.parser

class ParserConstants {
    companion object {
        const val SEPARATOR = "&&"
        const val FILL_BLANK_SEPARATOR = SEPARATOR + "填空"
        val FILL_BLANK_SEPARATOR_EN = SEPARATOR + "Completion".lowercase()
        const val TRUE_FALSE_SEPARATOR = SEPARATOR + "判断"
        val TRUE_FALSE_SEPARATOR_EN = SEPARATOR + "True False".lowercase()
        const val SINGLE_CHOICE_SEPARATOR = SEPARATOR + "单选"
        val SINGLE_CHOICE_SEPARATOR_EN = SEPARATOR + "Single Answer".lowercase()
        const val MULTIPLE_CHOICE_SEPARATOR = SEPARATOR + "多选"
        val MULTIPLE_CHOICE_SEPARATOR_EN = SEPARATOR + "Multiple Answer".lowercase()
        const val ESSAY_QUESTION_SEPARATOR = SEPARATOR + "问答"
        val ESSAY_QUESTION_SEPARATOR_EN = SEPARATOR + "Discussion Essays".lowercase()
        const val OPTION_SEPARATOR = SEPARATOR + "选项"
        val OPTION_SEPARATOR_EN = SEPARATOR + "Option".lowercase()
        const val ANSWER_SEPARATOR = SEPARATOR + "答案"
        val ANSWER_SEPARATOR_EN = SEPARATOR + "Answer".lowercase()
    }
}