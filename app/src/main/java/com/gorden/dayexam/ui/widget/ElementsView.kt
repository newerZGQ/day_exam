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
import com.gorden.dayexam.repository.model.Element
import com.gorden.dayexam.utils.ImageCacheHelper
import com.gorden.dayexam.utils.ScreenUtils

class ElementsView: LinearLayout {
// ... (omitted lines)
                    var requestBuilder = Glide.with(context).asBitmap()
                    requestBuilder = if (contentElement.content.startsWith("default_data_image")) {
                        requestBuilder.load("file:///android_asset/image/" + contentElement.content)
                    } else {
                        val imageFile = ImageCacheHelper.getImageFile(contentElement.content)
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