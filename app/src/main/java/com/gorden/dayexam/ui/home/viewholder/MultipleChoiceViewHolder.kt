package com.gorden.dayexam.ui.home.viewholder

import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
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

class MultipleChoiceViewHolder(itemView: View): BaseQuestionViewHolder(itemView) {

    private var selectedOptions = mutableListOf<Int>()

    private val optionContainer: LinearLayout = itemView.findViewById(R.id.options_container)
    private val answerContainer: View = itemView.findViewById(R.id.answer_container)
    private val answer: AnswerCardView = itemView.findViewById(R.id.answer)
    private val action = itemView.findViewById<View>(R.id.action)

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
        setActionView(paperInfo, question, isRememberMode)
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

    private fun setActionView(paperInfo: PaperInfo, question: QuestionDetail, isRememberMode: Boolean) {
        if (isRememberMode) {
            action.showOrGone(false)
            return
        }
        if (question.realAnswer != null) {
            action.showOrGone(false)
        } else {
            action.showOrGone(true)
        }

        val context = itemView.context
        action.setOnClickListener {
            if (selectedOptions.isEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_please_select_option),
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            question.realAnswer = Answer(optionAnswer = selectedOptions.sortedBy { it })
            resetAllStatus()
            val isCorrect = isCorrect(question)
            LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                .post(EventKey.AnswerEventModel(
                    StudyRecord.parseFromBoolean(isCorrect)))
        }
    }

    private fun switchToCommonOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        optionContainer.removeAllViews()
        question.options.forEachIndexed { index, optionItemWithElement ->
            val optionTag = (index + 'A'.toInt()).toChar()
            val optionCardView = OptionCardView(itemView.context)
            optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_default_color))
            optionCardView.setContent(paperInfo, optionItemWithElement.element, optionTag.toString(), ElementViewListener())
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.topMargin = ScreenUtils.dp2px(8f)
            optionContainer.addView(optionCardView, layoutParams)
            optionCardView.setOnClickListener {
                if (selectedOptions.contains(index)) {
                    selectedOptions.remove(index)
                    optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_default_color))
                } else {
                    selectedOptions.add(index)
                    optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_select_correct_color))
                }
            }
        }
    }

    private fun switchToAnsweredOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        val selectedOptions = question.realAnswer?.optionAnswer
        if (selectedOptions.isNullOrEmpty()) {
            return
        }
        val answer = question.answer.optionAnswer
        val context = itemView.context
        if (answer.isEmpty()) {
            Toast.makeText(
                context,
                context.getString(R.string.toast_please_check_answer_content),
                Toast.LENGTH_SHORT).show()
            return
        }
        question.options.forEachIndexed { index, optionItemWithElement ->
            if (answer.contains(index) && selectedOptions.contains(index)) {
                optionContainer.getChildAt(index)
                    .setBackgroundColor(context.getColor(R.color.option_select_correct_color))
            } else if (answer.contains(index) && !selectedOptions.contains(index)) {
                optionContainer.getChildAt(index)
                    .setBackgroundColor(context.getColor(R.color.option_select_missed_color))
            } else if (!answer.contains(index) && selectedOptions.contains(index)) {
                optionContainer.getChildAt(index)
                    .setBackgroundColor(context.getColor(R.color.option_select_incorrect_color))
            } else {
                optionContainer.getChildAt(index)
                    .setBackgroundColor(context.getColor(R.color.option_default_color))
            }

        }
    }

    private fun switchToRememberOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        optionContainer.removeAllViews()
        val correctAnswer = question.answer.optionAnswer
        question.options.forEachIndexed { index, optionItemWithElement ->
            val optionTag = (index + 'A'.toInt()).toChar()
            val optionCardView = OptionCardView(itemView.context)
            if (correctAnswer.contains(index)) {
                optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_select_correct_color))
            } else {
                optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_default_color))
            }
            optionCardView.setContent(paperInfo, optionItemWithElement.element, optionTag.toString(), ElementViewListener())
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
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

    private fun isCorrect(question: QuestionDetail): Boolean {
        if (question.answer.optionAnswer.isEmpty()) {
            return false
        }
        val standardAnswer = question.answer.optionAnswer
        val realAnswer = question.realAnswer?.optionAnswer
        return standardAnswer == realAnswer
    }

}