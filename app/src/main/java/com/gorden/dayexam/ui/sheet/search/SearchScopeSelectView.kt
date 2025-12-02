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

        const val SEARCH_GLOBAL = 4
        const val SEARCH_RECYCLE_BIN = 5
    }

    private var currentPaper: LinearLayout
    private var searchInPaperCheck: ImageView

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