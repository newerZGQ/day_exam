package com.gorden.dayexam.ui.paper

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo

class PaperListAdapter(
    private val listener: Listener
) : RecyclerView.Adapter<PaperViewHolder>() {

    interface Listener {
        fun onItemLongPressed(holder: PaperViewHolder, paperInfo: PaperInfo)
        fun onItemDeleteClicked(paperInfo: PaperInfo)
    }

    private val papers = mutableListOf<PaperInfo>()
    private var curPaperId = 0
    private var isEditMode: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaperViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.paper_item, parent, false)
        return PaperViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PaperViewHolder, position: Int) {
        holder.setData(
            paperInfo = papers[position],
            curPaperId = curPaperId,
            isEditMode = isEditMode,
            onLongPress = { vh, paper -> listener.onItemLongPressed(vh, paper) },
            onDeleteClick = { paper -> listener.onItemDeleteClicked(paper) }
        )
    }

    override fun getItemCount(): Int {
        return papers.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newPapers: List<PaperInfo>, curPaperId: Int) {
        papers.clear()
        papers.addAll(newPapers)
        this.curPaperId = curPaperId
        notifyDataSetChanged()
    }

    fun setEditMode(enabled: Boolean) {
        if (isEditMode != enabled) {
            isEditMode = enabled
            notifyDataSetChanged()
        }
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return
        val fromItem = papers.removeAt(fromPosition)
        papers.add(toPosition, fromItem)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getPapers(): List<PaperInfo> = papers
}