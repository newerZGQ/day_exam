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
import com.gorden.dayexam.db.entity.question.Element
import com.gorden.dayexam.executor.AppExecutors
import com.gorden.dayexam.parser.image.ImageCacheManager
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.utils.BookUtils
import java.io.ByteArrayOutputStream
import java.io.IOException


class EditElementsDialog(
    context: Context
): Dialog(context) {

    private var elements: List<Element> = listOf()
    private var contentId = -1

    private lateinit var rootView: ConstraintLayout
    private lateinit var elementList: RecyclerView
    private lateinit var adapter: ElementAdapter

    constructor(
        context: Context,
        contentId: Int,
        elements: List<Element>
    ): this(context) {
        this.elements = elements
        this.contentId = contentId
    }

    private fun init() {
        setContentView(R.layout.dialog_edit_elements_layout)
        window?.setBackgroundDrawableResource(R.color.colorTransparent)
        window?.setDimAmount(0.8f)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        rootView = findViewById(R.id.edit_elements_container)
        findViewById<View>(R.id.done_edit_content).setOnClickListener {
            saveAllElement()
            dismiss()
        }
        elementList = findViewById(R.id.element_list)
        findViewById<View>(R.id.cancel_edit_content).setOnClickListener {
            dismiss()
        }
        adapter = ElementAdapter()
        elementList.adapter = adapter
        elementList.layoutManager = LinearLayoutManager(context)
        adapter.setData(contentId, elements)
    }

    private fun saveAllElement() {
        AppExecutors.diskIO().execute {
            var index = 0
            val result = adapter.getData().filter {
                // 删除被操作delete的图片文件
                if (it.element.elementType == Element.PICTURE && it.isDeleted) {
                    ImageCacheManager.delete(it.element.content)
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
                                ImageCacheManager.save(fileName, byteArray)
                                Element(
                                    it.element.elementType,
                                    fileName,
                                    it.element.parentId,
                                    index
                                )
                            } catch (e: IOException) {

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