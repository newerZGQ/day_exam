package com.gorden.dayexam.ui.home.viewholder

import android.view.View
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.repository.model.QuestionDetail

class ErrorTypeQuestionViewHolder(itemView: View): BaseQuestionViewHolder(itemView) {

    override fun setContent(
        paperInfo: PaperInfo,
        question: QuestionDetail,
        isRememberMode: Boolean
    ) {

    }

}