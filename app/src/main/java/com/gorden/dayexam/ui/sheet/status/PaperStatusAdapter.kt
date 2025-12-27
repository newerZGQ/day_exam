package com.gorden.dayexam.ui.sheet.status

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.model.QuestionDetail

class PaperStatusAdapter(
    private val questions: List<QuestionDetail>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<PaperStatusAdapter.ViewHolder>() {

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.tv_status_index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_paper_status_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val question = questions[position]
        holder.textView.text = "${position + 1}"

        val colorResId = when {
            question.realAnswer == null -> R.color.status_unanswered
            isCorrect(question) -> R.color.status_correct
            else -> R.color.status_incorrect
        }

        holder.textView.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(holder.textView.context, colorResId)
        )

        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int = questions.size

    private fun isCorrect(question: QuestionDetail): Boolean {
        val real = question.realAnswer ?: return false
        val correct = question.answer
        
        // Compare based on question type logic usually found in ViewHolders
        // For simplicity, we check if optionAnswer or tfAnswer matches
        if (question.type == 2) { // True/False
             return real.tfAnswer == correct.tfAnswer
        }
        if (question.type == 3 || question.type == 4) { // Single/Multiple Choice
            // Simple comparison of lists
            return real.optionAnswer.sorted() == correct.optionAnswer.sorted()
        }
        // For other types (Fill blank, Essay) usually manual judgment or specific logic
        // Assuming if realAnswer is present it might be correct if it matches commonAnswer?
        // But for FillBlank/Essay, auto-grading might be hard. 
        // Based on user request: "Green correct, Red incorrect, Grey unanswered". 
        // If the app doesn't auto-grade fill-blank, we might treat it as correct or specific state.
        // Let's assume strict comparison for now or just check if realAnswer exists for non-auto-graded.
        return real.optionAnswer == correct.optionAnswer && real.tfAnswer == correct.tfAnswer
    }
}
