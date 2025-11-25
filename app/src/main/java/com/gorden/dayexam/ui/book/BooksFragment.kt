package com.gorden.dayexam.ui.book

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.MainActivity
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.Book
import com.gorden.dayexam.db.entity.Paper
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.parser.BookParser
import com.gorden.dayexam.parser.image.ImageCacheManager
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.action.*
import com.jeremyliao.liveeventbus.LiveEventBus
import java.lang.Exception

class BooksFragment : Fragment() {

    private val requestCode = 1001

    private lateinit var bookViewModel: BookViewModel
    val adapter = BooksAdapter()
    private var curCourseId: Int = 0
    private var curBookId: Int = 0
    private var curPaperId: Int = 0
    private var isRecycleBin: Boolean = false

    private lateinit var moreMenu: ImageButton
    private lateinit var openSort: ImageButton
    private lateinit var openSearch: ImageButton
    private lateinit var courseTitle: TextView
    private lateinit var bookList: RecyclerView
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var isDragMode = false

    private var targetPaperInfo: EventKey.QuestionAddEventModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_book_list_layout, container, false)
        initBookList(root)
        initActionBar(root)
        return root
    }

    @SuppressLint("SetTextI18n")
    private fun initBookList(root: View) {
        bookList = root.findViewById(R.id.book_list)
        bookList.adapter = adapter
        bookList.layoutManager = LinearLayoutManager(this.context)

        val touchCallback = DragCallback()
        touchCallback.listener = adapter
        itemTouchHelper = ItemTouchHelper(touchCallback)

        bookViewModel = ViewModelProvider(this).get(BookViewModel::class.java)
        bookViewModel.getBookDetail().observe(viewLifecycleOwner, {
            if (it == null) {
                curCourseId = 0
                curBookId = 0
                curPaperId = 0
                adapter.setData(listOf(), curBookId, curPaperId, false)
                val bookString = context?.resources?.getString(R.string.book)
                courseTitle.text = bookString
            } else {
                curCourseId = it.courseId
                curBookId = it.bookId
                curPaperId = it.paperId
                isRecycleBin = it.isRecycleBin
                adapter.setData(it.books, curBookId, curPaperId, isRecycleBin)
                val bookString = context?.resources?.getString(R.string.book)
                courseTitle.text = "" + bookString + "(" + it.books.size + ")"
                resetActionButton(it.isRecycleBin)
            }
        })
        registerBookClickedEvent()
    }

    private fun initActionBar(root: View) {
        moreMenu = root.findViewById(R.id.moreMenu)
        moreMenu.setOnClickListener {
            showPopMenu()
        }
        courseTitle = root.findViewById(R.id.courseTitle)
        openSort = root.findViewById(R.id.openSort)
        openSort.setOnClickListener {
            isDragMode = if (isDragMode) {
                itemTouchHelper.attachToRecyclerView(null)
                adapter.hideDragHandle()
                false
            } else {
                itemTouchHelper.attachToRecyclerView(bookList)
                adapter.showDragHandle()
                true
            }
        }
        openSearch = root.findViewById(R.id.openSearch)
        openSearch.setOnClickListener {
            LiveEventBus.get(EventKey.SEARCH_CLICKED, Int::class.java)
                // 0没有意义
                .post(0)
        }
    }

    private fun resetActionButton(isRecycleBin: Boolean) {
        openSort.visibility = if (isRecycleBin) View.GONE else View.VISIBLE
        moreMenu.visibility = if (isRecycleBin) View.GONE else View.VISIBLE
    }

    private fun showPopMenu() {
        val menuView = layoutInflater.inflate(R.layout.book_list_info_menu_layout, null)
        val popupWindow = PopupWindow(menuView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.colorPrimary)))
        popupWindow.elevation = 200f
        popupWindow.showAsDropDown(moreMenu, 0, -moreMenu.height)
        val addBook = menuView.findViewById<View>(R.id.addBookContainer)
        addBook.setOnClickListener {
            createBook()
            popupWindow.dismiss()
        }
    }

    private fun createPaper(book: Book) {
        activity?.let {
            CreatePaperAction(it, book.id).start()
        }
    }

    private fun editPaper(paper: Paper) {
        activity?.let {
            EditPaperAction(it, paper).start()
        }
    }

    private fun deletePaper(paper: Paper) {
        activity?.let {
            DeletePaperAction(it, paper).start()
        }
    }

    private fun movePaper(paper: Paper) {
        MovePaperAction(this, requireActivity(), paper.id).start()
    }

    private fun createBook() {
        activity?.let {
            CreateBookAction(it, curCourseId).start()
        }
    }

    private fun deleteBook(book: Book) {
        activity?.let {
            DeleteBookAction(it, book).start()
        }
    }

    private fun editBook(book: Book) {
        activity?.let {
            EditBookAction(it, book).start()
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
                        val inputStream = context?.contentResolver?.openInputStream(it)
                        inputStream?.let { it1 ->
                            BookParser.parse(it1, targetPaperInfo?.bookId!!,
                                targetPaperInfo?.paperId!!
                            )
                            DataRepository.increaseContentVersion()
                        }
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

    private fun registerBookClickedEvent() {
        LiveEventBus
            .get(EventKey.PAPER_CONTAINER_CLICKED,
                EventKey.PaperClickEventModel::class.java)
            .observe(this, {
                    DataRepository.updateCourseStatus(
                        curCourseId,
                        it.bookId,
                        it.paperId)
                    ImageCacheManager.setCacheFolder(it.bookId.toString())
                (activity as MainActivity).closeDrawerLayout()
            })

        LiveEventBus.get(EventKey.PAPER_MENU_ADD_QUESTION_FROM_FILE, EventKey.QuestionAddEventModel::class.java)
            .observe(this, {
                targetPaperInfo = it
                toFileBrowser()
            })
        // paper操作
        LiveEventBus.get(EventKey.PAPER_MENU_EDIT_PAPER, Paper::class.java)
            .observe(this, {
                editPaper(it)
            })
        LiveEventBus.get(EventKey.PAPER_MENU_DELETE_PAPER, Paper::class.java)
            .observe(this, {
                deletePaper(it)
            })
        // book操作
        LiveEventBus.get(EventKey.CREATE_PAPER_CLICKED, Book::class.java)
            .observe(this, {
                createPaper(it)
            })
        LiveEventBus.get(EventKey.DELETE_BOOK_CLICKED, Book::class.java)
            .observe(this, {
                deleteBook(it)
            })
        LiveEventBus.get(EventKey.EDIT_BOOK_CLICKED, Book::class.java)
            .observe(this, {
                editBook(it)
            })
        LiveEventBus.get(EventKey.PAPER_MENU_MOVE_PAPER, Paper::class.java)
            .observe(this, {
                movePaper(it)
            })
    }

}