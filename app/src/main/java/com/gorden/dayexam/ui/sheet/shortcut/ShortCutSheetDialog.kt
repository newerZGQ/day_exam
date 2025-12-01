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
import com.gorden.dayexam.repository.DataRepository
import com.gorden.dayexam.ui.EventKey
import com.gorden.dayexam.ui.action.ScreenShotHomeQuestionAction
import com.gorden.dayexam.ui.action.DeleteCurrentQuestionAction
import com.gorden.dayexam.ui.settings.SettingsActivity
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.android.synthetic.main.short_cut_sheet_layout.*

class ShortCutSheetDialog : BottomSheetDialogFragment() {

    private lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(
            R.layout.short_cut_sheet_layout,
            container, false
        )
        val viewModel = ViewModelProvider(this).get(ShortCutViewModel::class.java)
        viewModel.getConfig().observe(this, {
            it?.let {
                setStudyMode(it)
                setStudyModeListener()
            }
        })
        viewModel.getDContext().observe(this, {
            it?.let {
                if (it.curCourseId == it.recycleBinId) {
                    disableDeleteAction()
                }
            }
        })
        initCopyQuestion()
        initDeleteCurQuestion()
        initSearchAction()
        return rootView
    }

    private fun setStudyMode(config: Config) {
        remember_mode_switch.isChecked = config.rememberMode
        focus_mode_switch.isChecked = config.focusMode
        favorite_mode_switch.isChecked = config.onlyFavorite
        sort_by_accuracy_mode_switch.isChecked = config.sortByAccuracy
    }

    private fun setStudyModeListener() {
        remember_mode_switch.setOnCheckedChangeListener { _, b ->
            DataRepository.updateRememberMode(b)
        }
        remember_content_container.setOnClickListener {
            remember_mode_switch.isChecked = !remember_mode_switch.isChecked
        }
        focus_mode_switch.setOnCheckedChangeListener { _, b ->
            DataRepository.updateFocusMode(b)
            dismiss()
        }
        focus_content_container.setOnClickListener {
            focus_mode_switch.isChecked = !focus_mode_switch.isChecked
        }
        favorite_mode_switch.setOnCheckedChangeListener { _, b ->
            DataRepository.updateOnlyFavoriteMode(b)
        }
        favorite_content_container.setOnClickListener {
            favorite_mode_switch.isChecked = !favorite_mode_switch.isChecked
        }
        sort_by_accuracy_mode_switch.setOnCheckedChangeListener { _, b ->
            DataRepository.updateSortAccuracyMode(b)
        }
        sort_by_accuracy_content_container.setOnClickListener {
            sort_by_accuracy_mode_switch.isChecked = !sort_by_accuracy_mode_switch.isChecked
        }
    }

    private fun disableDeleteAction() {
        rootView.findViewById<View>(R.id.delete_content_container).setOnClickListener(null)
        rootView.findViewById<TextView>(R.id.delete_title)
            .setTextColor(requireActivity().resources.getColor(R.color.short_cut_delete_disable_color))
        rootView.findViewById<ImageView>(R.id.delete_content_icon)
            .setImageDrawable(requireActivity().resources.getDrawable(R.drawable.ic_baseline_delete_outline_half_transparent_24))
    }

    private fun initCopyQuestion() {
        rootView.findViewById<View>(R.id.copy_content_container).setOnClickListener {
            ScreenShotHomeQuestionAction(requireActivity()).start()
            dismiss()
        }
    }

    private fun initDeleteCurQuestion() {
        rootView.findViewById<View>(R.id.delete_content_container).setOnClickListener {
            DeleteCurrentQuestionAction(requireActivity()).start()
            dismiss()
        }
    }

    private fun initSearchAction() {
        rootView.findViewById<View>(R.id.search_content_container).setOnClickListener {
            LiveEventBus.get(EventKey.SEARCH_CLICKED, Int::class.java)
                // 0没有意义
                .post(0)
            dismiss()
        }
        rootView.findViewById<View>(R.id.search_icon).setOnClickListener {
            LiveEventBus.get(EventKey.SEARCH_CLICKED, Int::class.java)
                // 0没有意义
                .post(0)
            dismiss()
        }
        rootView.findViewById<View>(R.id.toSetting).setOnClickListener {
            val intent = Intent(requireActivity(), SettingsActivity::class.java)
            startActivity(intent)
            dismiss()
        }
    }

    private fun initHistoryAction() {
        rootView.findViewById<View>(R.id.history_content_container).setOnClickListener {

        }
    }

}