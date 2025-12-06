package com.gorden.dayexam.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.model.Element
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.utils.ScreenUtils
import java.io.File

class ElementsView: LinearLayout {

    var textSize = 7f
    var tagTextSize = 7f

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val typeArray = context.obtainStyledAttributes(attrs, R.styleable.element)
        textSize = typeArray.getDimension(R.styleable.element_e_text_size, 7f)
        tagTextSize = typeArray.getDimension(R.styleable.element_tag_text_size, 3f)
    }

    companion object {
        const val ELEMENT_MARGIN_TOP = 0f
    }

    fun setElements(paperInfo: PaperInfo, elements: List<Element>, highlightText: String, listener: ElementActionListener) {
        removeAllViews()
        this.orientation = VERTICAL
        addTagText(highlightText)
        elements.forEachIndexed { index, contentElement ->
            if (contentElement.elementType == Element.TEXT) {
                addCommonText(contentElement.content)
            } else if (contentElement.elementType == Element.PICTURE) {
                val imageView = ImageView(context)
                val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                layoutParams.topMargin = ScreenUtils.dp2px(ELEMENT_MARGIN_TOP)
                addView(imageView, layoutParams)
                imageView.setOnClickListener {
                    listener.onImageClick(index, elements)
                }
                imageView.setBackgroundColor(context.getColor(R.color.colorTransparent))
                val imageFile = File(ContextHolder.application.cacheDir, "/${paperInfo.hash}/image/${contentElement.content}")
                val requestBuilder = Glide.with(context).asBitmap().load(imageFile)
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

fun interface ElementActionListener {
    fun onImageClick(target: Int, elements: List<Element>)
}