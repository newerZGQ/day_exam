package com.gorden.dayexam.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import com.gorden.dayexam.R
import com.gorden.dayexam.databinding.FragmentHomeLayoutBinding
import com.gorden.dayexam.db.entity.PaperInfo
import com.gorden.dayexam.db.entity.StudyRecord
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.ui.EventKey
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeLayoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var questionPager: ViewPager2
    private var questions: List<QuestionDetail> = listOf()
    private var paperInfo: PaperInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
        registerActionEvent()
        registerRememberMode()
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
        questionPager.orientation = ORIENTATION_HORIZONTAL
    }

    private fun initData() {
        startLoad()
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
        DataRepository.getCurPaperId().observe(viewLifecycleOwner) {
            startLoad()
        }
    }
    
    /**
     * 从 Repository 加载试卷及其问题
     */
    private fun startLoad() {
        viewLifecycleOwner.lifecycleScope.launch {
            kotlin.runCatching {
                val paperDetail = withContext(Dispatchers.IO) {
                    val paperId = DataRepository.getCurPaperId().value ?: return@withContext null
                    DataRepository.getPaperDetailById(paperId)
                }

                if (paperDetail == null) {
                    Toast.makeText(
                        requireContext(),
                        requireContext().getString(R.string.toast_questions_file_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // 更新试题列表与 UI（主线程）
                questions = paperDetail.question
                paperInfo = paperDetail.paperInfo
                (questionPager.adapter as QuestionPagerAdapter).setData(
                    paperDetail.paperInfo,
                    questions
                )
                questionPager.currentItem = paperDetail.paperInfo.lastStudyPosition
            }.onFailure {
                it.printStackTrace()
                Toast.makeText(
                    context,
                    getString(R.string.toast_questions_load_failed, it.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
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