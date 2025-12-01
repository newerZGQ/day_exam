package com.gorden.dayexam.ui.home.viewholder

import android.view.View
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.StudyRecord
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.repository.model.RealAnswer
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus

class EssayQuestionViewHolder(itemView: View): BaseQuestionViewHolder(itemView) {

    override fun genOptionsView(question: QuestionDetail) {

    }

    override fun genAnsweredOptionsView(question: QuestionDetail) {

    }

    override fun genRememberOptionsView(question: QuestionDetail) {

    }

    override fun genActionView(question: QuestionDetail) {
        val action = itemView.findViewById<View>(R.id.action)
        action.visibility = View.VISIBLE
        action.setOnClickListener {
            action.visibility = View.GONE
            question.realAnswer = RealAnswer()
            showAnswer(question)
            LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                .post(EventKey.AnswerEventModel("", StudyRecord.NOT_AVAILABLE))
        }
    }

}