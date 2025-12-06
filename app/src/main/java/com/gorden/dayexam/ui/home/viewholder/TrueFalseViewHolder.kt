package com.gorden.dayexam.ui.home.viewholder

import android.text.TextUtils
import android.util.Log
import android.view.View
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.db.entity.StudyRecord
import com.gorden.dayexam.repository.model.Answer
import com.gorden.dayexam.repository.model.QuestionDetail
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
            question.realAnswer = Answer(tfAnswer = true)
            setAnsweredStatus(paperInfo, question)
            val answerTag = getAnswerEventTag(question)
            LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                .post(EventKey.AnswerEventModel(answerTag))
        }
        inCorrectOption.setOnClickListener {
            question.realAnswer = Answer(tfAnswer = false)
            setAnsweredStatus(paperInfo, question)
            val answerTag = getAnswerEventTag(question)
            LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                .post(EventKey.AnswerEventModel(answerTag))
        }
    }

    override fun genAnsweredOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        val context = itemView.context
        val answer = question.answer.tfAnswer
        if (question.realAnswer != null) {
            if (question.realAnswer?.tfAnswer == answer){
                if (answer) {
                    correctOption.setBackgroundColor(context.getColor(R.color.option_select_correct_color))
                    inCorrectOption.setBackgroundColor(context.getColor(R.color.option_default_color))
                } else {
                    correctOption.setBackgroundColor(context.getColor(R.color.option_default_color))
                    inCorrectOption.setBackgroundColor(context.getColor(R.color.option_select_correct_color))
                }
            } else {
                if (answer) {
                    correctOption.setBackgroundColor(context.getColor(R.color.option_select_correct_color))
                    inCorrectOption.setBackgroundColor(context.getColor(R.color.option_select_incorrect_color))
                } else {
                    correctOption.setBackgroundColor(context.getColor(R.color.option_select_incorrect_color))
                    inCorrectOption.setBackgroundColor(context.getColor(R.color.option_select_correct_color))
                }
            }
        }
        // 避免点击该view时触发翻页
        correctOption.setOnClickListener {}
        inCorrectOption.setOnClickListener {}
    }

    override fun genRememberOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        val answer = question.answer.tfAnswer
        val resources = itemView.resources
        if (answer) {
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
        val realAnswer = question.realAnswer?.tfAnswer ?: false
        return if (realAnswer) {
            StudyRecord.CORRECT
        } else {
            StudyRecord.IN_CORRECT
        }
    }

}