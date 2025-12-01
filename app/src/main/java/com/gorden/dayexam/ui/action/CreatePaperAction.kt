package com.gorden.dayexam.ui.action

import android.content.Context
import android.widget.Toast
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.ui.dialog.EditTextDialog

class CreatePaperAction(val context: Context, val bookId: Int): Action {
    override fun start() {
        EditTextDialog(context,
            context.resources.getString(R.string.dialog_create_paper_title),
            context.resources.getString(R.string.dialog_create_paper_subTitle),
            "",
            context.resources.getString(R.string.dialog_create_paper_hint),
            editCallBack = object : EditTextDialog.EditCallBack {
                override fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String) {
                    if (content.isNotEmpty()) {
                        DataRepository.insertPaper(content, "", "", 0)
                    } else {
                        val msg = context.resources.getString(R.string.create_paper_empty_title_msg)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }).show()
    }
}