package com.gorden.dayexam.ui.home.shortcut

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.gorden.dayexam.BaseActivity
import com.gorden.dayexam.MainActivity
import com.gorden.dayexam.R
import com.gorden.dayexam.databinding.ActivityFastQuestionSelectBinding
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch

class FastQuestionSelectActivity: BaseActivity() {

    companion object {
        const val PAPER_ID_KEY = "paperId"
        const val CURRENT_POSITION = "currentPosition"
    }

    private var adapter: QuestionListAdapter? = null
    private lateinit var viewModel: FastSelectViewModel
    private var currentPosition = 0
    private lateinit var binding: ActivityFastQuestionSelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel =  ViewModelProvider(this).get(FastSelectViewModel::class.java)
        binding = ActivityFastQuestionSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()
        val paperId = intent.getIntExtra(PAPER_ID_KEY, 0)
        currentPosition = intent.getIntExtra(CURRENT_POSITION, 0)
        initList()
        registerAction()
        lifecycleScope.launch {
            val questions = viewModel.currentQuestionDetail(paperId)
            adapter?.setData(questions, currentPosition)
            (binding.questionList.layoutManager as LinearLayoutManager).scrollToPosition(currentPosition)
            binding.toolbar.title = "要改"
        }
    }

    private fun initToolbar() {
        binding.toolbar.setTitleTextAppearance(this, R.style.XWWKBoldTextAppearance)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initList() {
        binding.questionList.layoutManager = LinearLayoutManager(this)
        binding.questionList.setHasFixedSize(true)
        adapter = QuestionListAdapter()
        val divider = DividerItemDecoration(this, LinearLayout.VERTICAL)
        divider.setDrawable(resources.getDrawable(R.drawable.question_group_inset_recyclerview_divider, null))
        binding.questionList.addItemDecoration(divider)
        binding.questionList.adapter = adapter
    }

    private fun registerAction() {
        LiveEventBus.get(EventKey.SELECT_QUESTION, Int::class.java)
            .observe(this) {
                val intent = Intent()
                intent.putExtra(SimpleQuestionViewHolder.SELECT_POSITION, it)
                setResult(MainActivity.SELECT_QUESTION_RESULT_CODE, intent)
                finish()
            }
    }
}
