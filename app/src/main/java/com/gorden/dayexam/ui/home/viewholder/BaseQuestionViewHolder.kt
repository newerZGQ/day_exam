package com.gorden.dayexam.ui.home.viewholder

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.repository.model.Element
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.widget.AnswerCardView
import com.gorden.dayexam.ui.widget.ElementViewListener
import com.gorden.dayexam.ui.widget.ElementsView
import com.gorden.dayexam.utils.NameUtils
import com.jeremyliao.liveeventbus.LiveEventBus

abstract class BaseQuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    open fun setData(paperInfo: PaperInfo, question: QuestionDetail, isRememberMode: Boolean) {
        genHeadView(paperInfo, question)
        if (isRememberMode) {
            setRememberStatus(paperInfo, question)
            return
        }
        if (question.realAnswer != null) {
            setAnsweredStatus(paperInfo, question)
        } else {
            setInitStatus(paperInfo, question)
        }
    }

    @SuppressLint("SetTextI18n")
    open fun genHeadView(paperInfo: PaperInfo, question: QuestionDetail) {
        val headView = itemView.findViewById<TextView>(R.id.question_info)
        headView.text = "第" + (adapterPosition + 1) + "题"
    }

    open fun setInitStatus(paperInfo: PaperInfo, question: QuestionDetail) {
        genBodyView(paperInfo, question)
        genOptionsView(paperInfo, question)
        genActionView(paperInfo, question)
        hideAnswer(paperInfo, question)
    }

    open fun setAnsweredStatus(paperInfo: PaperInfo, question: QuestionDetail) {
        genBodyView(paperInfo, question)
        genAnsweredOptionsView(paperInfo, question)
        genAnswerView(paperInfo, question)
        showAnswer(paperInfo, question)
    }

    open fun setRememberStatus(paperInfo: PaperInfo, question: QuestionDetail) {
        genBodyView(paperInfo, question)
        genRememberOptionsView(paperInfo, question)
        showAnswer(paperInfo, question)
    }
    @SuppressLint("NewApi")
    open fun genBodyView(paperInfo: PaperInfo, question: QuestionDetail) {
        val body = itemView.findViewById<ElementsView>(R.id.body)
        body.setElements(paperInfo, question.body, NameUtils.getTypeName(question.type), ElementViewListener())
    }

    abstract fun genOptionsView(paperInfo: PaperInfo, question: QuestionDetail)

    abstract fun genAnsweredOptionsView(paperInfo: PaperInfo, question: QuestionDetail)

    abstract fun genRememberOptionsView(paperInfo: PaperInfo, question: QuestionDetail)

    open fun genAnswerView(paperInfo: PaperInfo, question: QuestionDetail) {
        val answerCardView = itemView.findViewById<AnswerCardView>(R.id.answer)
        answerCardView.setElements(paperInfo, question.answer,
            "",
            question.realAnswer,
            ElementViewListener())
    }

    abstract fun genActionView(paperInfo: PaperInfo, question: QuestionDetail)

    open fun showAnswer(paperInfo: PaperInfo, question: QuestionDetail) {
        genAnswerView(paperInfo, question)
        itemView.findViewById<View>(R.id.answer_container).visibility = View.VISIBLE
    }

    open fun hideAnswer(paperInfo: PaperInfo, question: QuestionDetail) {
        itemView.findViewById<View>(R.id.answer_container).visibility = View.GONE
    }

    fun getAnswer(paperInfo: PaperInfo, question: QuestionDetail): String {
        if (question.answer.isNotEmpty() && question.answer[0].elementType == Element.TEXT) {
            return question.answer[0].content
        }
        return ""
    }

}