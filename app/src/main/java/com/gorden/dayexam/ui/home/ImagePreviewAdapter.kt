package com.gorden.dayexam.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gorden.dayexam.R
import com.gorden.dayexam.utils.ImageCacheHelper
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus

class ImagePreviewAdapter: RecyclerView.Adapter<ImagePreviewHolder>() {
// ... (omitted lines)
class ImagePreviewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    fun setData(imageUrl: String) {
        val previewImage = itemView.findViewById<ImageView>(R.id.previewImage)
        previewImage.setOnClickListener {
            LiveEventBus.get<String>(EventKey.IMAGE_PREVIEW_CLICKED)
                .post(null)
        }
        var requestBuilder = Glide.with(itemView.context).asBitmap()
        requestBuilder = if (imageUrl.startsWith("default_data_image")) {
            requestBuilder.load("file:///android_asset/image/" + imageUrl)
        } else {
            val imageFile = ImageCacheHelper.getImageFile(imageUrl)
            requestBuilder.load(imageFile)
        }
        requestBuilder.into(previewImage)
    }
}