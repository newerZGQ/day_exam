package com.gorden.dayexam.ui.book.question

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.Book
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.repository.model.PaperDetail
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.utils.ScreenUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import java.lang.StringBuilder
import java.text.SimpleDateFormat

class PaperViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)  {
    private val container:View = itemView.findViewById(R.id.paper_item_container)
    private val rippleContainer:View = itemView.findViewById(R.id.paper_ripple_item_container)
    private val title: TextView = itemView.findViewById(R.id.PaperTitle)
    private val desc: TextView = itemView.findViewById(R.id.paper_desc)
    private val record: TextView = itemView.findViewById(R.id.studyRecord)
    private val lastTouchDownXY = arrayOf(0f, 0f)

    @SuppressLint("ClickableViewAccessibility")
    fun onBind(paperDetail: PaperDetail, book: Book, curBookId: Int, adapter: PaperAdapter, isRecycleBin: Boolean) {
        val resources = itemView.context.resources
        if (adapter.isSortMode) {
            itemView.findViewById<View>(R.id.paper_drag_handle).visibility = View.VISIBLE
        } else {
            itemView.findViewById<View>(R.id.paper_drag_handle).visibility = View.GONE
        }
        title.text = paperDetail.paperInfo.title
        desc.text = generateDesc(paperDetail)
        record.text = generateStudyRecordInfo(paperDetail)
        if (adapterPosition == adapter.selectPosition && curBookId == book.id) {
            container.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark))
        } else {
            container.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        }
        rippleContainer.setOnClickListener {
            if (adapter.isSortMode) {
                adapter.cancelSortMode()
                return@setOnClickListener
            }
            adapter.selectPosition = position
            LiveEventBus.get(
                EventKey.PAPER_CONTAINER_CLICKED,
                EventKey.PaperClickEventModel::class.java)
                .post(EventKey.PaperClickEventModel(book.id, paperDetail.paperInfo.id))
        }
        rippleContainer.setOnLongClickListener {
            if (adapter.isSortMode) {
                return@setOnLongClickListener true
            } else {
                popMenu(adapter, paperDetail, isRecycleBin)
            }
            true
        }
        rippleContainer.setOnTouchListener { _, motionEvent ->
            if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                this.lastTouchDownXY[0] = motionEvent.rawX
                this.lastTouchDownXY[1] = motionEvent.rawY
            }
            false
        }
    }

    private fun popMenu(adapter: PaperAdapter, paperDetail: PaperDetail, isRecycleBin: Boolean) {
        var menuLayoutId = R.layout.paper_item_menu_layout
        if (isRecycleBin) {
            menuLayoutId = R.layout.recycle_bin_paper_item_menu_layout
        }
        val resources = itemView.context.resources
        val menuView = LayoutInflater.from(itemView.context)
            .inflate(menuLayoutId, null)
        menuView.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val measureHeight = menuView.measuredHeight
        val popupWindow = PopupWindow(menuView, ViewGroup.LayoutParams.WRAP_CONTENT, measureHeight)
        popupWindow.isFocusable = true
        // 计算偏移位置
        val screenHeight = ScreenUtils.screenHeight()
        popupWindow.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.colorPrimary)))
        popupWindow.elevation = 200f
        if ((screenHeight - lastTouchDownXY[1]) < measureHeight) {
            popupWindow.showAsDropDown(itemView, lastTouchDownXY[0].toInt(),
                itemView.height / 2)
        } else {
            popupWindow.showAsDropDown(itemView, lastTouchDownXY[0].toInt(),
                -itemView.height / 2)
        }
        menuView.findViewById<View>(R.id.add_question_from_file)?.setOnClickListener {
            LiveEventBus.get(EventKey.PAPER_MENU_ADD_QUESTION_FROM_FILE, EventKey.QuestionAddEventModel::class.java)
                .post(EventKey.QuestionAddEventModel(paperDetail.paperInfo.bookId, paperDetail.paperInfo.id))
            popupWindow.dismiss()
        }
        menuView.findViewById<View>(R.id.edit_paper)?.setOnClickListener {
            LiveEventBus.get(EventKey.PAPER_MENU_EDIT_PAPER, PaperInfo::class.java)
                .post(paperDetail.paperInfo)
            popupWindow.dismiss()
        }
        menuView.findViewById<View>(R.id.delete_paper)?.setOnClickListener {
            LiveEventBus.get(EventKey.PAPER_MENU_DELETE_PAPER, PaperInfo::class.java)
                .post(paperDetail.paperInfo)
            popupWindow.dismiss()
        }
        menuView.findViewById<View>(R.id.sortByHand)?.setOnClickListener {
            adapter.setSortMode()
            popupWindow.dismiss()
        }
        menuView.findViewById<View>(R.id.move)?.setOnClickListener {
            LiveEventBus.get(EventKey.PAPER_MENU_MOVE_PAPER, PaperInfo::class.java)
                .post(paperDetail.paperInfo)
            popupWindow.dismiss()
        }
    }

    private fun generateDesc(paperDetail: PaperDetail): String {
        val question = paperDetail.question
        return if (question?.body == null || question.body.element.isEmpty()) {
            ContextHolder.application.resources.getString(R.string.none_question_desc)
        } else {
            question.body.element[0].content
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun generateStudyRecordInfo(paperDetail: PaperDetail): String {
        val result = StringBuilder()
        val resources = ContextHolder.application.resources
        val lastStudy = resources.getString(R.string.last_study_time)
        val total = resources.getString(R.string.total)
        val hasStudy = resources.getString(R.string.has_study)
        val shortQuestion = resources.getString(R.string.short_question)
        val count = resources.getString(R.string.count)
        val hasNotStart = resources.getString(R.string.has_not_start_study)
        if (paperDetail.studyInfo.lastStudyDate != null) {
            result.append("$lastStudy ")
            val formatter = SimpleDateFormat("MM-dd HH:mm")
            result.append(formatter.format(paperDetail.studyInfo.lastStudyDate))
            result.append(" ")
        } else {
            result.append("$hasNotStart ")
        }
        result.append(total)
        result.append(paperDetail.questionCount)
        result.append("$shortQuestion ")
        result.append(hasStudy)
        result.append(paperDetail.studyInfo.studyCount)
        result.append(count)
        return result.toString()
    }
}