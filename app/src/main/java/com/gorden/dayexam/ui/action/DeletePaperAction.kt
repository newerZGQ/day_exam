package com.gorden.dayexam.ui.action

import android.content.Context
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.IsInRecycleBinCallback
import com.gorden.dayexam.ui.dialog.EditTextDialog

class DeletePaperAction(val context: Context, val paperInfo: PaperInfo): Action {
    override fun start() {
        performDeleteCommonPaper(context, paperInfo)
    }

    private fun performDeleteCommonPaper(context: Context, paperInfo: PaperInfo) {
        EditTextDialog(context,
            context.resources.getString(R.string.dialog_delete_paper_title),
            "确定要删除" + paperInfo.title + "么?(可从废纸篓找回)",
            editCallBack = object : EditTextDialog.EditCallBack {
                override fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String) {
                    DataRepository.deletePaper(paperInfo)
                }
            }).show()
    }

}