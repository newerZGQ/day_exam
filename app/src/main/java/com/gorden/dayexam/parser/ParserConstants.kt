package com.gorden.dayexam.parser

import com.gorden.dayexam.repository.model.QuestionType

object ParserConstants {
    const val SEPARATOR = "&&"

    private const val FILL_BLANK_SEPARATOR = SEPARATOR + "填空"
    private const val FILL_BLANK_SEPARATOR_EN = SEPARATOR + "completion"

    private const val TRUE_FALSE_SEPARATOR = SEPARATOR + "判断"
    private const val TRUE_FALSE_SEPARATOR_EN = SEPARATOR + "true false"
    private const val TRUE_FALSE_T_ANSWER = "对"
    private const val TRUE_FALSE_T_ANSWER_EN = "true"
    private const val TRUE_FALSE_F_ANSWER = "错"
    private const val TRUE_FALSE_F_ANSWER_EN = "false"

    private const val SINGLE_CHOICE_SEPARATOR = SEPARATOR + "单选"
    private const val SINGLE_CHOICE_SEPARATOR_EN = SEPARATOR + "single answer"

    private const val MULTIPLE_CHOICE_SEPARATOR = SEPARATOR + "多选"
    private const val MULTIPLE_CHOICE_SEPARATOR_EN = SEPARATOR + "multiple answer"

    private const val ESSAY_QUESTION_SEPARATOR = SEPARATOR + "问答"
    private const val ESSAY_QUESTION_SEPARATOR_EN = SEPARATOR + "discussion essays"

    private const val OPTION_SEPARATOR = SEPARATOR + "选项"
    private const val OPTION_SEPARATOR_EN = SEPARATOR + "option"

    private const val ANSWER_SEPARATOR = SEPARATOR + "答案"
    private const val ANSWER_SEPARATOR_EN = SEPARATOR + "answer"

    fun getQuestionType(typeText: String): Int {
        when {
            typeText.lowercase().startsWith(FILL_BLANK_SEPARATOR) ||
                    typeText.lowercase().startsWith(FILL_BLANK_SEPARATOR_EN) -> {
                return QuestionType.FILL_BLANK
            }
            typeText.lowercase().startsWith(TRUE_FALSE_SEPARATOR) ||
                    typeText.lowercase().startsWith(TRUE_FALSE_SEPARATOR_EN) -> {
                return QuestionType.TRUE_FALSE
            }
            typeText.lowercase().startsWith(SINGLE_CHOICE_SEPARATOR) ||
                    typeText.lowercase().startsWith(SINGLE_CHOICE_SEPARATOR_EN) -> {
                return QuestionType.SINGLE_CHOICE
            }
            typeText.lowercase().startsWith(MULTIPLE_CHOICE_SEPARATOR) ||
                    typeText.lowercase().startsWith(MULTIPLE_CHOICE_SEPARATOR_EN) -> {
                return QuestionType.MULTIPLE_CHOICE
            }
            typeText.lowercase().startsWith(ESSAY_QUESTION_SEPARATOR) ||
                    typeText.lowercase().startsWith(ESSAY_QUESTION_SEPARATOR_EN) -> {
                return QuestionType.ESSAY_QUESTION
            }
            else -> {
                return QuestionType.ERROR_TYPE
            }
        }
    }

    fun isQuestionSeparator(text: String): Boolean {
        if (text.isEmpty()) return false
        return text.lowercase().startsWith(FILL_BLANK_SEPARATOR) ||
                text.lowercase().startsWith(TRUE_FALSE_SEPARATOR) ||
                text.lowercase().startsWith(SINGLE_CHOICE_SEPARATOR) ||
                text.lowercase().startsWith(MULTIPLE_CHOICE_SEPARATOR) ||
                text.lowercase().startsWith(ESSAY_QUESTION_SEPARATOR) ||
                text.lowercase().startsWith(FILL_BLANK_SEPARATOR_EN) ||
                text.lowercase().startsWith(TRUE_FALSE_SEPARATOR_EN) ||
                text.lowercase().startsWith(SINGLE_CHOICE_SEPARATOR_EN) ||
                text.lowercase().startsWith(MULTIPLE_CHOICE_SEPARATOR_EN) ||
                text.lowercase().startsWith(ESSAY_QUESTION_SEPARATOR_EN)
    }

    fun isOption(optionText: String): Boolean {
        return optionText.startsWith(OPTION_SEPARATOR) ||
                optionText.lowercase().startsWith(OPTION_SEPARATOR_EN)
    }

    fun isAnswer(answerText: String): Boolean {
        return answerText.startsWith(ANSWER_SEPARATOR) ||
                answerText.lowercase().startsWith(ANSWER_SEPARATOR_EN)
    }

    fun toTrueFalseAnswer(answer: String): Boolean {
        return answer == TRUE_FALSE_T_ANSWER || answer == TRUE_FALSE_T_ANSWER_EN
    }
}