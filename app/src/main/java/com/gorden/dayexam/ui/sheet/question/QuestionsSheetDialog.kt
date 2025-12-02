package com.gorden.dayexam.ui.sheet.question

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gorden.dayexam.R

class QuestionsSheetDialog: BottomSheetDialogFragment() {

    lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(
            R.layout.questions_sheet_layout,
            container, false
        )
        return rootView
    }
}