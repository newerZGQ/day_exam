package com.gorden.dayexam.ui.dialog.element

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.gorden.dayexam.R

class TextElementEditCard: ElementEditCard {

    private var textContent: TextView
    private var editContent: EditText

    private var actionContainer: LinearLayout
    private var subActionContainer: LinearLayout
    private var editBtn: ImageButton
    private var resetBtn: ImageButton
    private var deleteBtn: ImageButton

    private var doneBtn: ImageButton
    private var cancelBtn: ImageButton

    private var listener: EditActionListener? = null

    private lateinit var editableElement: EditableElement

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        LayoutInflater.from(context).inflate(R.layout.text_element_edit_card_layout, this)
        textContent = findViewById(R.id.text_element_content)
        editContent = findViewById(R.id.text_element_edit)

        actionContainer = findViewById(R.id.action_container)
        subActionContainer = findViewById(R.id.sub_action_container)
        editBtn = findViewById(R.id.edit_element_btn)
        resetBtn = findViewById(R.id.reset_element_btn)
        deleteBtn = findViewById(R.id.delete_element_btn)

        doneBtn = findViewById(R.id.done_edit_element_btn)
        cancelBtn = findViewById(R.id.cancel_element_edit)
        setAction()
    }

    @SuppressLint("CutPasteId")
    override fun setElement(editableElement: EditableElement, adapter: ElementAdapter, listener: EditActionListener) {
        this.editableElement = editableElement
        this.listener = listener
        this.adapter = adapter
        editContent.visibility = View.GONE
        showTextContent()
    }

    override fun setAction() {
        super.setAction()
        editBtn.setOnClickListener {
            adapter?.isEditing = true
            adapter.currentItemEditMode()
        }
        resetBtn.setOnClickListener {
            editableElement.hasEdited = false
            editableElement.isDeleted = false
            editableElement.newContent = editableElement.element.content
            adapter.currentItemResetMode()
        }
        deleteBtn.setOnClickListener {
            editableElement.newContent = ""
            editableElement.isDeleted = true
            adapter.currentItemDeleteMode()
        }
        doneBtn.setOnClickListener {
            editableElement.newContent = editContent.text.toString()
            editContent.visibility = GONE
            showTextContent()
            actionContainer.visibility = View.VISIBLE
            subActionContainer.visibility = GONE
            adapter?.isEditing = false
            editableElement.hasEdited = true
        }
        cancelBtn.setOnClickListener {
            editContent.visibility = GONE
            textContent.visibility = VISIBLE
            textContent.text = editableElement.newContent
            editContent.setText(editableElement.newContent)
            actionContainer.visibility = View.VISIBLE
            subActionContainer.visibility = GONE
            adapter?.isEditing = false
        }
    }

    override fun hideAction() {
        actionContainer.visibility = GONE
    }

    override fun showAction() {
        actionContainer.visibility = VISIBLE
    }

    private fun showSoftInput() {
        val imm = (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
        imm.showSoftInput(editContent, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun showTextContent() {
        editContent.visibility = GONE
        textContent.visibility = VISIBLE
        if (editableElement.newContent.isNullOrEmpty()) {
            textContent.text = context.resources.getString(R.string.empty_paragraph)
        } else {
            textContent.text = editableElement.newContent
        }
    }

    private fun showEditContent() {
        textContent.visibility = View.GONE
        editContent.visibility = View.VISIBLE
        if (editableElement.newContent.isNullOrEmpty()) {
            editContent.setText("")
            editContent.hint = context.resources.getString(R.string.please_input)
        } else {
            editContent.setText(editableElement.newContent)
        }
        editContent.postDelayed({
            editContent.focusable = View.FOCUSABLE
            editContent.isFocusableInTouchMode = true
            editContent.requestFocus()
            editContent.setSelection(editContent.text.length);
            showSoftInput()
        }, 100)
    }

    override fun toEditMode() {
        actionContainer.visibility = View.GONE
        subActionContainer.visibility = VISIBLE
        showEditContent()
    }

    override fun toDeleteMode() {
        editBtn.visibility = View.GONE
        deleteBtn.visibility = View.GONE
        showTextContent()
    }

    override fun toResetMode() {
        editBtn.visibility = View.VISIBLE
        deleteBtn.visibility = View.VISIBLE
        showTextContent()
    }
}

interface EditActionListener {
    fun onStartEdit()
}