package com.gorden.dayexam.ui.home.viewholder

import android.view.View
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.StudyRecord
import com.gorden.dayexam.repository.model.QuestionWithElement
import com.gorden.dayexam.repository.model.RealAnswer
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus

class EssayQuestionViewHolder(itemView: View): BaseQuestionViewHolder(itemView) {

    override fun genOptionsView(question: QuestionWithElement) {

    }

    override fun genAnsweredOptionsView(question: QuestionWithElement) {

    }

    override fun genRememberOptionsView(question: QuestionWithElement) {

    }

    override fun genActionView(question: QuestionWithElement) {
        val action = itemView.findViewById<View>(R.id.action)
        action.visibility = View.VISIBLE
        action.setOnClickListener {
            action.visibility = View.GONE
            question.realAnswer = RealAnswer()
            showAnswer(question)
            LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                .post(EventKey.AnswerEventModel(question.id, "", StudyRecord.NOT_AVAILABLE))
        }
    }

}