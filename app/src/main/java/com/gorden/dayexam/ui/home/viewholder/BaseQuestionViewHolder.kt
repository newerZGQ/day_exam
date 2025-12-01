package com.gorden.dayexam.ui.home.viewholder

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.model.Element
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.widget.AnswerCardView
import com.gorden.dayexam.ui.widget.ElementViewListener
import com.gorden.dayexam.ui.widget.ElementsView
import com.gorden.dayexam.utils.BookUtils
import com.jeremyliao.liveeventbus.LiveEventBus

abstract class BaseQuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val questionInfo = itemView.findViewById<View>(R.id.question_info)

    init {
        questionInfo?.setOnClickListener {
            DataRepository.updateFocusMode(false)
        }
    }

    open fun setData(question: QuestionDetail, bookTitle: String, paperTitle: String, isRememberMode: Boolean) {
        genHeadView(question, bookTitle, paperTitle)
        if (isRememberMode) {
            setRememberStatus(question)
            return
        }
        if (question.realAnswer != null) {
            setAnsweredStatus(question)
        } else {
            setInitStatus(question)
        }

        ContextHolder.currentActivity()?.let {
            LiveEventBus.get(EventKey.REFRESH_QUESTION, Int::class.java)
                .observe(it as LifecycleOwner) {
                    if (isRememberMode) {
                        return@observe
                    }
                    setInitStatus(question)
                }
        }
    }

    @SuppressLint("SetTextI18n")
    open fun genHeadView(question: QuestionDetail, bookTitle: String, paperTitle: String) {
        val headView = itemView.findViewById<TextView>(R.id.question_info)
        headView.text = "$bookTitle-$paperTitle" + " 第" + (adapterPosition + 1) + "题"
    }

    open fun setInitStatus(question: QuestionDetail) {
        genOptionsView(question)
        genActionView(question)
        hideAnswer(question)
    }

    open fun setAnsweredStatus(question: QuestionDetail) {
        genBodyView(question)
        genAnsweredOptionsView(question)
        genAnswerView(question)
        showAnswer(question)
    }

    open fun setRememberStatus(question: QuestionDetail) {
        genBodyView(question)
        genRememberOptionsView(question)
        showAnswer(question)
    }
    @SuppressLint("NewApi")
    open fun genBodyView(question: QuestionDetail) {
        val body = itemView.findViewById<ElementsView>(R.id.body)
        body.setElements(question.body, BookUtils.getTypeName(question.type), ElementViewListener())
    }

    abstract fun genOptionsView(question: QuestionDetail)

    abstract fun genAnsweredOptionsView(question: QuestionDetail)

    abstract fun genRememberOptionsView(question: QuestionDetail)

    open fun genAnswerView(question: QuestionDetail) {
        val answerCardView = itemView.findViewById<AnswerCardView>(R.id.answer)
        answerCardView.setElements(question.answer,
            "",
            question.realAnswer,
            ElementViewListener())
    }

    abstract fun genActionView(question: QuestionDetail)

    open fun showAnswer(question: QuestionDetail) {
        genAnswerView(question)
        itemView.findViewById<View>(R.id.answer_container).visibility = View.VISIBLE
    }

    open fun hideAnswer(question: QuestionDetail) {
        itemView.findViewById<View>(R.id.answer_container).visibility = View.GONE
    }

    fun getAnswer(question: QuestionDetail): String {
        if (question.answer.isNotEmpty() && question.answer[0].elementType == Element.TEXT) {
            return question.answer[0].content
        }
        return ""
    }

}