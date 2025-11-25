package com.gorden.dayexam.ui.home.viewholder

import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.children
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.StudyRecord
import com.gorden.dayexam.repository.model.QuestionWithElement
import com.gorden.dayexam.repository.model.RealAnswer
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.action.EditQuestionContentAction
import com.gorden.dayexam.ui.widget.ElementViewListener
import com.gorden.dayexam.ui.widget.OptionCardView
import com.gorden.dayexam.utils.ScreenUtils
import com.jeremyliao.liveeventbus.LiveEventBus

class MultipleChoiceViewHolder(itemView: View): BaseQuestionViewHolder(itemView) {

    private val optionContainer: LinearLayout = itemView.findViewById(R.id.options_container)

    override fun genOptionsView(question: QuestionWithElement) {
        optionContainer.removeAllViews()
        question.options.forEachIndexed { index, optionItemWithElement ->
            val optionTag = (index + 'A'.toInt()).toChar()
            val optionCardView = OptionCardView(itemView.context)
            optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_default_color))
            optionCardView.setContent(optionItemWithElement.element, optionTag.toString(), ElementViewListener())
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.topMargin = ScreenUtils.dp2px(8f)
            optionContainer.addView(optionCardView, layoutParams)
            optionCardView.setOnClickListener {
                val realAnswerContent = (index + 'A'.toInt()).toChar().toString()
                if (question.realAnswer == null) {
                    val realAnswer = RealAnswer(realAnswerContent)
                    question.realAnswer = realAnswer
                    optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_select_correct_color))
                } else {
                    val currentAnswer = question.realAnswer!!.answer
                    if (currentAnswer.contains(realAnswerContent)) {
                        question.realAnswer!!.answer = currentAnswer.replace(realAnswerContent, "")
                        optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_default_color))
                    } else {
                        question.realAnswer!!.answer += realAnswerContent
                        optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_select_correct_color))
                    }
                }
            }
            optionCardView.setOnLongClickListener {
                EditQuestionContentAction(itemView.context, optionItemWithElement.element).start()
                return@setOnLongClickListener true
            }
        }
    }

    override fun setInitStatus(question: QuestionWithElement) {
        super.setInitStatus(question)
        question.realAnswer = null
    }

    override fun genAnsweredOptionsView(question: QuestionWithElement) {
        val selectedOptions = question.realAnswer?.answer?.toCharArray()
        if (selectedOptions == null || selectedOptions?.size == 0) {
            return
        }
        val answer = getAnswer(question)
        val context = itemView.context
        if (answer.isNullOrEmpty()) {
            Toast.makeText(
                context,
                context.getString(R.string.toast_please_check_answer_content),
                Toast.LENGTH_SHORT).show()
            return
        }
        val answerChar = answer.toCharArray()
        question.options.forEachIndexed { index, optionItemWithElement ->
            val context = itemView.context
            val optionTag = (index + 'A'.toInt()).toChar()
            if (answerChar.contains(optionTag) && selectedOptions.contains(optionTag)) {
                optionContainer.getChildAt(index)
                    .setBackgroundColor(context.getColor(R.color.option_select_correct_color))
            } else if (answerChar.contains(optionTag) && !selectedOptions.contains(optionTag)) {
                optionContainer.getChildAt(index)
                    .setBackgroundColor(context.getColor(R.color.option_select_missed_color))
            } else if (!answerChar.contains(optionTag) && selectedOptions.contains(optionTag)) {
                optionContainer.getChildAt(index)
                    .setBackgroundColor(context.getColor(R.color.option_select_incorrect_color))
            } else {
                optionContainer.getChildAt(index)
                    .setBackgroundColor(context.getColor(R.color.option_default_color))
            }

        }
    }

    override fun genRememberOptionsView(question: QuestionWithElement) {
        optionContainer.removeAllViews()
        var correctAnswer = ""
        if ((question.answer?.element?.size ?: 0) > 0) {
            correctAnswer = question.answer?.element?.get(0)?.content ?: ""
        }
        question.options.forEachIndexed { index, optionItemWithElement ->
            val optionTag = (index + 'A'.toInt()).toChar()
            val optionCardView = OptionCardView(itemView.context)
            if (correctAnswer.contains(optionTag)) {
                optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_select_correct_color))
            } else {
                optionCardView.setBackgroundColor(itemView.context.getColor(R.color.option_default_color))
            }
            optionCardView.setContent(optionItemWithElement.element, optionTag.toString(), ElementViewListener())
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.topMargin = ScreenUtils.dp2px(8f)
            optionContainer.addView(optionCardView, layoutParams)
            optionCardView.setOnLongClickListener {
                EditQuestionContentAction(itemView.context, optionItemWithElement.element).start()
                return@setOnLongClickListener true
            }
        }
    }

    override fun genActionView(question: QuestionWithElement) {
        val action = itemView.findViewById<View>(R.id.action)
        action.visibility = View.VISIBLE
        val context = itemView.context
        action.setOnClickListener {
            if (question.realAnswer == null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_please_select_option),
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            action.visibility = View.GONE
            setAnsweredStatus(question)
            val isCorrect = isCorrect(question)
            LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                .post(EventKey.AnswerEventModel(
                    question.id,
                    question.realAnswer?.answer ?: "",
                    StudyRecord.parseFromBoolean(isCorrect)))
        }
    }

    private fun isCorrect(question: QuestionWithElement): Boolean {
        if (question.answer == null || question.answer.element == null || question.answer.element.size == 0) {
            return false
        }
        var standardAnswer = question.answer.element[0].content
        var realAnswer = question.realAnswer?.answer ?: ""
        standardAnswer = standardAnswer.toCharArray().sortedBy { it.toInt() }.toString()
        realAnswer = realAnswer.toCharArray().sortedBy { it.toInt() }.toString()
        return TextUtils.equals(standardAnswer, realAnswer)
    }

    override fun setAnsweredStatus(question: QuestionWithElement) {
        super.setAnsweredStatus(question)
        optionContainer.children.forEach {
            it.setOnClickListener(null)
        }
    }

}