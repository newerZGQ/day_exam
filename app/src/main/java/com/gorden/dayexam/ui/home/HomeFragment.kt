package com.gorden.dayexam.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import com.google.gson.reflect.TypeToken
import com.gorden.dayexam.R
import com.gorden.dayexam.databinding.FragmentHomeLayoutBinding
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.db.entity.StudyRecord

import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus
import java.io.File

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeLayoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var questionPager: ViewPager2
    private var questions: List<QuestionDetail> = listOf()
    private var paperInfo: PaperInfo? = null

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
            paperInfo?.let {
                if (position < questions.size) {
                    DataRepository.updatePaperStatus(it.id, position)
                }
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
                paperInfo?.let { paperInfo ->
                    DataRepository.insertStudyRecord(
                        StudyRecord(
                            paperInfo.id,
                            it.answer,
                            it.correct
                        )
                    )
                }
            }
        
        // 监听试卷点击事件，从 JSON 文件加载试题
        LiveEventBus.get(EventKey.PAPER_CONTAINER_CLICKED, EventKey.PaperClickEventModel::class.java)
            .observe(this) { event ->
                loadQuestionsFromJson(event.paperInfo)
            }
    }
    
    /**
     * 从 JSON 文件加载试题
     */
    private fun loadQuestionsFromJson(paperInfo: PaperInfo) {
        try {
            val context = requireContext()
            val questionsFile = File(context.cacheDir, "${paperInfo.hash}/questions.json")
            
            if (!questionsFile.exists()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_questions_file_not_found),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            
            val json = questionsFile.readText()
            val gson = com.google.gson.Gson()
            val questionList: List<QuestionDetail> = gson.fromJson(
                json,
                object : TypeToken<List<QuestionDetail>>() {}.type
            )
            
            // 更新试题列表
            questions = questionList
            
            // 更新适配器 - 传入试卷标题作为 bookTitle 和 paperTitle
            (questionPager.adapter as QuestionPagerAdapter).setData(
                questions,
                paperInfo.title,  // bookTitle
                paperInfo.title   // paperTitle
            )
            
            // 跳转到上次学习位置
            questionPager.currentItem = paperInfo.lastStudyPosition
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_questions_load_failed, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun registerRememberMode() {
        DataRepository.getConfig().observe(viewLifecycleOwner) { config ->
            config?.let {
                (questionPager.adapter as QuestionPagerAdapter).setRememberMode(it.rememberMode)
            }
        }
    }

}