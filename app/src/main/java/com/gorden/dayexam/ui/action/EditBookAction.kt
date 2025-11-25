package com.gorden.dayexam.ui.action

import android.content.Context
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.Book
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.ui.dialog.EditTextDialog

class EditBookAction(val context: Context, val book: Book): Action {
    override fun start() {
        EditTextDialog(context,
            context.resources.getString(R.string.dialog_edit_book_title),
            context.resources.getString(R.string.dialog_edit_book_subTitle),
            book.title,
            "",
            editCallBack = object : EditTextDialog.EditCallBack {
                override fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String) {
                    if (content.isNotEmpty()) {
                        book.title = content
                        DataRepository.updateBook(book)
                        DataRepository.increaseContentVersion()
                    }
                }
            }).show()
    }
}