package com.gorden.dayexam.ui.sheet.shortcut

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gorden.dayexam.Constants.SP_HOME_SHOW_WELCOME
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.Config
import com.gorden.dayexam.databinding.ShortCutSheetLayoutBinding
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.utils.SharedPreferenceUtil
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.action.ScreenShotHomeQuestionAction
import com.gorden.dayexam.ui.settings.SettingsActivity
import com.jeremyliao.liveeventbus.LiveEventBus

class ShortCutSheetDialog : BottomSheetDialogFragment() {

    private var _binding: ShortCutSheetLayoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShortCutSheetLayoutBinding.inflate(inflater, container, false)
        rootView = binding.root
        val viewModel = ViewModelProvider(this).get(ShortCutViewModel::class.java)
        viewModel.getConfig().observe(this) {
            it?.let {
                setStudyMode(it)
                setStudyModeListener()
            }
        }
        initCopyQuestion()
        initSearchAction()
        initKeepScreenSwitch()
        initExitStudy()
        return rootView
    }

    private fun setStudyMode(config: Config) {
        binding.rememberModeSwitch.isChecked = config.rememberMode
    }

    private fun setStudyModeListener() {
        binding.rememberModeSwitch.setOnCheckedChangeListener { _, b ->
            DataRepository.updateRememberMode(b)
        }
        binding.rememberContentContainer.setOnClickListener {
            binding.rememberModeSwitch.isChecked = !binding.rememberModeSwitch.isChecked
        }
    }

    private fun initCopyQuestion() {
        binding.copyContentContainer.setOnClickListener {
            ScreenShotHomeQuestionAction(requireActivity()).start()
            dismiss()
        }
    }

    private fun initSearchAction() {
        binding.searchContentContainer.setOnClickListener {
            LiveEventBus.get(EventKey.SEARCH_CLICKED, Int::class.java)
                // 0没有意义
                .post(0)
            dismiss()
        }
        binding.toSetting.setOnClickListener {
            val intent = Intent(requireActivity(), SettingsActivity::class.java)
            startActivity(intent)
            dismiss()
        }
    }

    private fun initKeepScreenSwitch() {
        val key = requireContext().resources.getString(R.string.keep_screen_light_key)
        val opened = SharedPreferenceUtil.getBoolean(key, false)
        binding.keepScreenLightSwitch.isChecked = opened
        binding.keepScreenLightSwitch.setOnCheckedChangeListener { _, b ->
            SharedPreferenceUtil.setBoolean(key, b)
        }
        binding.keepScreenContentContainer.setOnClickListener {
            binding.keepScreenLightSwitch.isChecked = !binding.keepScreenLightSwitch.isChecked
        }
    }

    private fun initExitStudy() {
        binding.exitStudyContainer.setOnClickListener {
            // 设置偏好，主页展示欢迎页
            SharedPreferenceUtil.setBoolean(SP_HOME_SHOW_WELCOME, true)
            // 清除当前试卷，使 HomeFragment 能响应显示欢迎页
            DataRepository.updateCurPaperId(-1)
            // 通知 HomeFragment 切换视图
            LiveEventBus.get(EventKey.EXIT_STUDY, Boolean::class.java).post(true)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}