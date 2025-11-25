package com.gorden.dayexam.ui.sheet.search

import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.model.SearchItem
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.utils.BookUtils
import com.jeremyliao.liveeventbus.LiveEventBus

class SearchAdapter: RecyclerView.Adapter<SearchItemViewHolder>() {

    private var data = listOf<SearchItem>()
    private var searchKey: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_result_item, parent, false)
        return SearchItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SearchItemViewHolder, position: Int) {
        val searchItem = data[position]
        holder.courseTitle.text = "课:" + searchItem.courseTitle
        holder.bookTitle.text = "书:" + searchItem.bookTitle
        holder.paperTitle.text = "《" + searchItem.paperTitle + "》"
        holder.questionType.text = BookUtils.getTypeName(searchItem.questionType)
        holder.elementType.text = BookUtils.getElementParentTypeName(searchItem.elementType)
        holder.elementContent.text = getSpannableContent(searchItem.elementContent)
        holder.itemView.setOnClickListener {
            LiveEventBus.get(EventKey.SEARCH_RESULT_ITEM_CLICK, SearchItem::class.java)
                .post(searchItem)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<SearchItem>, searchKey: String) {
        this.data = data
        this.searchKey = searchKey
        notifyDataSetChanged()
    }

    private fun getSpannableContent(content: String): SpannableString {
        // 搜索词前面的最大字符长度
        val maxPreLength = 45
        if (searchKey.isEmpty() || !content.contains(searchKey)) {
            return SpannableString(content)
        }
        var newContent = content.replace("\n", "  ")
        var index = newContent.indexOf(searchKey)
        if (index > maxPreLength) {
            newContent = newContent.substring(index - maxPreLength)
            index = newContent.indexOf(searchKey)
        }
        val result = SpannableString(newContent)
        result.setSpan(
            BackgroundColorSpan(ContextHolder.application.resources.getColor(R.color.search_item_key_background)),
            index,
            index + searchKey.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        result.setSpan(
            ForegroundColorSpan(ContextHolder.application.resources.getColor(R.color.font_common_color)),
            index,
            index + searchKey.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        return result
    }
}