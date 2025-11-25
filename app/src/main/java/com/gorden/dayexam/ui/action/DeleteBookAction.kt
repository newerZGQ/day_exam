package com.gorden.dayexam.ui.action

import android.content.Context
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.Book
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.ui.dialog.EditTextDialog

class DeleteBookAction(val context: Context, val book: Book): Action {
    override fun start() {
        EditTextDialog(context,
            context.resources.getString(R.string.dialog_delete_book_title),
            "确定要删除" + book.title + "么?(可从废纸篓找回)",
            editCallBack = object : EditTextDialog.EditCallBack {
                override fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String) {
                    DataRepository.deleteBook(book)
                    DataRepository.increaseContentVersion()
                }
            }).show()
    }
}