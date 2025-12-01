package com.gorden.dayexam.ui.dialog.element

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gorden.dayexam.R
import com.gorden.dayexam.repository.model.Element
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.utils.ImageCacheHelper
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.utils.BookUtils
import java.io.ByteArrayOutputStream
import java.io.IOException


class EditElementsDialog(
    context: Context
): Dialog(context) {
// ... (omitted lines)
    private fun saveAllElement() {
        AppExecutors.diskIO().execute {
            var index = 0
            val result = adapter.getData().filter {
                // 删除被操作delete的图片文件
                if (it.element.elementType == Element.PICTURE && it.isDeleted) {
                    ImageCacheHelper.delete(it.element.content)
                }
                // 过滤掉需要删除的element
                !it.isDeleted
            }.map {
                when (it.element.elementType) {
                    Element.TEXT -> {
                        Element(
                            it.element.elementType,
                            it.newContent!!,
                            it.element.parentId,
                            ++index
                        )
                    }
                    Element.PICTURE -> {
                        if ((it.hasEdited && it.image != null)) {
                            val fileName = BookUtils.generateImageName(
                                "NewAdd" + ++index,
                                System.currentTimeMillis()
                            )
                            try {
                                val stream = ByteArrayOutputStream()
                                it.image!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                val byteArray: ByteArray = stream.toByteArray()
                                ImageCacheHelper.save(fileName, byteArray)
                                Element(
                                    it.element.elementType,
                                    fileName,
                                    it.element.parentId,
                                    index
                                )
                            } catch (e: IOException) {
// ... (omitted lines)

                            } finally {
                                it.image!!.recycle()
                            }
                        } else {
                            it.element.position = ++index
                            it.element
                        }
                    } else -> {

                    }
                }
            }
            DataRepository.updateElementsByContentId(contentId, result as List<Element>)
            DataRepository.increaseContentVersion()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

}