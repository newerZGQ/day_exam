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
import com.gorden.dayexam.MainActivity
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.parser.BookParser
import com.gorden.dayexam.repository.DataRepository
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

    private lateinit var moreMenu: ImageButton
    private lateinit var openSort: ImageButton
    private lateinit var openSearch: ImageButton
    private lateinit var courseTitle: TextView
    private lateinit var paperList: RecyclerView

    private var targetPaperInfo: EventKey.QuestionAddEventModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_book_list_layout, container, false)
        initPaperList(root)
        initActionBar(root)
        return root
    }

    @SuppressLint("SetTextI18n")
    private fun initPaperList(root: View) {
        paperList = root.findViewById(R.id.book_list)
        paperList.adapter = adapter
        paperList.layoutManager = LinearLayoutManager(this.context)

        paperListViewModel = ViewModelProvider(this).get(PaperListViewModel::class.java)
        paperListViewModel.getAllPapers().observe(viewLifecycleOwner, {
            if (it == null) {
                curPaperId = 0
                adapter.setData(listOf(), curPaperId, false)
                val bookString = context?.resources?.getString(R.string.book)
                courseTitle.text = bookString
            } else {
                // TODO: Get current paper ID from somewhere else if needed, or remove highlighting
                adapter.setData(it, curPaperId, isRecycleBin)
                val bookString = context?.resources?.getString(R.string.book)
                courseTitle.text = "" + bookString + "(" + it.size + ")"
            }
        })
        registerPaperClickedEvent()
    }

    private fun initActionBar(root: View) {
        moreMenu = root.findViewById(R.id.moreMenu)
        moreMenu.visibility = View.GONE // Hide more menu for now as book creation is removed
        
        courseTitle = root.findViewById(R.id.courseTitle)
        openSort = root.findViewById(R.id.openSort)
        openSort.visibility = View.GONE // Hide sort for now

        openSearch = root.findViewById(R.id.openSearch)
        openSearch.setOnClickListener {
            LiveEventBus.get(EventKey.SEARCH_CLICKED, Int::class.java)
                .post(0)
        }
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

    private fun movePaper(paperInfo: PaperInfo) {
        MovePaperAction(this, requireActivity(), paperInfo.id).start()
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
        LiveEventBus
            .get(EventKey.PAPER_CONTAINER_CLICKED,
                EventKey.PaperClickEventModel::class.java)
            .observe(this, {
                    DataRepository.updateCourseStatus(
                        0, // Course ID removed
                        0, // Book ID removed
                        it.paperId)
                (activity as MainActivity).closeDrawerLayout()
            })

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
        LiveEventBus.get(EventKey.PAPER_MENU_MOVE_PAPER, PaperInfo::class.java)
            .observe(this, {
                movePaper(it)
            })
    }

}