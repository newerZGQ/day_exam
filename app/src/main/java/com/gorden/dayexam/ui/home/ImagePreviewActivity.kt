package com.gorden.dayexam.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.gorden.dayexam.databinding.ActivityImagePreviewLayoutBinding
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus

class ImagePreviewActivity: AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewLayoutBinding

    companion object {
        const val IMAGE_LIST_DATA_KEY = "image_list"
        const val IMAGE_POSITION_KEY = "position"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        binding.imageList.adapter = ImagePreviewAdapter()
        binding.imageList.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        setImageList()
        LiveEventBus
            .get(EventKey.IMAGE_PREVIEW_CLICKED, String::class.java)
            .observe(this, {
                finish()
            })
    }

    private fun setImageList() {
        val imageList = intent.getStringArrayListExtra(IMAGE_LIST_DATA_KEY)
        binding.imageList.let {
            if (imageList?.isNotEmpty() == true) {
                (it.adapter as ImagePreviewAdapter).setData(imageList)
            }
        }
        val position = intent.getIntExtra(IMAGE_POSITION_KEY, 0)
        binding.imageList.setCurrentItem(position, false)

    }
}