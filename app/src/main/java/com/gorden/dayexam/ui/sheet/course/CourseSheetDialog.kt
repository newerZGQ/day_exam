package com.gorden.dayexam.ui.sheet.course

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.Course
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.dialog.EditTextDialog
import com.gorden.dayexam.utils.ScreenUtils
import com.jeremyliao.liveeventbus.LiveEventBus


class CourseSheetDialog : BottomSheetDialogFragment() {

    private lateinit var rootView: View
    private lateinit var bottomActionView: View
    private lateinit var parent: View
    private lateinit var courseList: RecyclerView
    private lateinit var behavior: BottomSheetBehavior<View>
    private var pendingOptCourse: Course? = null
    private var curCourseId = 0
    private var recycleBinId = 0

    private lateinit var courseIcon: View
    private lateinit var confirmAction: View
    private lateinit var editCourse: View
    private lateinit var deleteCourse: View

    private val bottomSheetCallback = object : BottomSheetCallback() {
        override fun onStateChanged(view: View, newState: Int) {
        }

        override fun onSlide(view: View, slideOffset: Float) {
            resetBottomActionPositionByParentHeight()
        }
    }

    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        if (useContentHeightStrategy()) {
            resetBottomActionPositionByContentHeight()
        } else {
            resetBottomActionPositionByParentHeight()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireActivity(), 0)
        dialog.setContentView(R.layout.course_sheet_layout)
        rootView = dialog.findViewById(R.id.course_sheet_container)!!
        initGridList()
        setBottomBehavior()
        initBottomIcon()
        initAction()
        return dialog
    }

    override fun onStart() {
        super.onStart()
        // bottomsheetdialog第一次展示时不会触发onStateChanged，所以要主动矫正底部action的位置
        rootView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }

    private fun setBottomBehavior() {
        parent = rootView.parent as ViewGroup
        val params = parent.layoutParams as CoordinatorLayout.LayoutParams
        behavior = params.behavior as BottomSheetBehavior
        bottomActionView = rootView.findViewById(R.id.shortcutContainer)
        behavior.addBottomSheetCallback(bottomSheetCallback)
        behavior.peekHeight = ScreenUtils.screenHeight() / 3 * 2
    }

    private fun initBottomIcon() {
        courseIcon = rootView.findViewById(R.id.course_icon)
        editCourse = rootView.findViewById(R.id.edit_course)
        deleteCourse = rootView.findViewById(R.id.delete_course)
        confirmAction = rootView.findViewById(R.id.confirm_course_action)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initGridList() {
        val adapter = CourseAdapter()
        courseList = rootView.findViewById(R.id.courseList)
        courseList.layoutManager = LinearLayoutManager(this.context)
        courseList.adapter = adapter
        val divider = DividerItemDecoration(requireContext(), LinearLayout.VERTICAL)
        divider.setDrawable(resources.getDrawable(R.drawable.question_group_inset_recyclerview_divider, null))
        courseList.addItemDecoration(divider)

        DataRepository.getDContext().observe(this, {
            curCourseId = it.curCourseId
            recycleBinId = it.recycleBinId
            adapter.setDContext(it)
        })
        val viewModel = ViewModelProvider(this).get(CourseViewModel::class.java)
        viewModel.getAllCourse().observe(this, {
            adapter.setData(it)
        })
    }

    private fun initAction() {
        rootView.findViewById<ImageButton>(R.id.addCourse).setOnClickListener {
            val context = rootView.context
            EditTextDialog(context,
                context.resources.getString(R.string.dialog_create_course_title),
                context.resources.getString(R.string.dialog_create_course_subTitle),
                "",
                context.resources.getString(R.string.dialog_create_course_hint),
                "",
                context.resources.getString(R.string.dialog_create_course_sub_hint),
                editCallBack = object : EditTextDialog.EditCallBack {
                    override fun onConfirmContent(
                        dialog: EditTextDialog,
                        content: String,
                        subContent: String
                    ) {
                        if (content.isEmpty()) {
                            Toast.makeText(
                                requireActivity(),
                                resources.getString(R.string.course_empty_title_msg),
                                Toast.LENGTH_SHORT).show()
                            return
                        }
                        DataRepository.insertCourse(content, subContent)
                        DataRepository.increaseContentVersion()
                    }
                })
                .show()
        }
        LiveEventBus.get(EventKey.COURSE_ITEM_CLICKED, Int::class.java).observe(
            requireActivity(),
            {
                DataRepository.updateDContextCourseId(it)
                this.dismiss()
            })
        LiveEventBus.get(EventKey.SELECT_COURSE, Course::class.java).observe(
            requireActivity(),
            {
                setEditMode()
                pendingOptCourse = it
            }
        )
        editCourse.setOnClickListener {
            pendingOptCourse?.let {
                editCourse(it)
            }
        }
        deleteCourse.setOnClickListener {
            pendingOptCourse?.let {
                deleteCourse(it)
            }
        }
        confirmAction.setOnClickListener {
            confirmAction()
        }
    }

    private fun setEditMode() {
        courseIcon.visibility = View.GONE
        editCourse.visibility = View.VISIBLE
        deleteCourse.visibility = View.VISIBLE
        confirmAction.visibility = View.VISIBLE
    }

    private fun setNormalMode() {
        courseIcon.visibility = View.VISIBLE
        editCourse.visibility = View.GONE
        deleteCourse.visibility = View.GONE
        confirmAction.visibility = View.GONE
    }

    private fun editCourse(course: Course) {
        activity?.let {
            EditTextDialog(it,
                it.resources.getString(R.string.dialog_edit_course_title),
                it.resources.getString(R.string.dialog_edit_course_subTitle),
                course.title,
                "",
                course.description,
                it.resources.getString(R.string.dialog_edit_course_sub_hint),
                editCallBack = object : EditTextDialog.EditCallBack {
                    override fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String) {
                        if (content.isEmpty()) {
                            Toast.makeText(
                                requireActivity(),
                                resources.getString(R.string.course_empty_title_msg),
                                Toast.LENGTH_SHORT).show()
                            setNormalMode()
                            return
                        }
                        DataRepository.updateCourse(course.id, content, subContent)
                        DataRepository.increaseContentVersion()
                    }
                }).show()
        }
    }

    private fun deleteCourse(course: Course) {
        activity?.let {
            EditTextDialog(it,
                it.resources.getString(R.string.dialog_delete_course_title),
                it.resources.getString(R.string.dialog_delete_course_subTitle),
                editCallBack = object : EditTextDialog.EditCallBack {
                    override fun onConfirmContent(dialog: EditTextDialog, content: String, subContent: String) {
                        if (course.id == curCourseId) {
                            Toast.makeText(
                                requireActivity(),
                                resources.getString(R.string.course_delete_error_msg),
                                Toast.LENGTH_SHORT).show()
                            return
                        }
                        DataRepository.deleteCourse(course.id)
                        DataRepository.increaseContentVersion()
                        setNormalMode()
                    }
                }).show()
        }
    }

    private fun confirmAction() {
        val adapter = courseList.adapter as CourseAdapter
        adapter.cancelSelectMode()
        setNormalMode()
    }

    private fun useContentHeightStrategy(): Boolean {
        val contentView = rootView.findViewById<View>(R.id.content)
        val contentHeight = contentView.measuredHeight
        return if (contentHeight <= behavior.peekHeight) {
            true
        } else {
            behavior.state == BottomSheetBehavior.STATE_HIDDEN ||
                    behavior.state == BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun resetBottomActionPositionByContentHeight() {
        val contentView = rootView.findViewById<View>(R.id.content)
        val contentHeight = contentView.measuredHeight
        val visibleHeight = contentHeight.coerceAtMost(behavior.peekHeight)
        val bottomSheetVisibleHeight = visibleHeight - bottomActionView.height
        bottomActionView.translationY = bottomSheetVisibleHeight.toFloat()
    }

    private fun resetBottomActionPositionByParentHeight() {
        val outParentHeight = (parent.parent as View).height
        val bottomSheetVisibleHeight = outParentHeight - parent.top - bottomActionView.height
        bottomActionView.translationY = bottomSheetVisibleHeight.toFloat()
    }
}