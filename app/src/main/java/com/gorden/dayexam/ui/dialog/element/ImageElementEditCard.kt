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
import com.gorden.dayexam.utils.ImageCacheHelper
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus


class ImageElementEditCard: ElementEditCard {
// ... (omitted lines)
    private fun loadCurImage() {
        var requestBuilder = Glide.with(context).asBitmap()
        val imageUrl = editableElement.newContent!!
        requestBuilder = if (imageUrl.startsWith("default_data_image")) {
            requestBuilder.load("file:///android_asset/image/$imageUrl")
        } else {
            val imageFile = ImageCacheHelper.getImageFile(imageUrl)
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