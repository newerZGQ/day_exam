package com.gorden.dayexam.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.question.Element
import com.gorden.dayexam.parser.image.ImageCacheManager
import com.gorden.dayexam.utils.ScreenUtils

class ElementsView: LinearLayout {

    var textSize = 7f
    var tagTextSize = 7f

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.element)
        textSize = typeArray.getDimension(R.styleable.element_e_text_size, 7f)
        tagTextSize = typeArray.getDimension(R.styleable.element_tag_text_size, 3f)
    }

    private var listener: ElementActionListener? = null

    companion object {
        const val ELEMENT_MARGIN_TOP = 0f
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setElements(elements: List<Element>, highlightText: String, listener: ElementActionListener) {
        removeAllViews()
        this.orientation = VERTICAL
        elements.forEachIndexed { index, contentElement ->
            if (index == 0 && contentElement.elementType == Element.TEXT) {
                addTagText(highlightText)
                addCommonText(contentElement.content)
            } else {
                if (contentElement.elementType == Element.TEXT) {
                    addCommonText(contentElement.content)
                } else if (contentElement.elementType == Element.PICTURE) {
                    val imageView = ImageView(context)
                    val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    layoutParams.topMargin = ScreenUtils.dp2px(ELEMENT_MARGIN_TOP)
                    addView(imageView, layoutParams)
                    imageView.setOnClickListener {
                        listener?.onImageClick(index, elements)
                    }
                    imageView.setBackgroundColor(context.getColor(R.color.colorTransparent))

                    var requestBuilder = Glide.with(context).asBitmap()
                    requestBuilder = if (contentElement.content.startsWith("default_data_image")) {
                        requestBuilder.load("file:///android_asset/image/" + contentElement.content)
                    } else {
                        val imageFile = ImageCacheManager.getImageFile(contentElement.content)
                        requestBuilder.load(imageFile)
                    }
                    requestBuilder
                        .into(object : CustomTarget<Bitmap>(){
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                // 这里返回的是像素值
                                this@ElementsView.post {
                                    val width = this@ElementsView.measuredWidth
                                    val height = width * resource.height / resource.width
                                    val layoutParams = imageView.layoutParams
                                    layoutParams.width = width
                                    layoutParams.height = height
                                    imageView.layoutParams = layoutParams
                                    imageView.setImageBitmap(resource)
                                }
                            }
                            override fun onLoadCleared(placeholder: Drawable?) {
                                // this is called when imageView is cleared on lifecycle call or for
                                // some other reason.
                                // if you are referencing the bitmap somewhere else too other than this imageView
                                // clear it here as you can no longer have the bitmap
                            }
                        })
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addTagText(text: String) {
        if (text.isEmpty()) {
            return
        }
        val tagTv = TextView(context)
        tagTv.text = text
        tagTv.background = ResourcesCompat.getDrawable(context.resources,
            R.drawable.question_type_background, null)
        tagTv.typeface = resources.getFont(R.font.xwwk)
        tagTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, tagTextSize)
        val tagLayoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        tagTv.setPadding(20, 3, 20, 3)
        addView(tagTv, tagLayoutParams)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addCommonText(text: String) {
        if (text.isEmpty()) {
            return
        }
        val contentTv = TextView(context)
        contentTv.text = text
        contentTv.typeface = resources.getFont(R.font.xwwk)
        contentTv.setTextColor(context.getColor(R.color.font_common_color))
        contentTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(contentTv, layoutParams)
    }
}

interface ElementActionListener {
    fun onImageClick(target: Int, elements: List<Element>)
}