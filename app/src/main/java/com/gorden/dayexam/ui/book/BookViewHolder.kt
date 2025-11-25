package com.gorden.dayexam.ui.book

import android.annotation.SuppressLint
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.Book
import com.gorden.dayexam.repository.model.BookWithPaper
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.book.question.PaperAdapter
import com.jeremyliao.liveeventbus.LiveEventBus

@SuppressLint("UseCompatLoadingForDrawables")
class BookViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    private val titleContainer = itemView.findViewById<View>(R.id.bookInfoContainer)
    val title = itemView.findViewById<TextView>(R.id.bookTitle)!!
    private val paperList = itemView.findViewById<RecyclerView>(R.id.paperList)!!
    private val dragHandle = itemView.findViewById<ImageButton>(R.id.drag_handle)
    private val addPaper = itemView.findViewById<ImageButton>(R.id.addPaper)
    private val editBook = itemView.findViewById<ImageButton>(R.id.edit_paper)
    private val deleteBook = itemView.findViewById<ImageButton>(R.id.delete_paper)
    private var isDragMode = false
    private var isEditMode = false

    init {
        paperList.layoutManager = LinearLayoutManager(itemView.context)
        val adapter = PaperAdapter()
        paperList.adapter = adapter
        val divider = DividerItemDecoration(itemView.context, LinearLayout.VERTICAL)
        divider.setDrawable(itemView.context.resources.getDrawable(R.drawable.question_group_inset_recyclerview_divider, null))
        paperList.addItemDecoration(divider)
    }

    fun setData(data: BookWithPaper, curBookId: Int, curPaperId: Int, showDragHandle: Boolean, isRecycleBin: Boolean) {
        if (isRecycleBin) {
            setRecycleBinHolder(data, curBookId, curPaperId, isRecycleBin)
        } else {
            setCommonHolder(data, curBookId, curPaperId, showDragHandle, isRecycleBin)
        }
    }

    private fun setRecycleBinHolder(data: BookWithPaper, curBookId: Int, curPaperId: Int, isRecycleBin: Boolean) {
        addPaper.visibility = View.GONE
        title.text = data.book.title
        (paperList.adapter as PaperAdapter)
            .setData(data.papers, curPaperId, curBookId, data.book, isRecycleBin)
        titleContainer.setOnClickListener {
            switchPaperListVisibility()
        }
        titleContainer.isLongClickable = false
    }

    private fun setCommonHolder(data: BookWithPaper, curBookId: Int, curPaperId: Int, showDragHandle: Boolean, isRecycleBin: Boolean) {
        this.isDragMode = showDragHandle
        title.text = data.book.title
        if (showDragHandle) {
            setDragMode()
            cancelEditMode()
            hidePaperList()
        } else {
            cancelDragMode()
        }
        (paperList.adapter as PaperAdapter)
            .setData(data.papers, curPaperId, curBookId, data.book, isRecycleBin)
        titleContainer.setOnClickListener {
            if (isDragMode) {
                return@setOnClickListener
            }
            if (isEditMode) {
                cancelEditMode()
                return@setOnClickListener
            }
            switchPaperListVisibility()
        }
        titleContainer.setOnLongClickListener {
            if (isDragMode) {
                return@setOnLongClickListener true
            }
            if (isEditMode) {
                cancelEditMode()
                return@setOnLongClickListener true
            }
            setEditMode()
            true
        }
        addPaper.setOnClickListener {
            LiveEventBus.get(EventKey.CREATE_PAPER_CLICKED, Book::class.java)
                .post(data.book)
        }
        editBook.setOnClickListener {
            cancelEditMode()
            LiveEventBus.get(EventKey.EDIT_BOOK_CLICKED, Book::class.java)
                .post(data.book)
        }
        deleteBook.setOnClickListener {
            cancelEditMode()
            LiveEventBus.get(EventKey.DELETE_BOOK_CLICKED, Book::class.java)
                .post(data.book)
        }
    }

    private fun setDragMode() {
        dragHandle.visibility = View.VISIBLE
        addPaper.visibility = View.GONE
    }

    private fun cancelDragMode() {
        dragHandle.visibility = View.GONE
        addPaper.visibility = View.VISIBLE
    }

    private fun setEditMode() {
        editBook.visibility = View.VISIBLE
        deleteBook.visibility = View.VISIBLE
        isEditMode = true
    }

    private fun cancelEditMode() {
        editBook.visibility = View.GONE
        deleteBook.visibility = View.GONE
        isEditMode = false
    }

    private fun switchPaperListVisibility() {
        if (paperList.visibility == View.GONE) {
            paperList.visibility = View.VISIBLE
            itemView.findViewById<ImageButton>(R.id.expandIcon)
                .setImageResource(R.drawable.ic_baseline_expand_more_24)
        } else {
            paperList.visibility = View.GONE
            itemView.findViewById<ImageButton>(R.id.expandIcon)
                .setImageResource(R.drawable.ic_baseline_keyboard_arrow_right_24)
        }
    }

    private fun hidePaperList() {
        paperList.visibility = View.GONE
        itemView.findViewById<ImageButton>(R.id.expandIcon)
            .setImageResource(R.drawable.ic_baseline_keyboard_arrow_right_24)
    }
}