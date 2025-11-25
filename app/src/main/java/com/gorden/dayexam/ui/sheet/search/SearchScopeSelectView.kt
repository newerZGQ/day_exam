package com.gorden.dayexam.ui.sheet.search

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.gorden.dayexam.R

class SearchScopeSelectView: FrameLayout {

    companion object {
        const val SEARCH_IN_PAPER = 1
        const val SEARCH_IN_BOOK = 2
        const val SEARCH_IN_COURSE = 3
        const val SEARCH_GLOBAL = 4
        const val SEARCH_RECYCLE_BIN = 5
    }

    private var currentPaper: LinearLayout
    private var searchInPaperCheck: ImageView
    private var currentBook: LinearLayout
    private var searchInBookCheck: ImageView
    private var currentCourse: LinearLayout
    private var searchInCourseCheck: ImageView
    private var currentGlobal: LinearLayout
    private var searchGlobalCheck: ImageView
    private var recycleBin: LinearLayout
    private var recycleBinCheck: ImageView

    private var curScope = SEARCH_IN_PAPER

    private var listener: ScopeSelectListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        LayoutInflater.from(context).inflate(R.layout.search_scope_select_layout, this)
        currentPaper = findViewById(R.id.search_in_paper)
        searchInPaperCheck = findViewById(R.id.search_in_paper_check)
        currentBook = findViewById(R.id.search_in_book)
        searchInBookCheck = findViewById(R.id.search_in_book_check)
        currentCourse = findViewById(R.id.search_in_course)
        searchInCourseCheck = findViewById(R.id.search_in_course_check)
        currentGlobal = findViewById(R.id.search_global)
        searchGlobalCheck = findViewById(R.id.search_global_check)
        recycleBin = findViewById(R.id.search_recycle_bin)
        recycleBinCheck = findViewById(R.id.search_recycle_bin_check)
        setClickEvent()
    }

    fun setListener(listener: ScopeSelectListener) {
        this.listener = listener
        this.listener?.onSelect(curScope)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setClickEvent() {
        currentPaper.setOnClickListener {
            curScope = SEARCH_IN_PAPER
            this.listener?.onSelect(curScope)
            currentPaper.background = context.getDrawable(R.drawable.search_scope_select_background)
            searchInPaperCheck.visibility = View.VISIBLE
            currentBook.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchInBookCheck.visibility = View.GONE
            currentCourse.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchInCourseCheck.visibility = View.GONE
            currentGlobal.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchGlobalCheck.visibility = View.GONE
            recycleBin.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            recycleBinCheck.visibility = View.GONE
        }
        currentBook.setOnClickListener {
            curScope = SEARCH_IN_BOOK
            this.listener?.onSelect(curScope)
            currentPaper.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchInPaperCheck.visibility = View.GONE
            currentBook.background = context.getDrawable(R.drawable.search_scope_select_background)
            searchInBookCheck.visibility = View.VISIBLE
            currentCourse.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchInCourseCheck.visibility = View.GONE
            currentGlobal.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchGlobalCheck.visibility = View.GONE
            recycleBin.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            recycleBinCheck.visibility = View.GONE
        }
        currentCourse.setOnClickListener {
            curScope = SEARCH_IN_COURSE
            this.listener?.onSelect(curScope)
            currentPaper.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchInPaperCheck.visibility = View.GONE
            currentBook.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchInBookCheck.visibility = View.GONE
            currentCourse.background = context.getDrawable(R.drawable.search_scope_select_background)
            searchInCourseCheck.visibility = View.VISIBLE
            currentGlobal.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchGlobalCheck.visibility = View.GONE
            recycleBin.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            recycleBinCheck.visibility = View.GONE
        }
        currentGlobal.setOnClickListener {
            curScope = SEARCH_GLOBAL
            this.listener?.onSelect(curScope)
            currentPaper.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchInPaperCheck.visibility = View.GONE
            currentBook.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchInBookCheck.visibility = View.GONE
            currentCourse.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchInCourseCheck.visibility = View.GONE
            currentGlobal.background = context.getDrawable(R.drawable.search_scope_select_background)
            searchGlobalCheck.visibility = View.VISIBLE
            recycleBin.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            recycleBinCheck.visibility = View.GONE
        }
        recycleBin.setOnClickListener {
            curScope = SEARCH_RECYCLE_BIN
            this.listener?.onSelect(curScope)
            currentPaper.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchInPaperCheck.visibility = View.GONE
            currentBook.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchInBookCheck.visibility = View.GONE
            currentCourse.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchInCourseCheck.visibility = View.GONE
            currentGlobal.background = context.getDrawable(R.drawable.search_scope_unselect_background)
            searchGlobalCheck.visibility = View.GONE
            recycleBin.background = context.getDrawable(R.drawable.search_scope_select_background)
            recycleBinCheck.visibility = View.VISIBLE
        }
    }

    interface ScopeSelectListener {
        fun onSelect(scope: Int)
    }

}