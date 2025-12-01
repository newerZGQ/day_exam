package com.gorden.dayexam.ui.home.shortcut

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.model.Element
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.utils.BookUtils
import com.jeremyliao.liveeventbus.LiveEventBus

class SimpleQuestionViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    private val contentView = itemView.findViewById<TextView>(R.id.question_content)
    private val tagView = itemView.findViewById<TextView>(R.id.question_tag)

    companion object {
        const val SELECT_POSITION = "select_position"
    }

    fun setData(question: QuestionDetail, target: Int) {
        val content = getDescription(question)
        contentView.text = content
        val tag = getTag(question)
        tagView.text = tag
        if (adapterPosition == target) {
            itemView.setBackgroundColor(itemView.context.resources.getColor(R.color.colorPrimaryDark))
        } else {
            itemView.setBackgroundColor(itemView.context.resources.getColor(R.color.colorPrimary))
        }
        itemView.setOnClickListener {
            LiveEventBus.get(EventKey.SELECT_QUESTION, Int::class.java)
                .post(adapterPosition)
        }
    }

    private fun getDescription(question: QuestionDetail): String {
        val textList = question.body.filter {
            it.elementType == Element.TEXT
        }
        var desc = ""
        var index = 0
        while (desc.length < 48 && index < textList.size) {
            desc += textList[index].content
            index++
        }
        return desc
    }

    private fun getTag(question: QuestionDetail): String {
        val positionTag = itemView.context.getString(R.string.the_type_key) +
                (adapterPosition + 1) + itemView.context.getString(R.string.short_of_question)
        val typeTag = BookUtils.getTypeName(question.type)
        return "$positionTag $typeTag"
    }
}