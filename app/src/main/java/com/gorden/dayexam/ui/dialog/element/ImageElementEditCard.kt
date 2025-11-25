package com.gorden.dayexam.ui.dialog.element

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.gorden.dayexam.R
import com.gorden.dayexam.parser.image.ImageCacheManager
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus


class ImageElementEditCard: ElementEditCard {

    private var imageContent: ImageView

    private var actionContainer: LinearLayout
    private var subActionContainer: LinearLayout
    private var editBtn: ImageButton
    private var resetBtn: ImageButton
    private var deleteBtn: ImageButton

    private var doneBtn: ImageButton
    private var cancelBtn: ImageButton

    private var listener: EditActionListener? = null

    private lateinit var editableElement: EditableElement

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    init {
        LayoutInflater.from(context).inflate(R.layout.image_element_edit_card_layout, this)
        imageContent = findViewById(R.id.image_element_content)

        actionContainer = findViewById(R.id.action_container)
        subActionContainer = findViewById(R.id.sub_action_container)
        editBtn = findViewById(R.id.edit_element_btn)
        resetBtn = findViewById(R.id.reset_element_btn)
        deleteBtn = findViewById(R.id.delete_element_btn)

        doneBtn = findViewById(R.id.done_edit_element_btn)
        cancelBtn = findViewById(R.id.cancel_element_edit)
        setAction()
    }

    @SuppressLint("CutPasteId")
    override fun setElement(
        editableElement: EditableElement,
        adapter: ElementAdapter,
        listener: EditActionListener
    ) {
        this.editableElement = editableElement
        this.listener = listener
        this.adapter = adapter
        imageContent.visibility = View.VISIBLE
        if (editableElement.newContent?.isNotBlank() == true) {
            loadCurImage()
        }
    }

    override fun setAction() {
        super.setAction()
        editBtn.setOnClickListener {
            LiveEventBus.get(EventKey.EDIT_SELECT_PHOTO_CLICK, PhotoSelectCallback::class.java)
                .post(object : PhotoSelectCallback {
                    override fun onSelect(image: Bitmap) {
                        editableElement.image = image
                        editableElement.hasEdited = true
                        adapter.currentItemEditMode()
                    }
                })
        }
        resetBtn.setOnClickListener {
            editableElement.newContent = editableElement.element?.content
            editableElement.image = null
            editableElement.hasEdited = false
            editableElement.isDeleted = false
            adapter.currentItemResetMode()
        }
        deleteBtn.setOnClickListener {
            editableElement.isDeleted = true
            editableElement.newContent = ""
            editableElement.image = null
            adapter.currentItemDeleteMode()
        }
        doneBtn.setOnClickListener {

        }
        cancelBtn.setOnClickListener {

        }
    }

    override fun hideAction() {
        actionContainer.visibility = GONE
    }

    override fun showAction() {
        actionContainer.visibility = VISIBLE
    }

    override fun toEditMode() {
        this.editableElement.image?.let { setImageBitmap(it) }
    }

    override fun toDeleteMode() {
        editBtn.visibility = View.GONE
        deleteBtn.visibility = View.GONE
        loadDefaultImage()
    }

    override fun toResetMode() {
        editBtn.visibility = View.VISIBLE
        deleteBtn.visibility = View.VISIBLE
        if (this.editableElement.newContent?.isEmpty() == true) {
            loadDefaultImage()
        } else {
            loadCurImage()
        }
    }

    private fun loadDefaultImage() {
        imageContent.scaleType = ImageView.ScaleType.CENTER
        val width = imageContent.measuredWidth
        val layoutParams = imageContent.layoutParams
        layoutParams.width = width
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        imageContent.scaleType = ImageView.ScaleType.CENTER
        imageContent.layoutParams = layoutParams
        imageContent.setImageDrawable(context.getDrawable(R.drawable.edit_element_default_picture))
    }

    private fun loadCurImage() {
        var requestBuilder = Glide.with(context).asBitmap()
        val imageUrl = editableElement.newContent!!
        requestBuilder = if (imageUrl.startsWith("default_data_image")) {
            requestBuilder.load("file:///android_asset/image/$imageUrl")
        } else {
            val imageFile = ImageCacheManager.getImageFile(imageUrl)
            requestBuilder.load(imageFile)
        }
        requestBuilder
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // 这里返回的是像素值
                    imageContent.post {
                        setImageBitmap(resource)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }
            })
    }

    private fun setImageBitmap(bitmap: Bitmap) {
        val width = imageContent.measuredWidth
        val height = width * bitmap.height / bitmap.width
        val layoutParams = imageContent.layoutParams
        layoutParams.width = width
        layoutParams.height = height
        imageContent.scaleType = ImageView.ScaleType.FIT_XY
        imageContent.layoutParams = layoutParams
        imageContent.setImageBitmap(bitmap)
    }

    interface PhotoSelectCallback {
        fun onSelect(image: Bitmap)
    }

}