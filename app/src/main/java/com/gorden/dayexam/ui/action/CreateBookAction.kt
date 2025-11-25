package com.gorden.dayexam.ui.action

import android.content.Context
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.ui.dialog.EditTextDialog

class CreateBookAction(val context: Context, val curCourseId: Int): Action {
    override fun start() {
        EditTextDialog(context,
            context.resources.getString(R.string.dialog_create_book_title),
            context.resources.getString(R.string.dialog_create_book_subTitle),
            "",
            context.resources.getString(R.string.dialog_create_book_hint),
            editCallBack = object : EditTextDialog.EditCallBack {
                override fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String) {
                    if (curCourseId != 0) {
                        DataRepository.insertBook(content, curCourseId)
                        DataRepository.increaseContentVersion()
                    }
                }
            }).show()
    }
}