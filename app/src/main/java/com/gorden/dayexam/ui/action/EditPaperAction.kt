package com.gorden.dayexam.ui.action

import android.content.Context
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.ui.dialog.EditTextDialog

class EditPaperAction(val context: Context, val paperInfo: PaperInfo): Action {
    override fun start() {
        EditTextDialog(context,
            context.resources.getString(R.string.dialog_edit_paper_title),
            context.resources.getString(R.string.dialog_edit_paper_subTitle),
            paperInfo.title,
            context.resources.getString(R.string.dialog_create_book_hint),
            editCallBack = object : EditTextDialog.EditCallBack {
                override fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String) {
                    paperInfo.title = content
                    DataRepository.updatePaper(paperInfo)
                }
            }).show()
    }
}