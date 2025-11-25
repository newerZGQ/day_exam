package com.gorden.dayexam.ui.book

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.Book
import com.gorden.dayexam.repository.model.BookWithPaper
import com.gorden.dayexam.repository.DataRepository
import java.util.*

class BooksAdapter: RecyclerView.Adapter<BookViewHolder>(), DragCallback.OnItemTouchListener {

    private var books = listOf<BookWithPaper>()
    private var curBookId = 0
    private var curPaperId = 0
    private var isRecycleBin = false
    private var showDragHandle = false

    private var allowUpdate = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.book_list_item, parent, false)
        return BookViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.setData(books[position], curBookId, curPaperId, showDragHandle, isRecycleBin)
    }

    override fun getItemCount(): Int {
        return books.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(books: List<BookWithPaper>, curBookId: Int, curPaperId: Int, isRecycleBin: Boolean) {
        this.books = books
        this.curBookId = curBookId
        this.curPaperId = curPaperId
        this.isRecycleBin = isRecycleBin
        notifyDataSetChanged()
    }

    fun showDragHandle() {
        this.showDragHandle = true
        notifyDataSetChanged()
    }

    fun hideDragHandle() {
        this.showDragHandle = false
        notifyDataSetChanged()
    }

    override fun onMove(fromPosition: Int, toPosition: Int) {
        books[fromPosition].book.position = toPosition + 1
        books[toPosition].book.position = fromPosition + 1
        Collections.swap(books, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        allowUpdate = true
    }

    override fun onSwiped(position: Int) {

    }

    override fun clearView() {
        if (allowUpdate) {
            val updateBooks = mutableListOf<Book>()
            books.forEach {
                updateBooks.add(it.book)
            }
            DataRepository.updateBooks(updateBooks)
        }
        allowUpdate = false
    }
}