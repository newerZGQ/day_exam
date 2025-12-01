package com.gorden.dayexam.ui.action

import android.content.Context
import com.gorden.dayexam.repository.model.question.Element
import com.gorden.dayexam.ui.dialog.element.EditElementsDialog

class EditQuestionContentAction(val context: Context, val elements: List<Element>): Action {
    override fun start() {
        if (elements.isNotEmpty()) {
            EditElementsDialog(
                context,
                elements[0].parentId,
                elements
            ).show()
        }
    }
}