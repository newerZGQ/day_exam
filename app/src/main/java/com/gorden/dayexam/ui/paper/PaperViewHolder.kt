package com.gorden.dayexam.ui.paper

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.utils.ScreenUtils
import com.jeremyliao.liveeventbus.LiveEventBus

class PaperViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)  {
    private val container:View = itemView.findViewById(R.id.paper_item_container)
    private val rippleContainer:View = itemView.findViewById(R.id.paper_ripple_item_container)
    private val title: TextView = itemView.findViewById(R.id.PaperTitle)
    private val desc: TextView = itemView.findViewById(R.id.paper_desc)
    private val record: TextView = itemView.findViewById(R.id.studyRecord)
    private val lastTouchDownXY = arrayOf(0f, 0f)

    @SuppressLint("ClickableViewAccessibility")
    fun setData(paperInfo: PaperInfo, curPaperId: Int) {
        val resources = itemView.context.resources
        itemView.findViewById<View>(R.id.paper_drag_handle).visibility = View.GONE
        
        title.text = paperInfo.title
        desc.text = paperInfo.description
        record.text = "题目数量: ${paperInfo.questionCount}"

        if (paperInfo.id == curPaperId) {
            container.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark))
        } else {
            container.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        }
        rippleContainer.setOnClickListener {
            LiveEventBus.get(
                EventKey.PAPER_CONTAINER_CLICKED,
                EventKey.PaperClickEventModel::class.java)
                .post(EventKey.PaperClickEventModel(0, paperInfo.id))
        }
        rippleContainer.setOnLongClickListener {
            popMenu(paperInfo)
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

    private fun popMenu(paperInfo: PaperInfo) {
        val menuLayoutId = R.layout.paper_item_menu_layout
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
        menuView.findViewById<View>(R.id.edit_paper)?.setOnClickListener {
            LiveEventBus.get(EventKey.PAPER_MENU_EDIT_PAPER, PaperInfo::class.java)
                .post(paperInfo)
            popupWindow.dismiss()
        }
        menuView.findViewById<View>(R.id.delete_paper)?.setOnClickListener {
            LiveEventBus.get(EventKey.PAPER_MENU_DELETE_PAPER, PaperInfo::class.java)
                .post(paperInfo)
            popupWindow.dismiss()
        }
        menuView.findViewById<View>(R.id.sortByHand)?.visibility = View.GONE
    }
}