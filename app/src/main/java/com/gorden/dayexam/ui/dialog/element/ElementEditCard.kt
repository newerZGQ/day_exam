package com.gorden.dayexam.ui.dialog.element

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageButton
import com.gorden.dayexam.R

abstract class ElementEditCard: FrameLayout {

    private lateinit var addTextBtn: ImageButton
    private lateinit var addImageBtn: ImageButton
    open lateinit var adapter: ElementAdapter

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    abstract fun setElement(editableElement: EditableElement, adapter: ElementAdapter, listener: EditActionListener)

    open fun setAction() {
        addTextBtn = findViewById(R.id.add_text_element_btn)
        addTextBtn.setOnClickListener {
            adapter.insertTextElementAfterCurrentPosition()
        }
        addImageBtn = findViewById(R.id.add_image_element_btn)
        addImageBtn.setOnClickListener {
            adapter.insertImageElementAfterCurrentPosition()
        }
    }

    abstract fun hideAction()

    abstract fun showAction()

    abstract fun toResetMode()

    abstract fun toEditMode()

    abstract fun toDeleteMode()


}