package com.gorden.dayexam.ui.sheet.search

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R

class SearchItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    val courseTitle = itemView.findViewById<TextView>(R.id.course_title)!!
    val bookTitle = itemView.findViewById<TextView>(R.id.book_title)!!
    val paperTitle = itemView.findViewById<TextView>(R.id.paper_title)!!
    val questionType = itemView.findViewById<TextView>(R.id.question_type)!!
    val elementType = itemView.findViewById<TextView>(R.id.element_type)!!
    val elementContent = itemView.findViewById<TextView>(R.id.element_content)!!
}