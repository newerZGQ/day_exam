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
import android.view.MotionEvent
import com.gorden.dayexam.utils.NameUtils
import com.gorden.dayexam.utils.ScreenUtils
import com.jeremyliao.liveeventbus.LiveEventBus

@SuppressLint("ClickableViewAccessibility")
abstract class BaseQuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var downX = 0f

    init {
        // 使用 OnTouchListener 记录 down 事件的坐标
        itemView.findViewById<View>(R.id.question_container).setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                downX = event.rawX
            }
            false // 返回 false 让事件继续传递给 OnClickListener
        }
        
        // 使用 itemView 的点击事件处理翻页
        itemView.findViewById<View>(R.id.question_container).setOnClickListener {
            val screenWidth = ScreenUtils.screenWidth()
            
            if (downX < screenWidth / 2) {
                LiveEventBus.get(EventKey.NAVIGATE_QUESTION, Int::class.java).post(-1)
            } else {
                LiveEventBus.get(EventKey.NAVIGATE_QUESTION, Int::class.java).post(1)
            }
        }
    }

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
        answerCardView.setElements(paperInfo, question.answer.commonAnswer,
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

}