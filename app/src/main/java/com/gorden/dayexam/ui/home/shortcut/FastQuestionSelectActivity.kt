package com.gorden.dayexam.ui.home.shortcut

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.BaseActivity
import com.gorden.dayexam.MainActivity
import com.gorden.dayexam.R
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.android.synthetic.main.app_bar_main.*
import org.apache.poi.ss.formula.functions.Even

class FastQuestionSelectActivity: BaseActivity() {

    companion object {
        const val PAPER_ID_KEY = "paperId"
        const val CURRENT_POSITION = "currentPosition"
    }

    private var adapter: QuestionListAdapter? = null
    private lateinit var viewModel: FastSelectViewModel
    private var currentPosition = 0
    private var recyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel =  ViewModelProvider(this).get(FastSelectViewModel::class.java)
        setContentView(R.layout.activity_fast_question_select)
        initToolbar()
        val paperId = intent.getIntExtra(PAPER_ID_KEY, 0)
        currentPosition = intent.getIntExtra(CURRENT_POSITION, 0)
        initList()
        registerAction()
        viewModel.currentQuestionDetail(paperId).observe(this, {
            if (it == null) {
                return@observe
            }
            adapter?.setData(it, currentPosition)
            (recyclerView?.layoutManager as LinearLayoutManager).scrollToPosition(currentPosition)
            toolbar?.title = "要改"
        })

    }

    private fun initToolbar() {
        toolbar.setTitleTextAppearance(this, R.style.XWWKBoldTextAppearance)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initList() {
        recyclerView = findViewById<RecyclerView>(R.id.question_list)
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.setHasFixedSize(true)
        adapter = QuestionListAdapter()
        val divider = DividerItemDecoration(this, LinearLayout.VERTICAL)
        divider.setDrawable(resources.getDrawable(R.drawable.question_group_inset_recyclerview_divider, null))
        recyclerView?.addItemDecoration(divider)
        recyclerView?.adapter = adapter
    }

    private fun registerAction() {
        LiveEventBus.get(EventKey.SELECT_QUESTION, Int::class.java)
            .observe(this, {
                val intent = Intent()
                intent.putExtra(SimpleQuestionViewHolder.SELECT_POSITION, it)
                setResult(MainActivity.SELECT_QUESTION_RESULT_CODE, intent)
                finish()
            })
    }
}
