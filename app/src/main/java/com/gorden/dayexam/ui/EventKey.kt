package com.gorden.dayexam.ui

class EventKey {
    companion object {
        // 首页问题中的图片被点击
        const val QUESTION_IMAGE_CLICKED = "801"
        // 图片预览页图片被点击
        const val IMAGE_PREVIEW_CLICKED = "802"
        const val START_PROGRESS_BAR = "803"
        const val END_PROGRESS_BAR = "804"

        // 试卷列表页试卷被点击
        const val PAPER_CONTAINER_CLICKED = "3"
        // 试卷列表页，菜单弹窗从文件添加试题被点击
        const val PAPER_MENU_ADD_QUESTION_FROM_FILE = "4"
        // 试卷列表页，菜单弹窗编辑试卷被点击
        const val PAPER_MENU_EDIT_PAPER = "5"
        // 试卷列表页，菜单弹窗删除试卷被点击
        const val PAPER_MENU_DELETE_PAPER = "6"
        // 试卷列表页，创建试卷被点击
        const val CREATE_PAPER_CLICKED = "7"
        // 试卷列表页，删除试卷被点击
        const val DELETE_BOOK_CLICKED = "8"
        // 试卷列表页，删除试卷被点击
        const val EDIT_BOOK_CLICKED = "9"
        // 试卷列表页，菜单弹窗移动试卷被点击
        const val PAPER_MENU_MOVE_PAPER = "10"

        // 课程sheet item点击
        const val COURSE_ITEM_CLICKED = "5"
        // FloatingActionButton点击
        const val REFRESH_QUESTION = "100"
        // FloatingActionButton点击
        const val FAVORITE_QUESTION = "101"
        // 点击回答问题事件
        const val ANSWER_EVENT = "200"
        // 在试题选择页面点击选择试题
        const val SELECT_QUESTION = "300"
        // 在课程选择页面选择某个课程
        const val SELECT_COURSE = "400"

        // short_cut_sheet页面事件
        const val SEARCH_CLICKED = "500"

        // search_sheet事件
        const val SEARCH_RESULT_ITEM_CLICK = "600"

        // edit 事件
        const val EDIT_SELECT_PHOTO_CLICK = "700"
    }

    data class ImagePreviewEventModel(
        val imageUrls: ArrayList<String>,
        val target: Int
    )

    data class PaperClickEventModel(
        val bookId: Int,
        val paperId: Int
    )

    data class QuestionAddEventModel(
        val bookId: Int,
        val paperId: Int
    )

    data class AnswerEventModel(
        val answer: String = "",
        // 0表答错 1表答对 -1表答案生效
        val correct: Int = -1
    )
}