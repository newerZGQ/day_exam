package com.gorden.dayexam.ui.action

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.Book
import com.gorden.dayexam.db.entity.Course
import com.gorden.dayexam.db.entity.Paper
import com.gorden.dayexam.repository.DataRepository


class MovePaperAction(private val owner: LifecycleOwner, val context: Context, val paperId: Int): Action {

    private val courses = MutableLiveData<List<Course>>()
    private var books: LiveData<List<Book>>? = null
    private var papers: LiveData<List<Paper>>? = null

    override fun start() {
        selectCourse()
    }

    private fun selectCourse() {
        courses.observe(owner,
            { it ->
                val resources = context.resources
                if (it.isEmpty()) {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.no_course_found_msg),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val titles = it.map {
                        it.title
                    }
                    AlertDialog.Builder(context, R.style.MyAlertDialogTheme)
                        .setTitle(resources.getString(R.string.select_target_course))
                        .setItems(titles.toTypedArray()
                        ) { p0, p1 ->
                            selectBook(it[p1])
                            p0.dismiss()
                        }.show()

                }
                courses.removeObservers(owner)
            }
        )
        DataRepository.getAllCourseExcludeRecycleBin(courses)
    }

    private fun selectBook(course: Course) {
        books = DataRepository.getAllBooks(course.id)
        books?.observe(
            owner, { it ->
                val resources = context.resources
                if (it.isEmpty()) {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.no_book_found_msg),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val titles = it.map {
                        it.title
                    }
                    AlertDialog.Builder(context, R.style.MyAlertDialogTheme)
                        .setTitle(resources.getString(R.string.select_target_book))
                        .setItems(titles.toTypedArray()
                        ) { p0, p1 ->
                            val book = it[p1]
                            selectPaper(book)
                            p0.dismiss()
                        }.show()

                }
                books?.removeObservers(owner)
            }
        )
    }

    private fun selectPaper(book: Book) {
        papers = DataRepository.getPapersByBookId(book.id)
        papers?.observe(
            owner, { it ->
                val resources = context.resources
                if (it.isEmpty()) {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.no_paper_found_msg),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val titles = it.map {
                        it.title
                    }
                    AlertDialog.Builder(context, R.style.MyAlertDialogTheme)
                        .setTitle(resources.getString(R.string.select_target_paper))
                        .setItems(titles.toTypedArray()
                        ) { p0, p1 ->
                            val paper = it[p1]
                            DataRepository.movePaper(paperId, paper.id)
                            DataRepository.increaseContentVersion()
                            p0.dismiss()
                        }.show()

                }
                papers?.removeObservers(owner)
            }
        )
    }
}