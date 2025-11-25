package com.gorden.dayexam.ui.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.gorden.dayexam.R

@SuppressLint("CutPasteId")
class EditTextDialog(context: Context
): AlertDialog(context) {

    private var title: String? = null
    private var subTitle: String? = null
    private var content: String? = null
    private var contentInputType: Int = -1
    private var hint: String? = null
    private var subContent: String? = null
    private var subHint: String? = null
    private var showEditText: Boolean = false
    private var showSubEditText: Boolean = false
    private var editCallBack: EditCallBack? = null

    constructor(context: Context,
                title: String,
                content: String,
                contentInputType: Int,
                editCallBack: EditCallBack): this(context) {
        this.title = title
        this.subTitle = ""
        this.content = content
        this.contentInputType = contentInputType
        this.hint = ""
        this.subContent = ""
        this.subHint = ""
        this.showEditText = true
        this.showSubEditText = false
        this.editCallBack = editCallBack
        init()
    }

    constructor(context: Context,
                title: String,
                subTitle: String,
                editCallBack: EditCallBack): this(context) {
        this.title = title
        this.subTitle = subTitle
        this.content = ""
        this.hint = ""
        this.subContent = ""
        this.subHint = ""
        this.showEditText = false
        this.showSubEditText = false
        this.editCallBack = editCallBack
        init()
    }

    constructor(context: Context,
                title: String,
                subTitle: String,
                content: String,
                hint: String,
                editCallBack: EditCallBack): this(context) {
        this.title = title
        this.subTitle = subTitle
        this.content = content
        this.hint = hint
        this.subContent = ""
        this.subHint = ""
        this.showEditText = true
        this.showSubEditText = false
        this.editCallBack = editCallBack
        init()
                }

    constructor(context: Context,
                title: String,
                subTitle: String,
                content: String,
                hint: String,
                subContent: String,
                subHint: String,
                editCallBack: EditCallBack): this(context) {
        this.title = title
        this.subTitle = subTitle
        this.content = content
        this.hint = hint
        this.subContent = subContent
        this.subHint = subHint
        this.showEditText = true
        this.showSubEditText = true
        this.editCallBack = editCallBack
        init()
                }

    private fun init() {
        val customView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_text_layout, null)
        setView(customView)
        customView.findViewById<TextView>(R.id.dialog_title).text = title
        customView.findViewById<TextView>(R.id.dialog_sub_title).text = subTitle
        customView.findViewById<EditText>(R.id.dialog_content).setText(content)
        customView.findViewById<EditText>(R.id.dialog_sub_content).setText(subContent)
        if (content?.isEmpty() == true && hint?.isNotEmpty() == true) {
            customView.findViewById<EditText>(R.id.dialog_content).hint = hint
        }
        if (subContent?.isEmpty() == true && subHint?.isNotEmpty() == true) {
            customView.findViewById<EditText>(R.id.dialog_sub_content).hint = subHint
        }
        if (showEditText) {
            customView.findViewById<EditText>(R.id.dialog_content).visibility = View.VISIBLE
            if (contentInputType > 0) {
                customView.findViewById<EditText>(R.id.dialog_content).inputType = this.contentInputType
            }
        } else {
            customView.findViewById<EditText>(R.id.dialog_content).visibility = View.GONE
        }
        if (showSubEditText) {
            customView.findViewById<EditText>(R.id.dialog_sub_content).visibility = View.VISIBLE
        } else {
            customView.findViewById<EditText>(R.id.dialog_sub_content).visibility = View.GONE
        }

        setButton(DialogInterface.BUTTON_NEGATIVE, context.resources.getString(R.string.dialog_cancel))
        { _, _ -> }

        setButton(DialogInterface.BUTTON_POSITIVE, context.resources.getString(R.string.dialog_confirm))
        { _, _ ->
            val content = customView.findViewById<EditText>(R.id.dialog_content).text
            val subContent = customView.findViewById<EditText>(R.id.dialog_sub_content).text
            editCallBack?.onConfirmContent(this, content.toString(), subContent.toString())
        }
    }

    interface EditCallBack {
        fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String)
    }

}