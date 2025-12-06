package com.gorden.dayexam.ui.home.viewholder

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.widget.ElementViewListener
import com.gorden.dayexam.ui.widget.ElementsView
import com.gorden.dayexam.utils.NameUtils
import com.gorden.dayexam.utils.ScreenUtils
import com.jeremyliao.liveeventbus.LiveEventBus

@SuppressLint("ClickableViewAccessibility")
abstract class BaseQuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var downX = 0f
    private var paperInfo: PaperInfo? = null
    private var question: QuestionDetail? = null
    private var isRememberMode = false

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

    fun setData(paperInfo: PaperInfo, question: QuestionDetail, isRememberMode: Boolean) {
        this.paperInfo = paperInfo
        this.question = question
        this.isRememberMode = isRememberMode
        setHeadView(paperInfo, question)
        setBodyView(paperInfo, question)
        setContent(paperInfo, question, isRememberMode)
    }

    abstract fun setContent(paperInfo: PaperInfo, question: QuestionDetail, isRememberMode: Boolean)

    open fun setHeadView(paperInfo: PaperInfo, question: QuestionDetail) {
        val headView = itemView.findViewById<TextView>(R.id.question_info)
        headView.text = itemView.context.getString(R.string.question_number_format, adapterPosition + 1)
    }


    private fun setBodyView(paperInfo: PaperInfo, question: QuestionDetail) {
        val body = itemView.findViewById<ElementsView>(R.id.body)
        body.setElements(paperInfo, question.body, NameUtils.getTypeName(question.type), ElementViewListener())
    }

    open fun resetAllStatus() {
        this.paperInfo?.let { paper ->
            this.question?.let { question ->
                setContent(paper, question, this.isRememberMode)
            }
        }
    }

}