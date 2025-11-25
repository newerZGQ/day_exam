package com.gorden.dayexam.ui.sheet.course

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R

class CourseViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    val title = itemView.findViewById<TextView>(R.id.courseTitle)!!
    val desc = itemView.findViewById<TextView>(R.id.courseDesc)!!
    val icon = itemView.findViewById<ImageView>(R.id.courseIcon)
}