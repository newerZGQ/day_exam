package com.gorden.dayexam.ui

import com.gorden.dayexam.db.entity.PaperInfo

class EventKey {
    companion object {
        // 首页问题中的图片被点击
        const val QUESTION_IMAGE_CLICKED = "801"
        // 图片预览页图片被点击
        const val IMAGE_PREVIEW_CLICKED = "802"

        // 试卷列表页试卷被点击
        const val PAPER_CONTAINER_CLICKED = "3"

        // FloatingActionButton点击
        const val FAVORITE_QUESTION = "101"
        // 点击回答问题事件
        const val ANSWER_EVENT = "200"
        // 切换常亮屏幕
        const val KEEP_SCREEN_ON = "300"
        // 左右点击切换题目
        const val NAVIGATE_QUESTION = "803"
        // 在试题选择页面点击选择试题
        const val SELECT_QUESTION = "300"

        // short_cut_sheet页面事件
        const val SEARCH_CLICKED = "500"
        // 退出学习，首页展示欢迎页
        const val EXIT_STUDY = "501"

        // search_sheet事件
        const val SEARCH_RESULT_ITEM_CLICK = "600"
    }

    data class ImagePreviewEventModel(
        val imageUrls: ArrayList<String>,
        val target: Int
    )

    data class PaperClickEventModel(
        val paperInfo: PaperInfo
    )

    data class AnswerEventModel(
        // 0表答错 1表答对 -1表答案生效
        val correct: Int = -1
    )
}