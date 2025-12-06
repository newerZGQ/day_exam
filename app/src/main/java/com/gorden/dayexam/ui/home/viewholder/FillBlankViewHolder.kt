package com.gorden.dayexam.ui.home.viewholder

import android.view.View
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.db.entity.StudyRecord
import com.gorden.dayexam.repository.model.Answer
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus

class FillBlankViewHolder(itemView: View): BaseQuestionViewHolder(itemView) {

    override fun genOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {

    }

    override fun genAnsweredOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {

    }

    override fun genRememberOptionsView(paperInfo: PaperInfo, question: QuestionDetail) {
        itemView.findViewById<View>(R.id.action).visibility = View.GONE
    }

    override fun genActionView(paperInfo: PaperInfo, question: QuestionDetail) {
        val action = itemView.findViewById<View>(R.id.action)
        action.visibility = View.VISIBLE
        action.setOnClickListener {
            action.visibility = View.GONE
            showAnswer(paperInfo, question)
            question.realAnswer = Answer()
            LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                .post(EventKey.AnswerEventModel(StudyRecord.NOT_AVAILABLE))
        }
    }

}