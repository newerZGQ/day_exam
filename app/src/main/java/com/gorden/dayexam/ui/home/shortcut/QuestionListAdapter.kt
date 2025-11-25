package com.gorden.dayexam.ui.home.shortcut

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.model.QuestionWithElement

class QuestionListAdapter: RecyclerView.Adapter<SimpleQuestionViewHolder>() {

    private var data: List<QuestionWithElement> = listOf()
    private var target = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleQuestionViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.question_select_item,
                parent,
                false)
        return SimpleQuestionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SimpleQuestionViewHolder, position: Int) {
        val question = data[position] ?: return
        holder.setData(question, this.target)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(questionList: List<QuestionWithElement>, target: Int) {
        this.data = questionList
        this.target = target
        notifyDataSetChanged()
    }
}