package com.gorden.dayexam.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.StudyRecord
import com.gorden.dayexam.parser.image.ImageCacheManager
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.QuestionWithElement
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var questionPager: ViewPager2
    private var questions: List<QuestionWithElement> = listOf()
    private var paperId = 0
    private var curQuestionId = 0
    private var curQuestionCount = 0

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home_layout, container, false)
        initView(root)
        registerActionEvent()
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        homeViewModel.currentQuestionDetail().observe(viewLifecycleOwner, {
            ImageCacheManager.setCacheFolder(it.bookId.toString())
            if (paperId != it.paperId || it.curQuestionId != curQuestionId || it.questions.size != curQuestionCount) {
                paperId = it.paperId
                curQuestionId = it.curQuestionId
                questions = it.questions
                (questionPager.adapter as QuestionPagerAdapter).setData(it.questions, it.bookTitle, it.paperTitle)
                val curPosition = getCurPosition(it.curQuestionId, it.questions)
                questionPager.setCurrentItem(curPosition, false)
            }
        })
        registerRememberMode()
        return root
    }

    fun currentPosition(): Int {
        return questionPager.currentItem
    }

    fun setCurrentPosition(position: Int) {
        questionPager.currentItem = position
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (position < questions.size) {
                val question = questions[position]
                curQuestionId = question.id
                DataRepository.updatePaperStatus(paperId, question.id)
            }
        }
    }

    private fun initView(root: View) {
        questionPager = root.findViewById(R.id.questionPager)
        questionPager.adapter = QuestionPagerAdapter()
        questionPager.registerOnPageChangeCallback(onPageChangeCallback)
        // TODO 这里调整问题的滑动方式
        questionPager.orientation = ORIENTATION_HORIZONTAL
    }

    private fun registerActionEvent() {
        LiveEventBus
            .get(EventKey.QUESTION_IMAGE_CLICKED, EventKey.ImagePreviewEventModel::class.java)
            .observe(this) { previewModel ->
                val intent = Intent(context, ImagePreviewActivity::class.java)
                intent.putStringArrayListExtra(
                    ImagePreviewActivity.IMAGE_LIST_DATA_KEY,
                    previewModel.imageUrls
                )
                intent.putExtra(ImagePreviewActivity.IMAGE_POSITION_KEY, previewModel.target)
                startActivity(intent)
            }
        LiveEventBus.get(EventKey.ANSWER_EVENT, EventKey.AnswerEventModel::class.java)
            .observe(this) {
                DataRepository.insertStudyRecord(
                    StudyRecord(
                        paperId,
                        it.questionId,
                        it.answer,
                        it.correct
                    )
                )
            }
    }

    private fun getCurPosition(questionId: Int, questions: List<QuestionWithElement>): Int {
        var result = 0
        questions.forEachIndexed { index, questionWithElement ->
            if (questionId == questionWithElement.id) {
                result = index
            }
        }
        return result
    }

    private fun registerRememberMode() {
        DataRepository.getConfig().observe(viewLifecycleOwner, { config ->
            config?.let {
                (questionPager.adapter as QuestionPagerAdapter).setRememberMode(it.rememberMode)
            }
        })
    }

}