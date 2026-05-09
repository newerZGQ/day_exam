package com.gorden.dayexam.ui.sheet.question

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gorden.dayexam.R
import com.gorden.dayexam.databinding.DialogQuestionListBinding
import com.gorden.dayexam.repository.PaperDetailCache
import com.gorden.dayexam.repository.model.PaperDetail
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.repository.model.QuestionType
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.home.shortcut.QuestionListAdapter
import com.gorden.dayexam.ui.sheet.status.AnswerStatusAdapter
import com.gorden.dayexam.utils.SharedPreferenceUtil
import com.jeremyliao.liveeventbus.LiveEventBus

class QuestionListBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogQuestionListBinding? = null
    private val binding get() = _binding!!
    private var isGridView = false
    private var paperDetail: PaperDetail? = null

    fun setData(detail: PaperDetail) {
        this.paperDetail = detail
    }

    companion object {
        private val TYPE_GROUP_ORDER = listOf(
            QuestionType.FILL_BLANK,
            QuestionType.TRUE_FALSE,
            QuestionType.SINGLE_CHOICE,
            QuestionType.MULTIPLE_CHOICE,
            QuestionType.ESSAY_QUESTION
        )

        const val PAPER_ID_KEY = "paper_id"
        const val CURRENT_POSITION_KEY = "current_position"

        fun newInstance(paperId: Int, currentPosition: Int): QuestionListBottomSheet {
            val fragment = QuestionListBottomSheet()
            val args = Bundle()
            args.putInt(PAPER_ID_KEY, paperId)
            args.putInt(CURRENT_POSITION_KEY, currentPosition)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        return dialog
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(R.id.design_bottom_sheet)
        bottomSheet?.let { sheet ->
            val behavior = BottomSheetBehavior.from(sheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.isHideable = true
            behavior.isDraggable = true

            val displayMetrics = resources.displayMetrics
            val height = displayMetrics.heightPixels - (80 * displayMetrics.density).toInt()
            val layoutParams = sheet.layoutParams
            layoutParams.height = height
            sheet.layoutParams = layoutParams
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogQuestionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view.parent as? View)?.setBackgroundColor(resources.getColor(android.R.color.transparent))

        val paperId = arguments?.getInt(PAPER_ID_KEY) ?: -1
        val currentPosition = arguments?.getInt(CURRENT_POSITION_KEY) ?: 0

        if (paperId == -1) {
            dismiss()
            return
        }
        paperDetail = PaperDetailCache.get(paperId)

        val originalQuestions = paperDetail?.question
        if (originalQuestions == null) {
            dismiss()
            return
        }

        val sortByType = SharedPreferenceUtil.getBoolean(
            resources.getString(R.string.sort_mode_key), false
        )
        val displayQuestions = if (sortByType) {
            originalQuestions
                .withIndex()
                .sortedWith(compareBy({ typeGroupOrder(it.value.type) }, { it.index }))
                .map { it.value }
        } else {
            originalQuestions
        }
        val originalIndices = displayQuestions.map { originalQuestions.indexOf(it) }

        initHeader()
        initList(displayQuestions, currentPosition)
        initGrid(displayQuestions, originalIndices)
        binding.questionList.isNestedScrollingEnabled = !isGridView
        binding.questionGrid.isNestedScrollingEnabled = isGridView

        centerSelection(currentPosition)
    }

    private fun centerSelection(position: Int) {
         binding.questionList.post {
            val layoutManager = binding.questionList.layoutManager as LinearLayoutManager
            val height = binding.questionList.height
            layoutManager.scrollToPositionWithOffset(position, height / 2)
        }
    }

    private fun initHeader() {
        binding.ivToggle.setOnClickListener {
             toggleView()
        }
    }

    private fun toggleView() {
        isGridView = !isGridView
        if (isGridView) {
            binding.questionList.visibility = View.GONE
            binding.questionGrid.visibility = View.VISIBLE
            binding.questionList.isNestedScrollingEnabled = false
            binding.questionGrid.isNestedScrollingEnabled = true
            binding.ivToggle.setImageResource(R.drawable.ic_outline_format_list_numbered_24)
            binding.tvTitle.text = getString(R.string.paper_status_title)
        } else {
            binding.questionList.visibility = View.VISIBLE
            binding.questionGrid.visibility = View.GONE
            binding.questionList.isNestedScrollingEnabled = true
            binding.questionGrid.isNestedScrollingEnabled = false
            binding.ivToggle.setImageResource(R.drawable.ic_baseline_apps_24)
            binding.tvTitle.text = getString(R.string.list_question)
        }
    }

    private fun initList(questions: List<QuestionDetail>, currentPosition: Int) {
        binding.questionList.layoutManager = LinearLayoutManager(context)
        binding.questionList.setHasFixedSize(true)
        val listAdapter = QuestionListAdapter()
        listAdapter.setData(questions, currentPosition)
        val divider = DividerItemDecoration(context, LinearLayout.VERTICAL)
        divider.setDrawable(resources.getDrawable(R.drawable.question_group_inset_recyclerview_divider))
        binding.questionList.addItemDecoration(divider)
        binding.questionList.adapter = listAdapter
        LiveEventBus.get(EventKey.SELECT_QUESTION, Int::class.java).observe(viewLifecycleOwner) { displayPos ->
            selectQuestion(displayPos, questions)
        }
    }

    private fun initGrid(questions: List<QuestionDetail>, originalIndices: List<Int>) {
        val layoutManager = GridLayoutManager(context, 6)
        binding.questionGrid.layoutManager = layoutManager
        val gridAdapter = AnswerStatusAdapter(questions, originalIndices) { originalIndex ->
            selectQuestion(originalIndex, questions)
        }
        binding.questionGrid.adapter = gridAdapter
        binding.questionGrid.setHasFixedSize(true)
    }

    private fun selectQuestion(position: Int, questions: List<QuestionDetail>) {
        val question = questions.getOrNull(position) ?: return
        val originalQuestions = paperDetail?.question ?: return
        val originalIndex = originalQuestions.indexOf(question)
        LiveEventBus.get(EventKey.SEARCH_RESULT_ITEM_CLICK, Int::class.java).post(originalIndex)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun typeGroupOrder(type: Int): Int {
        return TYPE_GROUP_ORDER.indexOf(type).let { if (it == -1) Int.MAX_VALUE else it }
    }
}
