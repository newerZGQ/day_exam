package com.gorden.dayexam.ui.home.viewholder

import android.view.View
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.db.entity.StudyRecord
import com.gorden.dayexam.repository.model.Answer
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.widget.AnswerCardView
import com.gorden.dayexam.utils.showOrGone
import com.jeremyliao.liveeventbus.LiveEventBus

class FillBlankViewHolder(itemView: View): BaseQuestionViewHolder(itemView) {
    override fun setContent(
        paperInfo: PaperInfo,
        question: QuestionDetail,
        isRememberMode: Boolean
    ) {
        if (isRememberMode) {
            question.realAnswer = null
        }
        setActionView(paperInfo, question, isRememberMode)
        setAnswerView(paperInfo, question, isRememberMode)
    }

    private fun setActionView(
        paperInfo: PaperInfo,
        question: QuestionDetail,
        isRememberMode: Boolean
    ) {
        if (isRememberMode) {
            itemView.findViewById<View>(R.id.action).showOrGone(false)
            return
        }
        if (question.realAnswer != null) {
            itemView.findViewById<View>(R.id.action).showOrGone(false)
        } else {
            itemView.findViewById<View>(R.id.action).apply {
                this.showOrGone(true)
                this.setOnClickListener {
                    question.realAnswer = Answer()
                    resetAllStatus()
                    LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
                        .post(EventKey.AnswerEventModel(StudyRecord.NOT_AVAILABLE))
                }
            }
        }
    }

    private fun setAnswerView(paperInfo: PaperInfo, question: QuestionDetail, isRememberMode: Boolean) {
        if (isRememberMode) {
            itemView.findViewById<View>(R.id.answer_container).showOrGone(true)
            genAnswerView(paperInfo, question)
            return
        }
        if (question.realAnswer != null) {
            itemView.findViewById<View>(R.id.answer_container).showOrGone(true)
            genAnswerView(paperInfo, question)
        } else {
            itemView.findViewById<View>(R.id.answer_container).showOrGone(false)
        }
    }

    private fun genAnswerView(paperInfo: PaperInfo, question: QuestionDetail) {
        itemView.findViewById<AnswerCardView>(R.id.answer).setElements(
            paperInfo = paperInfo,
            elements = question.answer.commonAnswer,
            highlightText = "",
            realAnswer = "",
            listener = { target, elements ->

            }
        )
    }

}