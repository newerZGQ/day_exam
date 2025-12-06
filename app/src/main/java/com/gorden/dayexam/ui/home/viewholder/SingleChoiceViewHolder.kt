package com.gorden.dayexam.ui.home.viewholder

import android.view.View
import android.widget.LinearLayout
import androidx.core.view.children
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.db.entity.StudyRecord
import com.gorden.dayexam.repository.model.Answer
import com.gorden.dayexam.repository.model.Element
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.widget.AnswerCardView
import com.gorden.dayexam.ui.widget.ElementViewListener
import com.gorden.dayexam.ui.widget.OptionCardView
import com.gorden.dayexam.utils.ScreenUtils
import com.gorden.dayexam.utils.showOrGone
import com.jeremyliao.liveeventbus.LiveEventBus

class SingleChoiceViewHolder(itemView: View): BaseQuestionViewHolder(itemView) {

    private val optionContainer: LinearLayout = itemView.findViewById(R.id.options_container)

    private val answerContainer: View = itemView.findViewById(R.id.answer_container)
    private val answer: AnswerCardView = itemView.findViewById(R.id.answer)

    override fun setContent(
        paperInfo: PaperInfo,
        question: QuestionDetail,
        isRememberMode: Boolean
    ) {
        if (isRememberMode) {
            question.realAnswer = null
        }
        setOptionsView(paperInfo, question, isRememberMode)
        setAnswerView(paperInfo, question, isRememberMode)
    }

    private fun setOptionsView(paperInfo: PaperInfo, question: QuestionDetail, isRememberMode: Boolean) {
        if (isRememberMode) {
            switchToRememberOptionsView(paperInfo, question)
            return
        }
        if (question.realAnswer != null) {
            switchToAnsweredOptionsView(paperInfo, question)
        } else {
            switchToCommonOptionsView(paperInfo, question)
        }
    }

    private fun setAnswerView(paperInfo: PaperInfo, question: QuestionDetail, isRememberMode: Boolean) {
        if (isRememberMode) {
            answerContainer.showOrGone(false)
            return
        }
        if (question.realAnswer != null) {
            answerContainer.showOrGone(true)
            genAnswerView(paperInfo, question)
        } else {
            answerContainer.showOrGone(false)
        }
    }

    private fun setAnsweredStatus(paperInfo: PaperInfo, question: QuestionDetail) {
        switchToAnsweredOptionsView(paperInfo, question)
        optionContainer.children.forEach {
            it.setOnClickListener(null)
        }
    }

    private fun switchToCommonOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        optionContainer.removeAllViews()
        question.options.forEachIndexed { index, optionItemWithElement ->
            val optionTag = (index + 'A'.toInt()).toChar().toString()
            val optionCardView = OptionCardView(itemView.context)
            optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_default_color))
            optionCardView.setContent(paperInfo, optionItemWithElement.element, optionTag, ElementViewListener())
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.topMargin = ScreenUtils.dp2px(8f)
            optionContainer.addView(optionCardView, layoutParams)
            optionCardView.setOnClickListener {
                val realAnswer = Answer(optionAnswer = listOf(index))
                question.realAnswer = realAnswer
                resetAllStatus()
                val isCorrectTag = getAnswerEventTag(question)
                LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                    .post(EventKey.AnswerEventModel(isCorrectTag))
            }
        }
    }

    private fun switchToAnsweredOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        question.options.forEachIndexed { index, optionItemWithElement ->
            val realAnswer = question.realAnswer?.optionAnswer?.firstOrNull() ?: -1
            val correctAnswer = question.answer.optionAnswer.firstOrNull() ?: -1

            val optionCardView = optionContainer.getChildAt(index)
            if (correctAnswer == index) {
                optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_select_correct_color))
            } else if (realAnswer == index) {
                optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_select_incorrect_color))
            } else {
                optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_default_color))
            }
            optionCardView.setOnClickListener {}
        }
    }

    private fun switchToRememberOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        optionContainer.removeAllViews()
        val correctAnswer = question.answer.optionAnswer.firstOrNull()
        question.options.forEachIndexed { index, optionItemWithElement ->
            val optionTag = (index + 'A'.toInt()).toChar().toString()
            val optionCardView = OptionCardView(itemView.context)
            if (correctAnswer == index) {
                optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_select_correct_color))
            } else {
                optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_default_color))
            }
            optionCardView.setContent(paperInfo, optionItemWithElement.element, optionTag, ElementViewListener())
            optionCardView.setOnClickListener {}
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.topMargin = ScreenUtils.dp2px(8f)
            optionContainer.addView(optionCardView, layoutParams)
        }
    }

    private fun genAnswerView(paperInfo: PaperInfo, question: QuestionDetail) {
        val answerText = question.answer.optionAnswer.sortedBy { it }.map {
            (it + 'A'.toInt()).toChar().toString()
        }.toString()
        val elements = listOf(Element(content = answerText, elementType = Element.TEXT))
        val realAnswer = question.realAnswer?.optionAnswer?.sortedBy { it }?.map {
            (it + 'A'.toInt()).toChar().toString()
        }.toString()
        answer.setElements(
            paperInfo = paperInfo, elements = elements, "", realAnswer,
            listener = { target, elements ->

            },
        )
    }

    private fun getAnswerEventTag(question: QuestionDetail): Int {
        val answer = question.answer.optionAnswer.firstOrNull()
        val realAnswer = question.realAnswer?.optionAnswer?.firstOrNull()
        return if (answer == realAnswer) {
            StudyRecord.CORRECT
        } else {
            StudyRecord.IN_CORRECT
        }
    }

}