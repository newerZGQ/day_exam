package com.gorden.dayexam.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.repository.model.Element

class AnswerCardView: FrameLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        LayoutInflater.from(context).inflate(R.layout.answer_card_view_layout, this)
    }

    @SuppressLint("CutPasteId")
    fun setElements(
        paperInfo: PaperInfo,
        elements: List<Element>,
        highlightText: String,
        realAnswer: String?,
        listener: ElementActionListener
    ) {
        findViewById<ElementsView>(R.id.answer_content).setElements(paperInfo, elements, highlightText, listener)
        if (realAnswer?.isNotEmpty() == true) {
            findViewById<TextView>(R.id.real_answer_content).visibility = VISIBLE
            findViewById<TextView>(R.id.real_answer_content).text = realAnswer
            findViewById<TextView>(R.id.real_answer_tag).visibility = VISIBLE
        } else {
            findViewById<TextView>(R.id.real_answer_content).visibility = GONE
            findViewById<TextView>(R.id.real_answer_content).text = ""
            findViewById<TextView>(R.id.real_answer_tag).visibility = GONE
        }
    }
}