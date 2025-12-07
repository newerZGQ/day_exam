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
import com.gorden.dayexam.databinding.DialogEditTextLayoutBinding

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

    private val binding: DialogEditTextLayoutBinding =
        DialogEditTextLayoutBinding.inflate(LayoutInflater.from(context))

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
        setView(binding.root)
        binding.dialogTitle.text = title
        binding.dialogSubTitle.text = subTitle
        binding.dialogContent.setText(content)
        binding.dialogSubContent.setText(subContent)
        if (content?.isEmpty() == true && hint?.isNotEmpty() == true) {
            binding.dialogContent.hint = hint
        }
        if (subContent?.isEmpty() == true && subHint?.isNotEmpty() == true) {
            binding.dialogSubContent.hint = subHint
        }
        if (showEditText) {
            binding.dialogContent.visibility = View.VISIBLE
            if (contentInputType > 0) {
                binding.dialogContent.inputType = this.contentInputType
            }
        } else {
            binding.dialogContent.visibility = View.GONE
        }
        if (showSubEditText) {
            binding.dialogSubContent.visibility = View.VISIBLE
        } else {
            binding.dialogSubContent.visibility = View.GONE
        }

        setButton(DialogInterface.BUTTON_NEGATIVE, context.resources.getString(R.string.dialog_cancel))
        { _, _ -> }

        setButton(DialogInterface.BUTTON_POSITIVE, context.resources.getString(R.string.dialog_confirm))
        { _, _ ->
            val content = binding.dialogContent.text
            val subContent = binding.dialogSubContent.text
            editCallBack?.onConfirmContent(this, content.toString(), subContent.toString())
        }
    }

    interface EditCallBack {
        fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String)
    }

}