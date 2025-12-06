package com.gorden.dayexam.ui.sheet.search

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gorden.dayexam.R
import com.gorden.dayexam.databinding.SearchSheetLayoutBinding
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.SearchItem
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.utils.ScreenUtils
import com.jeremyliao.liveeventbus.LiveEventBus

class SearchSheetDialog : BottomSheetDialogFragment(), TextWatcher {

    private var _binding: SearchSheetLayoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var rootView: View
    private lateinit var parent: View
    private lateinit var searchInput: EditText
    private lateinit var clearInputBtn: ImageButton
    private lateinit var questionList: RecyclerView
    private lateinit var adapter: SearchAdapter

    private var viewModel: SearchViewModel? = null

    private var curSearchKey: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireActivity(), 0)
        _binding = SearchSheetLayoutBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)
        rootView = binding.searchSheetContainer
        searchInput = binding.searchKeyInput
        clearInputBtn = binding.clearContent
        questionList = binding.searchQuestionList
        clearInputBtn.setOnClickListener {
            searchInput.text.clear()
        }
        setList()
        searchInput.addTextChangedListener(this)
        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)
        viewModel?.searchResult()?.observe(this, {
            questionList.layoutManager?.scrollToPosition(0)
            adapter.setData(it, curSearchKey)
        })
        registerAction()
        return dialog
    }

    @SuppressLint("ServiceCast")
    override fun onStart() {
        super.onStart()
        parent = rootView.parent as ViewGroup
        val params = parent.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior as BottomSheetBehavior
        behavior.peekHeight = ScreenUtils.screenHeight() / 3 * 2
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        rootView.postDelayed({
            searchInput.requestFocus()
            showSoftInput()
        }, 100)
    }

    override fun onResume() {
        super.onResume()
        this.curSearchKey = ""
        adapter.setData(listOf(), curSearchKey)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setList() {
        adapter = SearchAdapter()
        questionList.adapter = adapter
        questionList.layoutManager = LinearLayoutManager(requireContext())
        val divider = DividerItemDecoration(requireContext(), LinearLayout.VERTICAL)
        divider.setDrawable(resources.getDrawable(R.drawable.question_group_inset_recyclerview_divider, null))
        questionList.addItemDecoration(divider)
        questionList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                hideSoftInput()
            }
        })
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

    }

    override fun afterTextChanged(p0: Editable?) {
        curSearchKey = p0.toString()
        doSearch()
    }

    private fun doSearch() {
        if (curSearchKey.isNotEmpty()) {
            viewModel?.search(curSearchKey)
        }
    }

    private fun registerAction() {
        LiveEventBus.get(EventKey.SEARCH_RESULT_ITEM_CLICK, SearchItem::class.java)
            .observe(requireActivity()) {
                hideSoftInput()
                dismiss()
            }
    }

    private fun hideSoftInput() {
        val imm = (requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
        imm.hideSoftInputFromWindow(binding.searchQuestionList.windowToken, 0)
    }

    private fun showSoftInput() {
        val imm = (requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
        imm.showSoftInput(binding.searchKeyInput, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}