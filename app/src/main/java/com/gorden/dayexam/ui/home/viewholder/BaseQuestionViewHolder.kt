package com.gorden.dayexam.ui.home.viewholder

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.question.Element
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.QuestionWithElement
import com.gorden.dayexam.repository.model.RealAnswer
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.action.EditQuestionContentAction
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

    open fun setData(question: QuestionWithElement, bookTitle: String, paperTitle: String, isRememberMode: Boolean) {
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
    open fun genHeadView(question: QuestionWithElement, bookTitle: String, paperTitle: String) {
        val headView = itemView.findViewById<TextView>(R.id.question_info)
        headView.text = "$bookTitle-$paperTitle" + " 第" + (adapterPosition + 1) + "题"
    }

    open fun setInitStatus(question: QuestionWithElement) {
        genBodyView(question)
        genOptionsView(question)
        genActionView(question)
        hideAnswer(question)
    }

    open fun setAnsweredStatus(question: QuestionWithElement) {
//        genBodyView(question)
        genAnsweredOptionsView(question)
        genAnswerView(question)
        showAnswer(question)
    }

    open fun setRememberStatus(question: QuestionWithElement) {
        genBodyView(question)
        genRememberOptionsView(question)
        showAnswer(question)
    }

    open fun genBodyView(question: QuestionWithElement) {
        val body = itemView.findViewById<ElementsView>(R.id.body)
        body.setElements(question.body.element, BookUtils.getTypeName(question.type), ElementViewListener())
        body.setOnLongClickListener {
            EditQuestionContentAction(itemView.context, question.body.element).start()
            return@setOnLongClickListener true
        }
    }

    abstract fun genOptionsView(question: QuestionWithElement)

    abstract fun genAnsweredOptionsView(question: QuestionWithElement)

    abstract fun genRememberOptionsView(question: QuestionWithElement)

    open fun genAnswerView(question: QuestionWithElement) {
        val answerCardView = itemView.findViewById<AnswerCardView>(R.id.answer)
        answerCardView.setElements(question.answer.element,
            "",
            question.realAnswer,
            ElementViewListener())
        answerCardView.setOnLongClickListener {
            EditQuestionContentAction(itemView.context, question.answer.element).start()
            return@setOnLongClickListener true
        }
    }

    abstract fun genActionView(question: QuestionWithElement)

    open fun showAnswer(question: QuestionWithElement) {
        genAnswerView(question)
        itemView.findViewById<View>(R.id.answer_container).visibility = View.VISIBLE
    }

    open fun hideAnswer(question: QuestionWithElement) {
        itemView.findViewById<View>(R.id.answer_container).visibility = View.GONE
    }

    fun getAnswer(question: QuestionWithElement): String {
        if (question.answer?.element?.isNotEmpty() == true
            && question.answer!!.element[0].elementType == Element.TEXT) {
            return question.answer.element[0].content
        }
        return ""
    }

}