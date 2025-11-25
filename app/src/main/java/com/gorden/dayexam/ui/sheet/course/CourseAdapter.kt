package com.gorden.dayexam.ui.sheet.course

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.DContext
import com.gorden.dayexam.db.entity.Course
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus

class CourseAdapter: RecyclerView.Adapter<CourseViewHolder>() {

    private var data = mutableListOf<Course>()
    private var dContext: DContext? = null

    private var highLightCourseId: Int = -1
    private var highLightPosition: Int = -1

    private var recycleBinId: Int = -1

    private var selected = false
    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.course_sheet_item_layout, parent, false)
        return CourseViewHolder(itemView)
    }

    @SuppressLint("ResourceAsColor", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: CourseViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val course = data[position]
        // 标题和描述
        holder.title.text = course.title
        if (course.description.isEmpty()) {
            holder.desc.visibility = View.GONE
        } else {
            holder.desc.visibility = View.VISIBLE
            holder.desc.text = course.description
        }

        if (course.id == recycleBinId) {
            holder.icon.setImageResource(R.drawable.course_recycler_icon)
        } else {
            holder.icon.setImageResource(R.drawable.course_icon)
        }
        val resources = holder.itemView.context.resources
        val commonColor = resources.getColor(R.color.font_common_color)
        val selectedColor = resources.getColor(R.color.font_selected_color)
        if (highLightPosition == position) {
            holder.title.setTextColor(selectedColor)
            holder.desc.setTextColor(selectedColor)
        } else {
            holder.title.setTextColor(commonColor)
            holder.desc.setTextColor(commonColor)
        }
        holder.itemView.setOnClickListener {
            if (selected) {
                if (course.id == recycleBinId) {
                    Toast.makeText(ContextHolder.currentActivity(), "废纸篓无法删除", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (selectedPosition != position) {
                    val oldSelectedPosition = selectedPosition
                    selectedPosition = position
                    postSelectEvent()
                    notifyItemChanged(oldSelectedPosition)
                    notifyItemChanged(position)
                }
                return@setOnClickListener
            }
            highLightCourseId = this.data[position].id
            // 更新context
            LiveEventBus.get(EventKey.COURSE_ITEM_CLICKED, Int::class.java).post(highLightCourseId)
        }
        if (selected && selectedPosition != -1 && selectedPosition == position) {
            holder.itemView.setBackgroundColor(resources.getColor(R.color.course_selected_background, null))
        } else {
            holder.itemView.setBackgroundColor(resources.getColor(R.color.colorPrimary, null))
        }
        holder.itemView.setOnLongClickListener {
            if (selected) {
                return@setOnLongClickListener true
            }
            if (course.id == recycleBinId) {
                Toast.makeText(ContextHolder.currentActivity(), "废纸篓无法删除", Toast.LENGTH_SHORT).show()
                return@setOnLongClickListener true
            }
            holder.itemView.setBackgroundColor(resources.getColor(R.color.course_selected_background, null))
            selected = true
            selectedPosition = position
            postSelectEvent()
            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        // 多渲染一个空item
        return data.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(courses: List<Course>) {
        this.data.clear()
        this.data.addAll(courses)
//        this.data.add(generateRecycleItem())
        highLightPosition = this.data.indexOfFirst {
            it.id == highLightCourseId
        }
        selected = false
        selectedPosition = -1
        this.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun cancelSelectMode() {
        selected = false
        selectedPosition = -1
        this.notifyDataSetChanged()
    }

    fun setDContext(dContext: DContext) {
        this.dContext = dContext
        this.highLightCourseId = this.dContext?.curCourseId!!
        this.recycleBinId = this.dContext?.recycleBinId!!
    }

    private fun postSelectEvent() {
        LiveEventBus.get(EventKey.SELECT_COURSE, Course::class.java)
            .post(data[selectedPosition])
    }

//    private fun generateRecycleItem(): Course {
//        val course = Course("废纸篓", "找回您删除的试题", 0)
//        course.id = RECYCLE_BIN_ID
//        return course
//    }

}