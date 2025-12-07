package com.gorden.dayexam.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.gorden.dayexam.ContextHolder
import com.gorden.dayexam.databinding.ActivityImagePreviewLayoutBinding
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.PaperDetailCache
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus
import java.io.File

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
            .observe(this) {
                finish()
            }
    }

    private fun setImageList() {
        val paperHash = PaperDetailCache.get(DataRepository.getCurPaperId().value ?: -1)?.paperInfo?.hash
        val imageList = intent.getStringArrayListExtra(IMAGE_LIST_DATA_KEY)?.map {
            File(ContextHolder.application.cacheDir, "/${paperHash}/image/${it}").absolutePath
        }
        binding.imageList.let {
            if (imageList?.isNotEmpty() == true) {
                (it.adapter as ImagePreviewAdapter).setData(imageList)
            }
        }
        val position = intent.getIntExtra(IMAGE_POSITION_KEY, 0)
        binding.imageList.setCurrentItem(position, false)

    }
}