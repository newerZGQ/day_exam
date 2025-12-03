package com.gorden.dayexam.ui.paper

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo

class PaperListAdapter: RecyclerView.Adapter<PaperViewHolder>() {

    private var papers = listOf<PaperInfo>()
    private var curPaperId = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaperViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.paper_item, parent, false)
        return PaperViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PaperViewHolder, position: Int) {
        holder.setData(papers[position], curPaperId)
    }

    override fun getItemCount(): Int {
        return papers.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(papers: List<PaperInfo>, curPaperId: Int) {
        this.papers = papers
        this.curPaperId = curPaperId
        notifyDataSetChanged()
    }
}