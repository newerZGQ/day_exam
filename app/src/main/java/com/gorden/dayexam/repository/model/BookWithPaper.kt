package com.gorden.dayexam.repository.model

import com.gorden.dayexam.db.entity.Paper
import java.util.*

data class PaperWithQuestion (
    val paper: Paper,
    val curQuestionId: Int,
    val questionCount: Int,
    val studyInfo: PaperStudyInfo,
    val question: QuestionWithElement? )

data class PaperStudyInfo (
    val studyCount: Int,
    val lastStudyDate: Date? )