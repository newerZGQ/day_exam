package com.gorden.dayexam.ui.book

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.action.*
import com.jeremyliao.liveeventbus.LiveEventBus
import java.lang.Exception

class PaperListFragment : Fragment() {

    private val requestCode = 1001

    private lateinit var paperListViewModel: PaperListViewModel
    val adapter = PaperListAdapter()
    private var curPaperId: Int = 0
    private var isRecycleBin: Boolean = false

    private lateinit var paperList: RecyclerView

    private var targetPaperInfo: EventKey.QuestionAddEventModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_book_list_layout, container, false)
        initPaperList(root)

        return root
    }

    @SuppressLint("SetTextI18n")
    private fun initPaperList(root: View) {
        paperList = root.findViewById(R.id.book_list)
        paperList.adapter = adapter
        paperList.layoutManager = LinearLayoutManager(this.context)

        paperListViewModel = ViewModelProvider(this).get(PaperListViewModel::class.java)
        paperListViewModel.getAllPapers().observe(viewLifecycleOwner) {
            if (it == null) {
                curPaperId = 0
                adapter.setData(listOf(), curPaperId, false)

            } else {
                // TODO: Get current paper ID from somewhere else if needed, or remove highlighting
                adapter.setData(it, curPaperId, isRecycleBin)

            }
        }
        registerPaperClickedEvent()
    }



    private fun editPaper(paperInfo: PaperInfo) {
        activity?.let {
            EditPaperAction(it, paperInfo).start()
        }
    }

    private fun deletePaper(paperInfo: PaperInfo) {
        activity?.let {
            DeletePaperAction(it, paperInfo).start()
        }
    }

    private fun toFileBrowser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, requestCode, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == this.requestCode) {
            if (targetPaperInfo == null) {
                return
            }
            try {
                data?.data?.let {
                    LiveEventBus.get(EventKey.START_PROGRESS_BAR, Int::class.java).post(0)
                    AppExecutors.diskIO().execute {
                        // TODO: Handle file import logic correctly with new PaperInfo structure if needed
                        // For now assuming existing logic works or needs adaptation
                         val inputStream = context?.contentResolver?.openInputStream(it)
                         // BookParser.parse(...) // This might need adjustment as BookParser now takes PaperInfo
                        
                        AppExecutors.mainThread().execute {
                            LiveEventBus.get(EventKey.END_PROGRESS_BAR, Int::class.java).post(0)
                        }
                    }
                }
            } catch (e: Exception) {
                // TODO 异常处理
            }
        }
    }

    private fun registerPaperClickedEvent() {
        LiveEventBus.get(EventKey.PAPER_MENU_ADD_QUESTION_FROM_FILE, EventKey.QuestionAddEventModel::class.java)
            .observe(this, {
                targetPaperInfo = it
                toFileBrowser()
            })
        // paper操作
        LiveEventBus.get(EventKey.PAPER_MENU_EDIT_PAPER, PaperInfo::class.java)
            .observe(this, {
                editPaper(it)
            })
        LiveEventBus.get(EventKey.PAPER_MENU_DELETE_PAPER, PaperInfo::class.java)
            .observe(this, {
                deletePaper(it)
            })
    }

}