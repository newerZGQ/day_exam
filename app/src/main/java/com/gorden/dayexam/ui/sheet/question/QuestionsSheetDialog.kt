package com.gorden.dayexam.ui.sheet.question

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gorden.dayexam.databinding.QuestionsSheetLayoutBinding

class QuestionsSheetDialog: BottomSheetDialogFragment() {

    private var _binding: QuestionsSheetLayoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View {
        _binding = QuestionsSheetLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}