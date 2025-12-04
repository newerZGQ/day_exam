package com.gorden.dayexam.ui.paper

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus

class PaperViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val container: View = itemView.findViewById(R.id.paper_item_container)
    private val rippleContainer: View = itemView.findViewById(R.id.paper_ripple_item_container)
    private val title: TextView = itemView.findViewById(R.id.PaperTitle)
    private val record: TextView = itemView.findViewById(R.id.studyRecord)
    private val deleteButton: ImageButton = itemView.findViewById(R.id.paper_delete_button)

    fun setData(
        paperInfo: PaperInfo,
        curPaperId: Int,
        isEditMode: Boolean,
        onClick: (PaperInfo) -> Unit,
        onLongPress: (PaperViewHolder, PaperInfo) -> Unit,
        onDeleteClick: (PaperInfo) -> Unit
    ) {
        val resources = itemView.context.resources

        title.text = paperInfo.title
        record.text = resources.getString(R.string.paper_question_count, paperInfo.questionCount)

        // 选中的试卷标题显示为绿色
        if (paperInfo.id == curPaperId) {
            container.setBackgroundColor(resources.getColor(R.color.colorPrimaryDark))
            title.setTextColor(resources.getColor(android.R.color.holo_green_light))
        } else {
            container.setBackgroundColor(resources.getColor(R.color.colorPrimary))
            title.setTextColor(resources.getColor(R.color.font_common_color))
        }

        // 点击进入试卷 - 通过回调传递给 Fragment
        rippleContainer.setOnClickListener {
            onClick(paperInfo)
        }

        // 长按：进入编辑模式并触发拖拽（由外部回调处理）
        rippleContainer.setOnLongClickListener {
            onLongPress(this, paperInfo)
            true
        }

        // 编辑模式下显示删除按钮
        deleteButton.visibility = if (isEditMode) View.VISIBLE else View.GONE
        deleteButton.setOnClickListener {
            onDeleteClick(paperInfo)
        }
    }
}