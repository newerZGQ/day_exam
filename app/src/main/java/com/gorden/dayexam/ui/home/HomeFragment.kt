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
import com.gorden.dayexam.repository.PaperDetailCache
import com.gorden.dayexam.repository.model.QuestionDetail
import com.gorden.dayexam.repository.model.QuestionType
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.utils.SharedPreferenceUtil
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeLayoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var questionPager: ViewPager2
    private var originalQuestions: List<QuestionDetail> = listOf()
    private var displayQuestions: List<QuestionDetail> = listOf()
    private var paperInfo: PaperInfo? = null
    private var sortByType = false

    companion object {
        private val TYPE_GROUP_ORDER = listOf(
            QuestionType.FILL_BLANK,
            QuestionType.TRUE_FALSE,
            QuestionType.SINGLE_CHOICE,
            QuestionType.MULTIPLE_CHOICE,
            QuestionType.ESSAY_QUESTION
        )
    }

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
        sortByType = SharedPreferenceUtil.getBoolean(
            resources.getString(R.string.sort_mode_key), false
        )
        initView()
        initData()
        registerActionEvent()
        registerRememberMode()
        registerSortMode()
    }

    fun setCurrentPosition(position: Int) {
        questionPager.setCurrentItem(position, false)
    }

    fun currentPosition(): Int {
        return questionPager.currentItem
    }

    fun currentOriginalPosition(): Int {
        val displayPos = questionPager.currentItem
        return displayToOriginal(displayPos)
    }

    fun getDisplayQuestions(): List<QuestionDetail> = displayQuestions

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            paperInfo?.let {
                if (position < displayQuestions.size) {
                    val originalPos = displayToOriginal(position)
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            paperInfo?.lastStudyPosition = originalPos
                            DataRepository.updatePapers(listOfNotNull(paperInfo))
                        }
                    }
                }
            }
        }
    }

    private fun initView() {
        questionPager = binding.questionPager
        questionPager.adapter = QuestionPagerAdapter()
        questionPager.registerOnPageChangeCallback(onPageChangeCallback)
        questionPager.orientation = ORIENTATION_HORIZONTAL

        binding.goToSettingsButton.setOnClickListener {
            val intent = Intent(requireContext(), android.provider.Settings.ACTION_SETTINGS::class.java)
            val settingsIntent = Intent(requireActivity(), com.gorden.dayexam.ui.settings.SettingsActivity::class.java)
            startActivity(settingsIntent)
        }
    }

    private fun showWelcome() {
        binding.questionPager.visibility = View.GONE
        binding.welcomeContainer.visibility = View.VISIBLE
    }

    private fun hideWelcome() {
        binding.welcomeContainer.visibility = View.GONE
        binding.questionPager.visibility = View.VISIBLE
    }

    private fun initData() {
        val showWelcome = (DataRepository.getCurPaperId().value ?: -1) < 0
        if (showWelcome) {
            showWelcome()
        } else {
            startLoad()
        }
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
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            DataRepository.insertStudyRecord(
                                StudyRecord(
                                    paperInfo.id,
                                    it.correct
                                )
                            )
                        }
                    }
                }
            }
        DataRepository.getCurPaperId().observe(viewLifecycleOwner) {
            startLoad()
        }
        LiveEventBus.get(EventKey.EXIT_STUDY, Boolean::class.java)
            .observe(viewLifecycleOwner) { exited ->
                if (exited == true) {
                    showWelcome()
                } else {
                    hideWelcome()
                }
            }
        LiveEventBus.get(EventKey.NAVIGATE_QUESTION, Int::class.java)
            .observe(viewLifecycleOwner) { direction ->
                val current = currentPosition()
                if (direction == -1) {
                    if (current > 0) {
                        questionPager.setCurrentItem(current - 1, true)
                    }
                } else if (direction == 1) {
                    if (current < displayQuestions.size - 1) {
                        questionPager.setCurrentItem(current + 1, true)
                    }
                }
            }
        LiveEventBus.get(EventKey.SEARCH_RESULT_ITEM_CLICK, Int::class.java)
            .observe(viewLifecycleOwner) { originalIndex ->
                val displayPos = originalToDisplay(originalIndex)
                if (displayPos in 0 until displayQuestions.size) {
                    questionPager.setCurrentItem(displayPos, false)

                    paperInfo?.let { paper ->
                        paper.lastStudyPosition = originalIndex
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            DataRepository.updatePapers(listOf(paper))
                        }
                    }
                }
            }
    }

    private fun startLoad() {
        viewLifecycleOwner.lifecycleScope.launch {
            kotlin.runCatching {
                val paperId = DataRepository.getCurPaperId().value ?: -1
                if (paperId < 0) {
                    showWelcome()
                    return@launch
                }
                val paperDetail = withContext(Dispatchers.IO) {
                    DataRepository.getPaperDetailById(paperId)
                }
                if (paperDetail == null) {
                    showWelcome()
                    Toast.makeText(requireContext(), requireContext().getString(R.string.toast_questions_file_not_found), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                PaperDetailCache.put(paperId, paperDetail)
                originalQuestions = paperDetail.question
                paperInfo = paperDetail.paperInfo
                displayQuestions = computeDisplayQuestions()
                (questionPager.adapter as QuestionPagerAdapter).setData(
                    paperDetail.paperInfo,
                    displayQuestions
                )
                val displayPos = originalToDisplay(paperDetail.paperInfo.lastStudyPosition)
                questionPager.setCurrentItem(displayPos, false)
                hideWelcome()
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
        DataRepository.getRememberMode().observe(viewLifecycleOwner) { rememberMode ->
            rememberMode?.let {
                (questionPager.adapter as QuestionPagerAdapter).setRememberMode(rememberMode)
            }
        }
    }

    private fun registerSortMode() {
        LiveEventBus.get(EventKey.SORT_MODE_CHANGED, Boolean::class.java)
            .observe(viewLifecycleOwner) { typeGroup ->
                if (sortByType == typeGroup) return@observe
                sortByType = typeGroup
                val currentOriginalPos = displayToOriginal(questionPager.currentItem)
                displayQuestions = computeDisplayQuestions()
                (questionPager.adapter as QuestionPagerAdapter).setData(
                    paperInfo ?: return@observe,
                    displayQuestions
                )
                val newDisplayPos = originalToDisplay(currentOriginalPos)
                questionPager.setCurrentItem(newDisplayPos, false)
            }
    }

    private fun computeDisplayQuestions(): List<QuestionDetail> {
        if (!sortByType) return originalQuestions
        return originalQuestions
            .withIndex()
            .sortedWith(compareBy({ typeGroupOrder(it.value.type) }, { it.index }))
            .map { it.value }
    }

    private fun typeGroupOrder(type: Int): Int {
        return TYPE_GROUP_ORDER.indexOf(type).let { if (it == -1) Int.MAX_VALUE else it }
    }

    private fun displayToOriginal(displayPos: Int): Int {
        if (!sortByType) return displayPos
        val question = displayQuestions.getOrNull(displayPos) ?: return displayPos
        return originalQuestions.indexOf(question)
    }

    private fun originalToDisplay(originalPos: Int): Int {
        if (!sortByType) return originalPos
        val question = originalQuestions.getOrNull(originalPos) ?: return originalPos
        return displayQuestions.indexOf(question)
    }
}
