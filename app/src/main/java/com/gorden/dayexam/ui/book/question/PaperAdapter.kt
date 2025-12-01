package com.gorden.dayexam.ui.book.question

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.Book
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.repository.model.PaperDetail
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.ui.book.DragCallback
import java.util.*

class PaperAdapter: RecyclerView.Adapter<PaperViewHolder>(),
    DragCallback.OnItemTouchListener {
    private var papers = listOf<PaperDetail>()
    var selectPosition = -1
    private var curBookId = -1
    private var book: Book? = null
    private var isRecycleBin: Boolean = false

    var isSortMode = false
    private var recyclerView: RecyclerView? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    init {
        val touchCallback = DragCallback()
        touchCallback.listener = this
        itemTouchHelper = ItemTouchHelper(touchCallback)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaperViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.paper_item, parent, false)
        return PaperViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PaperViewHolder, position: Int) {
        val paperWithQuestion = papers[position]
        holder.onBind(paperWithQuestion, this.book!!, curBookId, this, isRecycleBin)
    }

    override fun getItemCount(): Int {
        return papers.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(papers: List<PaperDetail>, curPaperId: Int, curBookId: Int, book: Book, isRecycleBin: Boolean) {
        this.papers = papers
        this.isRecycleBin = isRecycleBin
        papers.forEachIndexed { index, paperWithQuestion ->
            if (paperWithQuestion.paperInfo.id == curPaperId) {
                this.selectPosition = index
            }
        }
        this.curBookId = curBookId
        this.book = book
        notifyDataSetChanged()
    }

    fun setSortMode() {
        isSortMode = true
        itemTouchHelper?.attachToRecyclerView(recyclerView)
        notifyDataSetChanged()
    }

    fun cancelSortMode() {
        isSortMode = false
        itemTouchHelper?.attachToRecyclerView(null)
        notifyDataSetChanged()
    }

    override fun onMove(fromPosition: Int, toPosition: Int) {
        papers[fromPosition].paperInfo.position = toPosition + 1
        papers[toPosition].paperInfo.position = fromPosition + 1
        Collections.swap(papers, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onSwiped(position: Int) {

    }

    override fun clearView() {
        val tPaperInfos = mutableListOf<PaperInfo>()
        papers.forEach {
            tPaperInfos.add(it.paperInfo)
        }
        DataRepository.updatePapers(tPaperInfos)
        DataRepository.increaseContentVersion()
    }
}