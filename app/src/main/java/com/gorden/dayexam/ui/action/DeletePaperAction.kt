package com.gorden.dayexam.ui.action

import android.content.Context
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.Paper
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.IsInRecycleBinCallback
import com.gorden.dayexam.ui.dialog.EditTextDialog

class DeletePaperAction(val context: Context, val paper: Paper): Action {
    override fun start() {
        DataRepository.isInRecycleBin(paper, object : IsInRecycleBinCallback {
            override fun onFinished(isInRecycleBin: Boolean) {
                if (isInRecycleBin) {
                    performDeleteRecyclePaper(context, paper)
                } else {
                    performDeleteCommonPaper(context, paper)
                }
            }
        })
    }

    private fun performDeleteCommonPaper(context: Context, paper: Paper) {
        EditTextDialog(context,
            context.resources.getString(R.string.dialog_delete_paper_title),
            "确定要删除" + paper.title + "么?(可从废纸篓找回)",
            editCallBack = object : EditTextDialog.EditCallBack {
                override fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String) {
                    DataRepository.deletePaper(paper, false)
                    DataRepository.increaseContentVersion()
                }
            }).show()
    }

    private fun performDeleteRecyclePaper(context: Context, paper: Paper) {
        EditTextDialog(context,
            context.resources.getString(R.string.dialog_delete_paper_title),
            "确定要彻底删除" + paper.title + "么?(无法找回)",
            editCallBack = object : EditTextDialog.EditCallBack {
                override fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String) {
                    DataRepository.deletePaper(paper, true)
                    DataRepository.increaseContentVersion()
                }
            }).show()
    }

}