package com.gorden.dayexam.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.repository.model.QuestionType
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.ui.home.viewholder.*

class QuestionPagerAdapter: RecyclerView.Adapter<BaseQuestionViewHolder>() {

    private var data = listOf<QuestionDetail>()
    private var isRememberMode: Boolean = false

    private var paperInfo: PaperInfo? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setData(paperInfo: PaperInfo, data: List<QuestionDetail>) {
        this.data = data
        this.paperInfo = paperInfo
        this.notifyDataSetChanged()
    }

    fun setRememberMode(isRememberMode: Boolean) {
        if (this.isRememberMode == isRememberMode) {
            return
        }
        this.isRememberMode = isRememberMode
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return data[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseQuestionViewHolder {
        when(viewType) {
            QuestionType.FILL_BLANK -> {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.viewpager_fill_blank_question_layout,
                        parent,
                        false)
                return FillBlankViewHolder(itemView)
            }
            QuestionType.TRUE_FALSE -> {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.viewpager_true_false_question_layout,
                        parent,
                        false)
                return TrueFalseViewHolder(itemView)
            }
            QuestionType.SINGLE_CHOICE -> {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.viewpager_single_choice_question_layout,
                        parent,
                        false)
                return SingleChoiceViewHolder(itemView)
            }
            QuestionType.MULTIPLE_CHOICE -> {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.viewpager_multiple_choice_question_layout,
                        parent,
                        false)
                return MultipleChoiceViewHolder(itemView)
            }
            QuestionType.ESSAY_QUESTION -> {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.viewpager_essay_question_layout,
                        parent,
                        false)
                return EssayQuestionViewHolder(itemView)
            }
            else -> {
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.viewpager_error_type_question_layout,
                        parent,
                        false)
                return ErrorTypeQuestionViewHolder(itemView)
            }
        }
    }

    override fun onBindViewHolder(holder: BaseQuestionViewHolder, position: Int) {
        paperInfo?.let {
            holder.setData(it, this.data[position], isRememberMode)
        }
    }

    override fun getItemCount(): Int {
       return data.size
    }
}