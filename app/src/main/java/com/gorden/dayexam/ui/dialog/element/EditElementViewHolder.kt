package com.gorden.dayexam.ui.dialog.element

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R

class EditElementViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    val editCard: ElementEditCard = itemView.findViewById(R.id.edit_card)
}