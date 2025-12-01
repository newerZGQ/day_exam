package com.gorden.dayexam.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.model.Element

class OptionCardView: FrameLayout {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        LayoutInflater.from(context).inflate(R.layout.option_card_view_layout, this)
    }

    fun setContent(elements: List<Element>, optionTag: String, listener: ElementActionListener) {
        val elementView = findViewById<ElementsView>(R.id.option_content)
        val optionTagView = findViewById<TextView>(R.id.option_tag)
        optionTagView.text = optionTag
        elementView.textSize = 16f
        elementView.setElements(elements, "", listener)
    }
}