package com.gorden.dayexam.ui.widget

import com.gorden.dayexam.repository.model.question.Element
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus

class ElementViewListener: ElementActionListener {
    override fun onImageClick(target: Int, elements: List<Element>) {
        val targetElement = elements[target]
        val pictureElements = elements.filter {
            it.elementType == Element.PICTURE
        }
        val renderTarget = pictureElements.indexOf(targetElement)
        val imageUrls = arrayListOf<String>()
        pictureElements.forEach {
            imageUrls.add(it.content)
        }
        LiveEventBus.get(EventKey.QUESTION_IMAGE_CLICKED, EventKey.ImagePreviewEventModel::class.java)
            .post(EventKey.ImagePreviewEventModel(imageUrls, renderTarget))
    }
}