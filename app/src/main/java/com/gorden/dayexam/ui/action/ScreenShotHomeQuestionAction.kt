package com.gorden.dayexam.ui.action

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.MediaStore
import android.util.Log
import android.widget.ScrollView
import android.widget.Toast
import com.gorden.dayexam.MainActivity
import com.gorden.dayexam.R
import java.io.ByteArrayOutputStream
import java.util.*


class ScreenShotHomeQuestionAction(val context: Context): Action {
    override fun start() {
        if (context is MainActivity) {
            val decorView = context.window.decorView
            val scrollView = decorView.findViewById<ScrollView>(R.id.question_content_scrollview)
            val bitMap = getScrollViewBitmap(scrollView)
            if (bitMap != null) {
                saveImage(context, bitMap)
            }
            Toast.makeText(
                context,
                context.resources.getString(R.string.screen_shot_question_success),
                Toast.LENGTH_SHORT
            ).
            show()
            return
        }
        Toast.makeText(
            context,
            context.resources.getString(R.string.screen_shot_question_failed),
            Toast.LENGTH_SHORT
        ).
        show()
    }

    private fun saveImage(context: Context, image: Bitmap) {
        val bytes = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        MediaStore.Images.Media.insertImage(
            context.contentResolver,
            image,
            "dayexam" + System.currentTimeMillis(),
            null
        )
    }

    /**
     * 截取scrollview的屏幕
     */
    fun getScrollViewBitmap(scrollView: ScrollView): Bitmap? {
        var h = 0
        // 获取listView实际高度
        for (i in 0 until scrollView.childCount) {
            h += scrollView.getChildAt(i).height
        }
        Log.d("TAG", "实际高度:$h")
        Log.d("TAG", " 高度:" + scrollView.height)
        // 创建对应大小的bitmap
        val bitmap: Bitmap = Bitmap.createBitmap(
            scrollView.width, h,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        scrollView.draw(canvas)
        return bitmap
    }
}