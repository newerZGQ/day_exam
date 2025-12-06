package com.gorden.dayexam.ui.home.viewholder

import android.text.TextUtils
import android.util.Log
import android.view.View
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.db.entity.StudyRecord
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.repository.model.RealAnswer
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus

class TrueFalseViewHolder(itemView: View): BaseQuestionViewHolder(itemView) {

    private val correctOption: View = itemView.findViewById(R.id.action_correct)
    private val inCorrectOption: View = itemView.findViewById(R.id.action_incorrect)

    override fun genOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        val resources = itemView.resources
        correctOption.setBackgroundColor(resources.getColor(R.color.option_default_color))
        inCorrectOption.setBackgroundColor(resources.getColor(R.color.option_default_color))
        correctOption.setOnClickListener {
            val realAnswerContent = resources.getString(R.string.correct)
            question.realAnswer = RealAnswer(realAnswerContent)
            setAnsweredStatus(paperInfo, question)
            val answerTag = getAnswerEventTag(question)
            LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                .post(EventKey.AnswerEventModel(realAnswerContent, answerTag))
        }
        inCorrectOption.setOnClickListener {
            val realAnswerContent = resources.getString(R.string.incorrect)
            question.realAnswer = RealAnswer(realAnswerContent)
            setAnsweredStatus(paperInfo, question)
            val answerTag = getAnswerEventTag(question)
            LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                .post(EventKey.AnswerEventModel(realAnswerContent, answerTag))
        }
    }

    override fun genAnsweredOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        val context = itemView.context
        val answer = question.answer[0].content
        if (question.realAnswer != null) {
            if (question.realAnswer?.answer.equals(answer)){
                if (answer == context.getString(R.string.correct)) {
                    correctOption.setBackgroundColor(context.getColor(R.color.option_select_correct_color))
                    inCorrectOption.setBackgroundColor(context.getColor(R.color.option_default_color))
                } else {
                    correctOption.setBackgroundColor(context.getColor(R.color.option_default_color))
                    inCorrectOption.setBackgroundColor(context.getColor(R.color.option_select_correct_color))
                }
            } else {
                if (answer == context.getString(R.string.correct)) {
                    correctOption.setBackgroundColor(context.getColor(R.color.option_select_correct_color))
                    inCorrectOption.setBackgroundColor(context.getColor(R.color.option_select_incorrect_color))
                } else {
                    correctOption.setBackgroundColor(context.getColor(R.color.option_select_incorrect_color))
                    inCorrectOption.setBackgroundColor(context.getColor(R.color.option_select_correct_color))
                }
            }
        }
        correctOption.setOnClickListener {
            Log.d("", "")
        }
        inCorrectOption.setOnClickListener {
            Log.d("", "")
        }
    }

    override fun genRememberOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        val answer = question.answer[0].content
        val resources = itemView.resources
        if (answer.isEmpty()) {
            return
        }
        if (answer == resources.getString(R.string.correct)) {
            correctOption.setBackgroundColor(resources.getColor(R.color.option_select_correct_color))
            inCorrectOption.setBackgroundColor(resources.getColor(R.color.option_default_color))
        } else {
            correctOption.setBackgroundColor(resources.getColor(R.color.option_default_color))
            inCorrectOption.setBackgroundColor(resources.getColor(R.color.option_select_correct_color))
        }
        correctOption.setOnClickListener(null)
        inCorrectOption.setOnClickListener(null)
    }

    override fun genActionView(paperInfo: PaperInfo, question: QuestionDetail) {

    }

    private fun getAnswerEventTag(question: QuestionDetail): Int {
        val answer = question.answer
        var answerString = ""
        if (answer.isNotEmpty() && answer[0].content.isNotEmpty()) {
            val answerContent = answer[0].content
            if (answerContent.trim() == itemView.context.resources.getString(R.string.correct)) {
                answerString = itemView.context.resources.getString(R.string.correct)
            } else if (answerContent.trim() == itemView.context.resources.getString(R.string.incorrect)) {
                answerString = itemView.context.resources.getString(R.string.incorrect)
            }
        }
        val realAnswerString = question.realAnswer?.answer
        return if (answerString.isNotEmpty() && TextUtils.equals(answerString, realAnswerString)) {
            StudyRecord.CORRECT
        } else if (answerString.isNotEmpty() && !TextUtils.equals(answerString, realAnswerString)) {
            StudyRecord.IN_CORRECT
        } else {
            StudyRecord.NOT_AVAILABLE
        }
    }

}