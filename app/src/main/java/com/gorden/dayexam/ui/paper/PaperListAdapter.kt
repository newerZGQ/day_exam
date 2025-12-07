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
        fun onItemClicked(paperInfo: PaperInfo)
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
            onClick = { paper -> listener.onItemClicked(paper) },
            onLongPress = { vh, paper -> listener.onItemLongPressed(vh, paper) },
            onDeleteClick = { paper -> listener.onItemDeleteClicked(paper) }
        )
    }

    override fun onBindViewHolder(holder: PaperViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            // If we have payloads, we can optimize updates if needed.
            // For now, full rebind is safe, or we can check specific payloads.
            // Since PaperViewHolder.setData handles everything, we can just call it.
            // But to be truly efficient with payloads, we might want to update specific views.
            // However, given the current ViewHolder implementation, a full rebind is likely fine
            // as long as DiffUtil prevents unnecessary calls.
             onBindViewHolder(holder, position)
        }
    }

    override fun getItemCount(): Int {
        return papers.size
    }

    fun setData(newPapers: List<PaperInfo>, curPaperId: Int) {
        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize(): Int = papers.size
            override fun getNewListSize(): Int = newPapers.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return papers[oldItemPosition].id == newPapers[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = papers[oldItemPosition]
                val newItem = newPapers[newItemPosition]
                // Check if content changed AND if the "current paper" status changed for this item
                val wasSelected = oldItem.id == this@PaperListAdapter.curPaperId
                val isSelected = newItem.id == curPaperId
                
                return oldItem == newItem && wasSelected == isSelected
            }
        })

        papers.clear()
        papers.addAll(newPapers)
        this.curPaperId = curPaperId
        diffResult.dispatchUpdatesTo(this)
    }

    fun setEditMode(enabled: Boolean) {
        if (isEditMode != enabled) {
            isEditMode = enabled
            notifyDataSetChanged() // Edit mode affects all items, so full refresh is appropriate
        }
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return
        val fromItem = papers.removeAt(fromPosition)
        papers.add(toPosition, fromItem)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getPapers(): List<PaperInfo> = papers.toMutableList()
}