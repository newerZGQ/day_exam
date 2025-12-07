package com.gorden.dayexam.repository.model

import com.gorden.dayexam.db.entity.PaperInfo
import java.util.*

data class PaperDetail (
    val paperInfo: PaperInfo,
    val studyInfo: PaperStudyInfo,
    val question: List<QuestionDetail>
)

data class PaperStudyInfo (
    val studyCount: Int,
    val lastStudyDate: Date? )