package com.gorden.dayexam.ui.book

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo

class PaperListAdapter: RecyclerView.Adapter<PaperViewHolder>() {

    private var papers = listOf<PaperInfo>()
    private var curPaperId = 0
    private var isRecycleBin = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaperViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.paper_item, parent, false)
        return PaperViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PaperViewHolder, position: Int) {
        holder.setData(papers[position], curPaperId, isRecycleBin)
    }

    override fun getItemCount(): Int {
        return papers.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(papers: List<PaperInfo>, curPaperId: Int, isRecycleBin: Boolean) {
        this.papers = papers
        this.curPaperId = curPaperId
        this.isRecycleBin = isRecycleBin
        notifyDataSetChanged()
    }
}