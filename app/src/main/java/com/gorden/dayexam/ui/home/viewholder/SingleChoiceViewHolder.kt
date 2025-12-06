package com.gorden.dayexam.ui.home.viewholder

import android.view.View
import android.widget.LinearLayout
import androidx.core.view.children
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.db.entity.StudyRecord
import com.gorden.dayexam.repository.model.Answer
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.widget.ElementViewListener
import com.gorden.dayexam.ui.widget.OptionCardView
import com.gorden.dayexam.utils.ScreenUtils
import com.jeremyliao.liveeventbus.LiveEventBus

class SingleChoiceViewHolder(itemView: View): BaseQuestionViewHolder(itemView) {

    private val optionContainer: LinearLayout = itemView.findViewById(R.id.options_container)

    override fun genOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
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
                setAnsweredStatus(paperInfo, question)
                val isCorrectTag = getAnswerEventTag(question)
                LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                    .post(EventKey.AnswerEventModel(isCorrectTag))
            }
        }
    }

    override fun genAnsweredOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        question.options.forEachIndexed { index, optionItemWithElement ->
            val context = itemView.context
            val realAnswer = question.realAnswer?.optionAnswer?.firstOrNull() ?: -1
            val answer = question.answer.optionAnswer.firstOrNull() ?: -1
            if (question.realAnswer != null) {
                if (answer == realAnswer) {
                    optionContainer.getChildAt(index).setBackgroundColor(context.getColor(R.color.option_select_correct_color))
                } else {
                    optionContainer.getChildAt(index).setBackgroundColor(context.getColor(R.color.option_select_incorrect_color))
                }
            } else {
                if (answer == realAnswer) {
                    optionContainer.getChildAt(index).setBackgroundColor(context.getColor(R.color.option_select_correct_color))
                } else {
                    optionContainer.getChildAt(index).setBackgroundColor(context.getColor(R.color.option_default_color))
                }
            }

        }
    }

    override fun genRememberOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
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
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.topMargin = ScreenUtils.dp2px(8f)
            optionContainer.addView(optionCardView, layoutParams)
        }
    }

    override fun genActionView(paperInfo: PaperInfo, question: QuestionDetail) {

    }

    override fun setAnsweredStatus(paperInfo: PaperInfo, question: QuestionDetail) {
        super.setAnsweredStatus(paperInfo, question)
        optionContainer.children.forEach {
            it.setOnClickListener(null)
        }
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