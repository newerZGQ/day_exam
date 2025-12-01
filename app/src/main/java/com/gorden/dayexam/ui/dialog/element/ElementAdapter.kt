package com.gorden.dayexam.ui.dialog.element

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.model.Element
import com.gorden.dayexam.ui.book.DragCallback

class ElementAdapter: RecyclerView.Adapter<EditElementViewHolder>(),
    DragCallback.OnItemTouchListener  {

    private var data = mutableListOf<EditableElement>()
    private var contentId = -1
    var isEditing = false
    private var currentActionPosition = -1

    private val payLoadHide = "hide"
    private val payLoadShow = "show"
    private val toEditMode = "editMode"
    private val toDeleteMode = "deleteMode"
    private val toResetMode = "resetMode"

    override fun getItemViewType(position: Int): Int {
        val element = this.data[position].element
        return element?.elementType ?: Element.TEXT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditElementViewHolder {
        return if (viewType == Element.TEXT) {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.dialog_edit_text_element_item, parent, false)
            EditElementViewHolder(itemView)
        } else {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.dialog_edit_image_element_item, parent, false)
            EditElementViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: EditElementViewHolder, position: Int) {
        val context = holder.itemView.context
        val resources = context.resources
        holder.editCard.setElement(data[position], this, object : EditActionListener {
            override fun onStartEdit() {
                isEditing = true
            }
        })
        if (currentActionPosition == position) {
            holder.editCard.showAction()
        } else {
            holder.editCard.hideAction()
        }
        holder.editCard.setOnClickListener {
            if (isEditing) {
                Toast.makeText(holder.editCard.context,
                    resources.getString(R.string.current_other_question_editing),
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (position == currentActionPosition) {
                holder.editCard.hideAction()
                currentActionPosition = -1
            } else {
                val oldActionPosition = currentActionPosition
                currentActionPosition = position
                notifyItemChanged(oldActionPosition, payLoadHide)
                notifyItemChanged(currentActionPosition, payLoadShow)
            }
        }
    }

    override fun onBindViewHolder(
        holder: EditElementViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            when (payloads[0].toString()) {
                payLoadHide -> {
                    holder.editCard.hideAction()
                }
                payLoadShow -> {
                    holder.editCard.showAction()
                }
                toEditMode -> {
                    holder.editCard.toEditMode()
                }
                toDeleteMode -> {
                    holder.editCard.toDeleteMode()
                }
                toResetMode -> {
                    holder.editCard.toResetMode()
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onMove(fromPosition: Int, toPosition: Int) {

    }

    override fun onSwiped(position: Int) {

    }

    override fun clearView() {

    }

    fun getData(): List<EditableElement> {
        return data
    }

    fun setData(contentId: Int, elements: List<Element>) {
        this.contentId = contentId
        val editableElements = elements.map {
            EditableElement(it, false)
        }
        this.data.clear()
        this.data.addAll(editableElements)
        notifyDataSetChanged()
    }

    fun insertTextElementAfterCurrentPosition() {
        createEditableElement(Element.TEXT)?.let {
            this.data.add(currentActionPosition + 1, it)
            notifyItemInserted(currentActionPosition + 1)
            notifyItemRangeChanged(currentActionPosition + 1, this.data.size)
        }
    }

    fun insertImageElementAfterCurrentPosition() {
        createEditableElement(Element.PICTURE)?.let {
            this.data.add(currentActionPosition + 1, it)
            notifyItemInserted(currentActionPosition + 1)
            notifyItemRangeChanged(currentActionPosition + 1, this.data.size)
        }
    }

    fun currentItemEditMode() {
        notifyItemChanged(currentActionPosition, toEditMode)
    }

    fun currentItemDeleteMode() {
        notifyItemChanged(currentActionPosition, toDeleteMode)
    }

    fun currentItemResetMode() {
        notifyItemChanged(currentActionPosition, toResetMode)
    }

    private fun createEditableElement(elementType: Int): EditableElement? {
        if (currentActionPosition == -1) {
            return null
        }
        val currentElement = this.data[currentActionPosition].element ?: return null
        val editableElement = EditableElement(
            Element(
                elementType,
                "",
                currentElement.parentId,
                currentActionPosition + 1),
            true
        )
        editableElement.hasEdited = false
        editableElement.newContent = ""
        return editableElement
    }
}

data class EditableElement (
    var element: Element,
    val isNewAdd: Boolean) {
    var newContent: String? = element?.content
    var image: Bitmap? = null
    var hasEdited: Boolean = false
    var isDeleted: Boolean = false
}