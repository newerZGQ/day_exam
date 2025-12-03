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
import com.gorden.dayexam.databinding.FragmentHomeLayoutBinding
import com.gorden.dayexam.db.entity.StudyRecord

import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeLayoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var questionPager: ViewPager2
    private var questions: List<QuestionDetail> = listOf()
    private var paperId = 0
    private var curQuestionId = 0
    private var curQuestionCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeLayoutBinding.inflate(inflater, container, false)
        initView()
        registerActionEvent()
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        registerRememberMode()
        return binding.root
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
                DataRepository.updatePaperStatus(paperId, position)
            }
        }
    }

    private fun initView() {
        questionPager = binding.questionPager
        questionPager.adapter = QuestionPagerAdapter()
        questionPager.registerOnPageChangeCallback(onPageChangeCallback)
        // TODO 这里调整问题的滑动方式
        questionPager.orientation = ORIENTATION_HORIZONTAL
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                        it.answer,
                        it.correct
                    )
                )
            }
    }

    private fun registerRememberMode() {
        DataRepository.getConfig().observe(viewLifecycleOwner, { config ->
            config?.let {
                (questionPager.adapter as QuestionPagerAdapter).setRememberMode(it.rememberMode)
            }
        })
    }

}