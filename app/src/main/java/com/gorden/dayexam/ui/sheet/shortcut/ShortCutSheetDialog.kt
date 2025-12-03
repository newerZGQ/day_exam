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
import com.gorden.dayexam.R
import com.gorden.dayexam.db.entity.Config
import com.gorden.dayexam.databinding.ShortCutSheetLayoutBinding
import com.gorden.dayexam.repository.DataRepository
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
        viewModel.getConfig().observe(this, {
            it?.let {
                setStudyMode(it)
                setStudyModeListener()
            }
        })
        initCopyQuestion()
        initSearchAction()
        return rootView
    }

    private fun setStudyMode(config: Config) {
        binding.rememberModeSwitch.isChecked = config.rememberMode
        binding.focusModeSwitch.isChecked = config.focusMode
        binding.favoriteModeSwitch.isChecked = config.onlyFavorite
        binding.sortByAccuracyModeSwitch.isChecked = config.sortByAccuracy
    }

    private fun setStudyModeListener() {
        binding.rememberModeSwitch.setOnCheckedChangeListener { _, b ->
            DataRepository.updateRememberMode(b)
        }
        binding.rememberContentContainer.setOnClickListener {
            binding.rememberModeSwitch.isChecked = !binding.rememberModeSwitch.isChecked
        }
        binding.focusModeSwitch.setOnCheckedChangeListener { _, b ->
            DataRepository.updateFocusMode(b)
            dismiss()
        }
        binding.focusContentContainer.setOnClickListener {
            binding.focusModeSwitch.isChecked = !binding.focusModeSwitch.isChecked
        }
        binding.favoriteModeSwitch.setOnCheckedChangeListener { _, b ->
            DataRepository.updateOnlyFavoriteMode(b)
        }
        binding.favoriteContentContainer.setOnClickListener {
            binding.favoriteModeSwitch.isChecked = !binding.favoriteModeSwitch.isChecked
        }
        binding.sortByAccuracyModeSwitch.setOnCheckedChangeListener { _, b ->
            DataRepository.updateSortAccuracyMode(b)
        }
        binding.sortByAccuracyContentContainer.setOnClickListener {
            binding.sortByAccuracyModeSwitch.isChecked = !binding.sortByAccuracyModeSwitch.isChecked
        }
    }

    private fun disableDeleteAction() {
        binding.deleteContentContainer.setOnClickListener(null)
        binding.deleteTitle
            .setTextColor(requireActivity().resources.getColor(R.color.short_cut_delete_disable_color))
        binding.deleteContentIcon
            .setImageDrawable(requireActivity().resources.getDrawable(R.drawable.ic_baseline_delete_outline_half_transparent_24))
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
        binding.searchIcon.setOnClickListener {
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

    private fun initHistoryAction() {
        binding.historyContentContainer.setOnClickListener {

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}